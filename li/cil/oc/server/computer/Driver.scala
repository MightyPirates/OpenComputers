package li.cil.oc.server.computer

import java.lang.reflect.InvocationTargetException

import scala.compat.Platform.EOL
import scala.reflect.runtime.{ universe => ru }
import scala.reflect.runtime.universe._

import com.naef.jnlua.DefaultConverter
import com.naef.jnlua.JavaFunction
import com.naef.jnlua.LuaRuntimeException
import com.naef.jnlua.LuaState

import li.cil.oc.OpenComputers
import li.cil.oc.api.Callback
import li.cil.oc.api.IComputerContext
import li.cil.oc.api.scala.IBlockDriver
import li.cil.oc.api.scala.IDriver
import li.cil.oc.api.scala.IItemDriver

class ItemDriver(val instance: IItemDriver) extends Driver
class BlockDriver(val instance: IBlockDriver) extends Driver

/**
 * Wrapper for external drivers.
 *
 * We create one instance per registered driver of this. It is used to inject
 * the API the driver offers into the computer, and, in particular, for
 * generating the wrappers for the API functions, which are closures with the
 * computer the API was installed into to provide context should a function
 * require it.
 */
abstract private[oc] class Driver {
  /** The actual driver as registered via the Drivers registry. */
  def instance: IDriver

  /** Installs this driver's API on the specified computer. */
  def installOn(computer: Computer) {
    // Check if the component actually provides an API.
    val apiName = instance.apiName
    if (!apiName.isDefined || apiName.get.isEmpty) return
    if (apiName.get == "component") {
      OpenComputers.log.warning("Trying to register API with reserved name 'component'.")
      return
    }
    val lua = computer.lua

    // Get or create table holding API tables.
    lua.getGlobal("driver") // ... drivers?
    assert(!lua.isNil(-1)) // ... drivers

    // Get or create API table.
    lua.getField(-1, apiName.get) // ... drivers api?
    if (lua.isNil(-1)) { // ... drivers nil
      lua.pop(1) // ... drivers
      lua.newTable() // ... drivers api
      lua.pushValue(-1) // ... drivers api api
      lua.setField(-3, apiName.get) // ... drivers api
    } // ... drivers api

    val mirror = ru.runtimeMirror(instance.getClass.getClassLoader)
    val instanceMirror = mirror.reflect(instance)
    val instanceType = instanceMirror.symbol.typeSignature
    val callbackSymbol = ru.typeOf[Callback].typeSymbol
    for (method <- instanceType.members collect { case m if m.isMethod => m.asMethod })
      method.annotations.filter(_.tpe.typeSymbol == callbackSymbol).
        foreach(annotation => {
          val name = annotation.javaArgs.get(newTermName("name")) match {
            case Some(arg) if arg != null => arg.asInstanceOf[String]
            case _ => method.name.decoded
          }
          lua.getField(-1, name) // ... drivers api func?
          if (lua.isNil(-1)) { // ... drivers api nil
            // No such entry yet.
            lua.pop(1) // ... drivers api
            lua.pushJavaFunction(new APIClosure(mirror, instanceMirror.reflectMethod(method), computer)) // ... drivers api func
            lua.setField(-2, name) // ... drivers api
          }
          else { // ... drivers api func
            // Entry already exists, skip it.
            lua.pop(1) // ... drivers api
            // Note that we can be sure it's an issue with two drivers
            // colliding, because this is guaranteed to run before any user
            // code had a chance to mess with the table.
            OpenComputers.log.warning(String.format(
              "Duplicate API entry, ignoring %s.%s of driver %s.",
              apiName, name, instance.componentName))
          }
        })
    // ... drivers api
    lua.pop(2) // ...

    // Run custom init script.
    instance.apiCode match {
      case None => // Nothing to do.
      case Some(apiCode) =>
        try {
          lua.load(apiCode, apiName.get, "t") // ... func
          apiCode.close()
          lua.call(0, 0) // ...
        }
        catch {
          case e: LuaRuntimeException =>
            OpenComputers.log.warning(String.format(
              "Initialization code of driver %s threw an error: %s",
              instance.componentName, e.getLuaStackTrace.mkString("", EOL, EOL)))
          case e: Throwable =>
            OpenComputers.log.warning(String.format(
              "Initialization code of driver %s threw an error: %s",
              instance.componentName, e.getStackTraceString))
        }
    }
  }

  /**
   * This class is used to represent closures for driver API callbacks.
   *
   * It stores the computer it is used by, to allow passing it along as the
   * {@see IComputerContext} for callbacks if they specify it as a parameter,
   * and for interacting with the Lua state to pull parameters and push results
   * returned from the callback.
   */
  private class APIClosure(mirror: Mirror, val method: MethodMirror, val computer: Computer) extends JavaFunction {
    /**
     * Based on the method's parameters we build a list of transformations
     * that convert Lua input to the expected parameter type.
     */
    val (parameterTransformations, parameterCountCheck) =
      buildParameterTransformations(mirror, method.symbol)

    /**
     * Based on the method's return value we build a callback that will push
     * that result to the stack, if any, and return the number of pushed values.
     */
    val returnTransformation = buildReturnTransformation(mirror, method.symbol)

    /** This is the function actually called from the Lua state. */
    def invoke(lua: LuaState) = {
      if (!parameterCountCheck(computer)) throw new Throwable {
        override def toString = "invalid argument count"
      }
      try {
        returnTransformation(computer,
          method(parameterTransformations.map(_(computer)): _*))
      }
      catch {
        // Transform error messages to only keep the actual message, to avoid
        // Java/scala specific stuff to be shown on the Lua side.
        case e: InvocationTargetException =>
          e.getCause.printStackTrace(); throw new Throwable {
            override def toString = e.getCause.getMessage
          }
        case e: Throwable =>
          e.printStackTrace(); throw new Throwable {
            override def toString = e.getMessage
          }
      }
    }

    /**
     * This generates the transformation functions that are used to convert the
     * Lua parameters to Java types to be passed on to the API function.
     */
    private def buildParameterTransformations(mirror: Mirror, method: MethodSymbol): (Array[Computer => Any], Computer => Boolean) =
      if (method.paramss.length == 0 || method.paramss(0).length == 0) {
        // No parameters.
        (Array(), c => c.lua.getTop() == 0)
      }
      else {
        val paramTypes = method.paramss(0).map(_.typeSignature.typeSymbol)

        if (paramTypes.length == 2 &&
          paramTypes(0) == typeOf[IComputerContext].typeSymbol &&
          paramTypes(1) == typeOf[Array[Any]].typeSymbol)
          // Callback function that wants to handle its arguments manually.
          // Convert all arguments and pack them into an array.
          (Array(
            c => c,
            c => (1 to c.lua.getTop()).map(
              c.lua.toJavaObject(_, classOf[Object]))), c => true)

        // Normal callback. Build converters based on the method's signature.
        else {
          // We keep track of the parameter's index manually because we have
          // to skip any occurrence of IComputerContext.
          var paramIndex = 0
          // We can set the check first because a var is really just a getter,
          // meaning any changes we make in the map below will apply.
          (paramTypes.map {
            case t if t == typeOf[IComputerContext].typeSymbol =>
              (c: Computer) => c
              case t => {
              val clazz = mirror.runtimeClass(t.asClass)
              paramIndex = paramIndex + 1
              val i = paramIndex
              (c: Computer) => c.lua.toJavaObject(i, clazz)
            }
          }.toArray, c => c.lua.getTop() == paramIndex)
        }
      }

    /**
     * This generates the transformation function that is used to convert the
     * return value of an API function to a Lua type and push it onto the stack,
     * returning the number of values pushed. We need that number in case we
     * return a tuple (i.e. the function returned an array).
     */
    private def buildReturnTransformation(mirror: Mirror, method: MethodSymbol): (Computer, Any) => Int =
      method.returnType match {
        case t if t.typeSymbol == typeOf[Unit].typeSymbol => (c, v) => 0
        case t if t.typeSymbol == typeOf[Array[_]].typeSymbol && checkType(mirror, t.asInstanceOf[TypeRefApi].args(0).typeSymbol) => (c, v) => {
          val array = v.asInstanceOf[Array[_]]
          array.foreach(c.lua.pushJavaObject(_))
          array.length
        }
        case t if checkType(mirror, t.typeSymbol) => (c, v) => c.lua.pushJavaObject(v); 1
        case _ =>
          OpenComputers.log.warning("Unsupported return type in function " + method.name.decoded + ".")
          (c, v) => 0
      }

    private def checkType(m: Mirror, t: Symbol) =
      if (t.isClass) DefaultConverter.isTypeSupported(mirror.runtimeClass(t.asClass))
      else if (t.isType) DefaultConverter.isTypeSupported(mirror.runtimeClass(t.typeSignature))
      else false
  }
}
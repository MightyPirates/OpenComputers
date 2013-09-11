package li.cil.oc.server.computer

import java.lang.reflect.Method
import scala.compat.Platform.EOL
import scala.reflect.runtime.universe._
import com.naef.jnlua.JavaFunction
import com.naef.jnlua.LuaRuntimeException
import com.naef.jnlua.LuaState
import li.cil.oc.OpenComputers
import li.cil.oc.api._

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
    val api = instance.apiName
    if (api == null || api.isEmpty()) return
    if (api.equals("component")) {
      OpenComputers.log.warning("Trying to register API with reserved name 'component'.")
      return
    }
    val lua = computer.lua

    // Get or create table holding API tables.
    lua.getGlobal("driver") // ... drivers?
    assert(!lua.isNil(-1)) // ... drivers

    // Get or create API table.
    lua.getField(-1, api) // ... drivers api?
    if (lua.isNil(-1)) { // ... drivers nil
      lua.pop(1) // ... drivers
      lua.newTable() // ... drivers api
      lua.pushValue(-1) // ... drivers api api
      lua.setField(-3, api) // ... drivers api
    } // ... drivers api

    val mirror = runtimeMirror(instance.getClass.getClassLoader)
    val instanceMirror = mirror.reflect(instance)
    val instanceType = instanceMirror.symbol.typeSignature
    for (method <- instanceType.members collect { case m if m.isMethod => m.asMethod })
      method.annotations collect {
        case annotation: Callback => {
          val name = annotation.name match {
            case s if s == null || s.isEmpty() => method.name.decoded
            case name => name
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
              api, name, instance.componentName))
          }
        }
      } // ... drivers api
    lua.pop(2) // ...

    // Run custom init script.
    val apiCode = instance.apiCode
    if (apiCode != null) {
      try {
        lua.load(apiCode, instance.apiName, "t") // ... func
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
    val parameterTransformations = buildParameterTransformations(mirror, method.symbol)

    /**
     * Based on the method's return value we build a callback that will push
     * that result to the stack, if any, and return the number of pushed values.
     */
    val returnTransformation = buildReturnTransformation(method.symbol)

    /** This is the function actually called from the Lua state. */
    def invoke(lua: LuaState): Int = {
      return returnTransformation(computer,
        method(parameterTransformations.map(_(computer)): _*))
    }
  }

  /**
   * This generates the transformation functions that are used to convert the
   * Lua parameters to Java types to be passed on to the API function.
   */
  private def buildParameterTransformations(mirror: Mirror, method: MethodSymbol): Array[Computer => Any] = {
    // No parameters?
    if (method.paramss.length == 0 || method.paramss(0).length == 0) {
      return Array()
    }

    val params = method.paramss(0)

    // Do we have a callback function that wants to handle its arguments
    // manually? If so, convert all arguments and pack them into an array.
    // TODO test if this really works.
    if (params.length == 2 &&
      params(0) == typeOf[IComputerContext].typeSymbol &&
      params(1) == typeOf[Array[Any]].typeSymbol) {
      Array(
        (c: Computer) => c,
        (c: Computer) => (1 to c.lua.getTop()).map(
          c.lua.toJavaObject(_, classOf[Object])))
    }
    // Otherwise build converters based on the method's signature.
    else params.zipWithIndex.map {
      case (t, i) if t == typeOf[IComputerContext].typeSymbol => (c: Computer) => c
      case (t, i) => {
        val clazz = mirror.runtimeClass(t.asClass)
        (c: Computer) => c.lua.toJavaObject(i, clazz)
      }
    }.toArray
  }

  /**
   * This generates the transformation function that is used to convert the
   * return value of an API function to a Lua type and push it onto the stack,
   * returning the number of values pushed. We need that number in case we
   * return a tuple (i.e. the function returned an array).
   */
  private def buildReturnTransformation(method: MethodSymbol): (Computer, Any) => Int =
    method.returnType match {
      case t if t == Unit => (c, v) => 0
      case t => (c, v) => c.lua.pushJavaObject(v); 1
    }
}
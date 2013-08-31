package li.cil.oc.server.computer

import java.lang.reflect.Method

import com.naef.jnlua.JavaFunction
import com.naef.jnlua.LuaState

import li.cil.oc.api.Callback
import li.cil.oc.api.IDriver
import li.cil.oc.common.computer.IInternalComputerContext

private[oc] class Driver(val driver: IDriver) {
  def injectInto(context: IInternalComputerContext) {
    // Check if the component actually provides an API.
    val api = driver.apiName
    if (api == null || api.isEmpty()) return
    val lua = context.luaState

    // Get or create table holding API tables.
    lua.getGlobal("drivers") // ... drivers?
    assert(!lua.isNil(-1)) // ... drivers

    // Get or create API table.
    lua.getField(-1, api) // ... drivers api?
    if (lua.isNil(-1)) { // ... drivers nil
      lua.pop(1) // ... drivers
      lua.newTable() // ... drivers api
      lua.pushValue(-1) // ... drivers api api
      lua.setField(-3, api) // ... drivers api
    } // ... drivers api

    for (method <- driver.getClass().getMethods())
      method.getAnnotation(classOf[Callback]) match {
        case null => Unit // No annotation.
        case annotation => {
          val name = annotation.name
          lua.getField(-1, name) // ... drivers api func?
          if (lua.isNil(-1)) { // ... drivers api nil
            // No such entry yet.
            lua.pop(1) // ... drivers api
            lua.pushJavaFunction(new MethodWrapper(context, method)) // ... drivers api func
            lua.setField(-2, name) // ... drivers api
          }
          else { // ... drivers api func
            // Entry already exists, skip it.
            lua.pop(1) // ... drivers api
            // TODO Log warning properly via a logger.
            println("WARNING: Duplicate API entry, ignoring: " + api + "." + name)
          }
        }
      } // ... drivers api
    lua.pop(2) // ...
  }

  private class MethodWrapper(val context: IInternalComputerContext, val method: Method) extends JavaFunction {
    def invoke(state: LuaState): Int = {
      return 0
    }
  }

  /*
  private class MethodWrapper(val context: IInternalComputerContext, val method: Method) extends JavaFunction {
    private val classOfBoolean = classOf[Boolean]
    private val classOfByte = classOf[Byte]
    private val classOfShort = classOf[Short]
    private val classOfInteger = classOf[Int]
    private val classOfLong = classOf[Long]
    private val classOfFloat = classOf[Float]
    private val classOfDouble = classOf[Double]
    private val classOfString = classOf[String]
    private val parameterTypes = method.getParameterTypes.zipWithIndex
    private val parameterCount = parameterTypes.size
    private val returnType = method.getReturnType
    private val returnsTuple = returnType.isInstanceOf[Array[Object]]
    private val returnsNothing = returnType.equals(Void.TYPE)

    // TODO Rework all of this, most likely won't work because of type erasure.
    def invoke(state: LuaState): Int = {
      // Parse the parameters, convert them to Java types.
      val parameters = Array(context) ++ parameterTypes.map {
        //case (classOfBoolean, i) => boolean2Boolean(state.checkBoolean(i + 1))
        case (classOfByte, i) => java.lang.Byte.valueOf(state.checkInteger(i + 1).toByte)
        case (classOfShort, i) => java.lang.Short.valueOf(state.checkInteger(i + 1).toShort)
        case (classOfInteger, i) => java.lang.Integer.valueOf(state.checkInteger(i + 1))
        case (classOfLong, i) => java.lang.Long.valueOf(state.checkInteger(i + 1).toLong)
        case (classOfFloat, i) => java.lang.Float.valueOf(state.checkNumber(i + 1).toFloat)
        case (classOfDouble, i) => java.lang.Double.valueOf(state.checkNumber(i + 1))
        case (classOfString, i) => state.checkString(i + 1)
        case _ => null
      }

      // Call the actual function, grab the result, if any.
      val result = call(parameters: _*)

      // Check the result, convert it to Lua.
      if (returnsTuple) {
        val array = result.asInstanceOf[Array[Object]]
        array.foreach(v => push(state, v, v.getClass()))
        return array.length
      }
      else if (returnsNothing) {
        return 0
      }
      else {
        push(state, result, returnType)
        return 1
      }
    }

    private def push(state: LuaState, value: Object, clazz: Class[_]) = clazz match {
      case classOfBoolean => state.pushBoolean(value.asInstanceOf[Boolean])
      case classOfInteger => state.pushNumber(value.asInstanceOf[Int])
      case classOfDouble => state.pushNumber(value.asInstanceOf[Double])
      case classOfString => state.pushString(value.asInstanceOf[String])
      case _ => state.pushNil()
    }

    protected def call(args: AnyRef*) = {
      method.invoke(driver, args)
    }
  }
*/
}
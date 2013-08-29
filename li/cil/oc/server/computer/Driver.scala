package li.cil.oc.server.computer

import java.lang.reflect.Method

import scala.Array.canBuildFrom

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

    // Get or create registry entry holding API tables.
    lua.getField(LuaState.REGISTRYINDEX, ComputerRegistry.driverApis)
    if (lua.isNil(-1)) {
      lua.pop(1)
      lua.newTable()
      lua.pushValue(-1)
      lua.setField(LuaState.REGISTRYINDEX, ComputerRegistry.driverApis)
    }

    // Get or create API table.
    lua.getField(-1, api)
    if (lua.isNil(-1)) {
      lua.pop(1)
      lua.newTable()
      lua.pushValue(-1)
      lua.setField(-3, api)
    }

    for (method <- driver.getClass().getMethods()) {
      val annotation = method.getAnnotation(classOf[Callback])
      if (annotation != null) {
        val name = annotation.name
        lua.getField(-1, name)
        if (lua.isNil(-1)) {
          // Entry already exists, skip it.
          lua.pop(1)
          // TODO Log warning properly via a logger.
          println("WARNING: Duplicate API entry, ignoring: " + api + "." + name)
        }
        else {
          // No such entry yet. Pop the nil and build our callback wrapper.
          lua.pop(1)
          if (annotation.synchronize) {
            lua.pushJavaFunction(new SynchronizedMethodWrapper(context, method))
          }
          else {
            lua.pushJavaFunction(new MethodWrapper(context, method))
          }
          lua.setField(-2, name)
        }
      }
    }

    // Pop the API table and the table holding all APIs.
    lua.pop(2)
  }

  /**
   * This installs the driver on the computer, providing an API to interact
   * with the device.
   *
   * This copies an existing API table from the registry and executes any
   * initialization code provided by the driver.
   */
  def install(context: IInternalComputerContext) {
    copyAPI(context)

    // Do we have custom initialization code?
    val code = driver.apiCode
    if (code != null && !code.isEmpty()) {
      val lua = context.luaState
      lua.load(code, driver.componentName)
      // TODO Set environment so that variables not explicitly added to globals
      //      table won't accidentally clutter it.
      lua.call(0, 0)
    }
  }

  private def copyAPI(context: IInternalComputerContext) {
    // Check if the component actually provides an API.
    val api = driver.apiName
    if (api == null && api.isEmpty()) return

    // Get the Lua state and check if the API table already exists.
    val lua = context.luaState

    lua.getField(LuaState.REGISTRYINDEX, ComputerRegistry.driverApis)
    if (lua.isNil(-1)) {
      // We don't have any APIs at all.
      lua.pop(1)
      return
    }

    lua.getField(-1, api)
    if (lua.isNil(-1)) {
      // No such API. Which is kind of weird, but hey.
      lua.pop(2)
      return
    }

    // OK, we have our registry table. Create a new table to copy into.
    val registryTable = lua.getTop()
    lua.newTable()
    val globalTable = lua.getTop()

    // Copy all keys (which are the API functions).
    lua.pushNil()
    while (lua.next(registryTable)) {
      val key = lua.toString(-2)
      lua.setField(globalTable, key)
    }

    // Push our globals table into the global name space.
    lua.setGlobal(api)

    // Pop the registry API table and registry table holding all API tables.
    lua.pop(2)
  }

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

  private class SynchronizedMethodWrapper(context: IInternalComputerContext, method: Method) extends MethodWrapper(context, method) {
    override def call(args: AnyRef*) = {
      context.lock()
      try super.call(args)
      finally context.unlock()
    }
  }
}
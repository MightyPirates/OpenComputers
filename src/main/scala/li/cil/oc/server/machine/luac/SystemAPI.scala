package li.cil.oc.server.machine.luac

import li.cil.oc.Settings
import li.cil.oc.util.ExtendedLuaState.extendLuaState
import li.cil.repack.com.naef.jnlua.LuaType

class SystemAPI(owner: NativeLuaArchitecture) extends NativeLuaAPI(owner) {
  override def initialize() {
    // Until we get to ingame screens we log to Java's stdout.
    lua.pushScalaFunction(lua => {
      println((1 to lua.getTop).map(i => lua.`type`(i) match {
        case LuaType.NIL => "nil"
        case LuaType.BOOLEAN => lua.toBoolean(i)
        case LuaType.NUMBER => if (lua.isInteger(i)) lua.toInteger(i) else lua.toNumber(i)
        case LuaType.STRING => lua.toString(i)
        case LuaType.TABLE => "table"
        case LuaType.FUNCTION => "function"
        case LuaType.THREAD => "thread"
        case LuaType.LIGHTUSERDATA | LuaType.USERDATA => "userdata"
      }).mkString("  "))
      0
    })
    lua.setGlobal("print")

    // Create system table, avoid magic global non-tables.
    lua.newTable()

    // Whether bytecode may be loaded directly.
    lua.pushScalaFunction(lua => {
      lua.pushBoolean(Settings.get.allowBytecode)
      1
    })
    lua.setField(-2, "allowBytecode")

    // Whether custom __gc callbacks are allowed.
    lua.pushScalaFunction(lua => {
      lua.pushBoolean(Settings.get.allowGC)
      1
    })
    lua.setField(-2, "allowGC")

    // How long programs may run without yielding before we stop them.
    lua.pushScalaFunction(lua => {
      lua.pushNumber(Settings.get.timeout)
      1
    })
    lua.setField(-2, "timeout")

    lua.setGlobal("system")
  }
}

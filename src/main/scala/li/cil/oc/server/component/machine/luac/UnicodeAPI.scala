package li.cil.oc.server.component.machine.luac

import li.cil.oc.server.component.machine.NativeLuaArchitecture
import li.cil.oc.util.ExtendedLuaState.extendLuaState

class UnicodeAPI(owner: NativeLuaArchitecture) extends NativeLuaAPI(owner) {
  override def initialize() {
    // Provide some better Unicode support.
    lua.newTable()

    lua.pushScalaFunction(lua => {
      lua.pushString(String.valueOf((1 to lua.getTop).map(lua.checkInteger).map(_.toChar).toArray))
      1
    })
    lua.setField(-2, "char")

    lua.pushScalaFunction(lua => {
      lua.pushInteger(lua.checkString(1).length)
      1
    })
    lua.setField(-2, "len")

    lua.pushScalaFunction(lua => {
      lua.pushString(lua.checkString(1).toLowerCase)
      1
    })
    lua.setField(-2, "lower")

    lua.pushScalaFunction(lua => {
      lua.pushString(lua.checkString(1).reverse)
      1
    })
    lua.setField(-2, "reverse")

    lua.pushScalaFunction(lua => {
      val string = lua.checkString(1)
      val start = math.max(0, lua.checkInteger(2) match {
        case i if i < 0 => string.length + i
        case i => i - 1
      })
      val end =
        if (lua.getTop > 2) math.min(string.length, lua.checkInteger(3) match {
          case i if i < 0 => string.length + i + 1
          case i => i
        })
        else string.length
      if (end <= start) lua.pushString("")
      else lua.pushString(string.substring(start, end))
      1
    })
    lua.setField(-2, "sub")

    lua.pushScalaFunction(lua => {
      lua.pushString(lua.checkString(1).toUpperCase)
      1
    })
    lua.setField(-2, "upper")

    lua.setGlobal("unicode")
  }
}

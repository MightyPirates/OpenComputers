package li.cil.oc.server.machine.luac

import li.cil.oc.util.ExtendedLuaState.extendLuaState
import li.cil.oc.util.FontUtils

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

    lua.pushScalaFunction(lua => {
      lua.pushBoolean(FontUtils.wcwidth(lua.checkString(1).codePointAt(0)) > 1)
      1
    })
    lua.setField(-2, "isWide")

    lua.pushScalaFunction(lua => {
      lua.pushInteger(FontUtils.wcwidth(lua.checkString(1).codePointAt(0)))
      1
    })
    lua.setField(-2, "charWidth")

    lua.pushScalaFunction(lua => {
      val value = lua.checkString(1)
      lua.pushInteger(value.toCharArray.map(ch => math.max(1, FontUtils.wcwidth(ch))).sum)
      1
    })
    lua.setField(-2, "wlen")

    lua.pushScalaFunction(lua => {
      val value = lua.checkString(1)
      val count = lua.checkInteger(2)
      var width = 0
      var end = 0
      while (width < count) {
        width += math.max(1, FontUtils.wcwidth(value(end)))
        end += 1
      }
      if (end > 1) lua.pushString(value.substring(0, end - 1))
      else lua.pushString("")
      1
    })
    lua.setField(-2, "wtrunc")

    lua.setGlobal("unicode")
  }
}

package li.cil.oc.server.machine.luac

import java.util.function.IntUnaryOperator
import li.cil.oc.util.ExtendedLuaState.extendLuaState
import li.cil.oc.util.{ExtendedUnicodeHelper, FontUtils}

class UnicodeAPI(owner: NativeLuaArchitecture) extends NativeLuaAPI(owner) {
  override def initialize() {
    // Provide some better Unicode support.
    lua.newTable()

    lua.pushScalaFunction(lua => {
      val builder = new java.lang.StringBuilder()
      (1 to lua.getTop).map(lua.checkInt32).foreach(builder.appendCodePoint)
      lua.pushString(builder.toString)
      1
    })
    lua.setField(-2, "char")

    lua.pushScalaFunction(lua => {
      val s = lua.checkString(1)
      lua.pushInteger(ExtendedUnicodeHelper.length(s))
      1
    })
    lua.setField(-2, "len")

    lua.pushScalaFunction(lua => {
      lua.pushString(lua.checkString(1).toLowerCase)
      1
    })
    lua.setField(-2, "lower")

    lua.pushScalaFunction(lua => {
      lua.pushString(ExtendedUnicodeHelper.reverse(lua.checkString(1)))
      1
    })
    lua.setField(-2, "reverse")

    lua.pushScalaFunction(lua => {
      val string = lua.checkString(1)
      val sLength = ExtendedUnicodeHelper.length(string)
      val start = lua.checkInt32(2) match {
        case i if i < 0 => string.offsetByCodePoints(string.length, math.max(i, -sLength))
        case i => string.offsetByCodePoints(0, math.min(i - 1, sLength))
      }
      val end =
        if (lua.getTop > 2) lua.checkInt32(3) match {
          case i if i < 0 => string.offsetByCodePoints(string.length, math.max(i + 1, -sLength))
          case i => string.offsetByCodePoints(0, math.min(i, sLength))
        }
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
      lua.pushInteger(value.codePoints().map(new IntUnaryOperator {
        override def applyAsInt(ch: Int): Int = math.max(1, FontUtils.wcwidth(ch))
      }).sum)
      1
    })
    lua.setField(-2, "wlen")

    lua.pushScalaFunction(lua => {
      val value = lua.checkString(1)
      val count = lua.checkInteger(2)
      var width = 0
      var end = 0
      while (width < count) {
        width += math.max(1, FontUtils.wcwidth(value.codePointAt(end)))
        end = value.offsetByCodePoints(end, 1)
      }
      if (end > 1) lua.pushString(value.substring(0, end - 1))
      else lua.pushString("")
      1
    })
    lua.setField(-2, "wtrunc")

    lua.setGlobal("unicode")
  }
}

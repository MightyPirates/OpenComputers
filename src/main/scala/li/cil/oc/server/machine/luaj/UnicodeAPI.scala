package li.cil.oc.server.machine.luaj

import java.util.function.IntUnaryOperator
import li.cil.oc.util.{ExtendedUnicodeHelper, FontUtils}
import li.cil.oc.util.ScalaClosure._
import li.cil.repack.org.luaj.vm2.LuaValue
import li.cil.repack.org.luaj.vm2.Varargs

class UnicodeAPI(owner: LuaJLuaArchitecture) extends LuaJAPI(owner) {
  override def initialize() {
    // Provide some better Unicode support.
    val unicode = LuaValue.tableOf()

    unicode.set("lower", (args: Varargs) => LuaValue.valueOf(args.checkjstring(1).toLowerCase))

    unicode.set("upper", (args: Varargs) => LuaValue.valueOf(args.checkjstring(1).toUpperCase))

    unicode.set("char", (args: Varargs) => {
      val builder = new java.lang.StringBuilder()
      (1 to args.narg).map(args.checkint).foreach(builder.appendCodePoint)
      LuaValue.valueOf(builder.toString)
    })

    unicode.set("len", (args: Varargs) => {
      val s = args.checkjstring(1)
      LuaValue.valueOf(s.codePointCount(0, s.length))
    })

    unicode.set("reverse", (args: Varargs) => LuaValue.valueOf(ExtendedUnicodeHelper.reverse(args.checkjstring(1))))

    unicode.set("sub", (args: Varargs) => {
      val string = args.checkjstring(1)
      val sLength = ExtendedUnicodeHelper.length(string)
      val start = args.checkint(2) match {
        case i if i < 0 => string.offsetByCodePoints(string.length, math.max(i, -sLength))
        case i => string.offsetByCodePoints(0, math.min(i - 1, sLength))
      }
      val end =
        if (args.narg > 2) args.checkint(3) match {
          case i if i < 0 => string.offsetByCodePoints(string.length, math.max(i + 1, -sLength))
          case i => string.offsetByCodePoints(0, math.min(i, sLength))
        }
        else string.length
      if (end <= start) LuaValue.valueOf("")
      else LuaValue.valueOf(string.substring(start, end))
    })

    unicode.set("isWide", (args: Varargs) =>
      LuaValue.valueOf(FontUtils.wcwidth(args.checkjstring(1).codePointAt(0)) > 1))

    unicode.set("charWidth", (args: Varargs) =>
      LuaValue.valueOf(FontUtils.wcwidth(args.checkjstring(1).codePointAt(0))))

    unicode.set("wlen", (args: Varargs) => {
      val value = args.checkjstring(1)
      LuaValue.valueOf(value.codePoints.map(new IntUnaryOperator {
        override def applyAsInt(ch: Int): Int = math.max(1, FontUtils.wcwidth(ch))
      }).sum)
    })

    unicode.set("wtrunc", (args: Varargs) => {
      val value = args.checkjstring(1)
      val count = args.checkint(2)
      var width = 0
      var end = 0
      while (width < count) {
        width += math.max(1, FontUtils.wcwidth(value.codePointAt(end)))
        end = value.offsetByCodePoints(end, 1)
      }
      if (end > 1) LuaValue.valueOf(value.substring(0, end - 1))
      else LuaValue.valueOf("")
    })

    lua.set("unicode", unicode)
  }
}

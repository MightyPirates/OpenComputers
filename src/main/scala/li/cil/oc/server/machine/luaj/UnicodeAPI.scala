package li.cil.oc.server.machine.luaj

import li.cil.oc.util.FontUtils
import li.cil.oc.util.ScalaClosure._
import li.cil.repack.org.luaj.vm2.LuaValue
import li.cil.repack.org.luaj.vm2.Varargs

class UnicodeAPI(owner: LuaJLuaArchitecture) extends LuaJAPI(owner) {
  override def initialize() {
    // Provide some better Unicode support.
    val unicode = LuaValue.tableOf()

    unicode.set("lower", (args: Varargs) => LuaValue.valueOf(args.checkjstring(1).toLowerCase))

    unicode.set("upper", (args: Varargs) => LuaValue.valueOf(args.checkjstring(1).toUpperCase))

    unicode.set("char", (args: Varargs) => LuaValue.valueOf(String.valueOf((1 to args.narg).map(args.checkint).map(_.toChar).toArray)))

    unicode.set("len", (args: Varargs) => LuaValue.valueOf(args.checkjstring(1).length))

    unicode.set("reverse", (args: Varargs) => LuaValue.valueOf(args.checkjstring(1).reverse))

    unicode.set("sub", (args: Varargs) => {
      val string = args.checkjstring(1)
      val start = math.max(0, args.checkint(2) match {
        case i if i < 0 => string.length + i
        case i => i - 1
      })
      val end =
        if (args.narg > 2) math.min(string.length, args.checkint(3) match {
          case i if i < 0 => string.length + i + 1
          case i => i
        })
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
      LuaValue.valueOf(value.toCharArray.map(ch => math.max(1, FontUtils.wcwidth(ch))).sum)
    })

    unicode.set("wtrunc", (args: Varargs) => {
      val value = args.checkjstring(1)
      val count = args.checkint(2)
      var width = 0
      var end = 0
      while (width < count) {
        width += math.max(1, FontUtils.wcwidth(value(end)))
        end += 1
      }
      if (end > 1) LuaValue.valueOf(value.substring(0, end - 1))
      else LuaValue.valueOf("")
    })

    lua.set("unicode", unicode)
  }
}

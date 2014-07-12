package li.cil.oc.server.component.machine.luaj

import li.cil.oc.server.component.machine.LuaJLuaArchitecture
import li.cil.oc.util.FontUtil
import li.cil.oc.util.ScalaClosure._
import org.luaj.vm3.{LuaValue, Varargs}

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
      LuaValue.valueOf(FontUtil.wcwidth(args.checkint(1)) > 1))

    unicode.set("charWidth", (args: Varargs) =>
      LuaValue.valueOf(FontUtil.wcwidth(args.checkint(1))))

    lua.set("unicode", unicode)
  }
}

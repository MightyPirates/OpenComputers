package li.cil.oc.server.machine.luaj

import li.cil.oc.util.GameTimeFormatter
import li.cil.oc.util.ScalaClosure._
import li.cil.repack.org.luaj.vm2.LuaValue
import li.cil.repack.org.luaj.vm2.Varargs

class OSAPI(owner: LuaJLuaArchitecture) extends LuaJAPI(owner) {
  override def initialize() {
    val os = LuaValue.tableOf()

    os.set("clock", (_: Varargs) => LuaValue.valueOf(machine.cpuTime()))

    // Date formatting function.
    os.set("date", (args: Varargs) => {
      val format =
        if (args.narg > 0 && args.isstring(1)) args.tojstring(1)
        else "%d/%m/%y %H:%M:%S"
      val time =
        if (args.narg > 1 && args.isnumber(2)) args.todouble(2)
        else (machine.worldTime + 6000) * 60 * 60 / 1000

      val dt = GameTimeFormatter.parse(time)
      def fmt(format: String) = {
        if (format == "*t") {
          val table = LuaValue.tableOf(0, 8)
          table.set("year", LuaValue.valueOf(dt.year))
          table.set("month", LuaValue.valueOf(dt.month))
          table.set("day", LuaValue.valueOf(dt.day))
          table.set("hour", LuaValue.valueOf(dt.hour))
          table.set("min", LuaValue.valueOf(dt.minute))
          table.set("sec", LuaValue.valueOf(dt.second))
          table.set("wday", LuaValue.valueOf(dt.weekDay))
          table.set("yday", LuaValue.valueOf(dt.yearDay))
          table
        }
        else {
          LuaValue.valueOf(GameTimeFormatter.format(format, dt))
        }
      }

      // Just ignore the allowed leading '!', Minecraft has no time zones...
      if (format.startsWith("!"))
        fmt(format.substring(1))
      else
        fmt(format)
    })

    // Return ingame time for os.time().
    os.set("time", (args: Varargs) => {
      if (args.isnoneornil(1)) {
        // Game time is in ticks, so that each day has 24000 ticks, meaning
        // one hour is game time divided by one thousand. Also, Minecraft
        // starts days at 6 o'clock, versus the 1 o'clock of timestamps so we
        // add those five hours. Thus:
        // timestamp = (time + 5000) * 60[kh] * 60[km] / 1000[s]
        LuaValue.valueOf((machine.worldTime + 5000) * 60 * 60 / 1000)
      }
      else {
        val table = args.checktable(1)

        def getField(key: String, d: Int) = {
          val res = table.get(key)
          if (!res.isint())
            if (d < 0) throw new Exception("field '" + key + "' missing in date table")
            else d
          else res.toint()
        }

        val sec = getField("sec", 0)
        val min = getField("min", 0)
        val hour = getField("hour", 12)
        val mday = getField("day", -1)
        val mon = getField("month", -1)
        val year = getField("year", -1)

        GameTimeFormatter.mktime(year, mon, mday, hour, min, sec) match {
          case Some(time) => LuaValue.valueOf(time)
          case _ => LuaValue.NIL
        }
      }
    })

    lua.set("os", os)
  }
}

package li.cil.oc.server.component.machine.luaj

import li.cil.oc.server.component.machine.LuaJLuaArchitecture
import li.cil.oc.util.GameTimeFormatter
import li.cil.oc.util.ScalaClosure._
import org.luaj.vm3.{LuaValue, Varargs}

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
        if (args.narg > 1 && args.isnumber(2)) args.todouble(2) * 1000 / 60 / 60
        else machine.worldTime + 6000

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
    os.set("time", (_: Varargs) => {
      // Game time is in ticks, so that each day has 24000 ticks, meaning
      // one hour is game time divided by one thousand. Also, Minecraft
      // starts days at 6 o'clock, so we add those six hours. Thus:
      // timestamp = (time + 6000) * 60[kh] * 60[km] / 1000[s]
      LuaValue.valueOf((machine.worldTime + 6000) * 60 * 60 / 1000)
    })

    lua.set("os", os)
  }
}

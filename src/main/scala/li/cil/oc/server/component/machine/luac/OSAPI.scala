package li.cil.oc.server.component.machine.luac

import li.cil.oc.server.component.machine.NativeLuaArchitecture
import li.cil.oc.util.ExtendedLuaState.extendLuaState
import li.cil.oc.util.GameTimeFormatter

class OSAPI(owner: NativeLuaArchitecture) extends NativeLuaAPI(owner) {
  override def initialize() {
    // Push a couple of functions that override original Lua API functions or
    // that add new functionality to it.
    lua.getGlobal("os")

    // Custom os.clock() implementation returning the time the computer has
    // been actively running, instead of the native library...
    lua.pushScalaFunction(lua => {
      lua.pushNumber(machine.cpuTime())
      1
    })
    lua.setField(-2, "clock")

    // Date formatting function.
    lua.pushScalaFunction(lua => {
      val format =
        if (lua.getTop > 0 && lua.isString(1)) lua.toString(1)
        else "%d/%m/%y %H:%M:%S"
      val time =
        if (lua.getTop > 1 && lua.isNumber(2)) lua.toNumber(2) * 1000 / 60 / 60
        else machine.worldTime + 6000

      val dt = GameTimeFormatter.parse(time)
      def fmt(format: String) {
        if (format == "*t") {
          lua.newTable(0, 8)
          lua.pushInteger(dt.year)
          lua.setField(-2, "year")
          lua.pushInteger(dt.month)
          lua.setField(-2, "month")
          lua.pushInteger(dt.day)
          lua.setField(-2, "day")
          lua.pushInteger(dt.hour)
          lua.setField(-2, "hour")
          lua.pushInteger(dt.minute)
          lua.setField(-2, "min")
          lua.pushInteger(dt.second)
          lua.setField(-2, "sec")
          lua.pushInteger(dt.weekDay)
          lua.setField(-2, "wday")
          lua.pushInteger(dt.yearDay)
          lua.setField(-2, "yday")
        }
        else {
          lua.pushString(GameTimeFormatter.format(format, dt))
        }
      }

      // Just ignore the allowed leading '!', Minecraft has no time zones...
      if (format.startsWith("!"))
        fmt(format.substring(1))
      else
        fmt(format)
      1
    })
    lua.setField(-2, "date")

    // Return ingame time for os.time().
    lua.pushScalaFunction(lua => {
      // Game time is in ticks, so that each day has 24000 ticks, meaning
      // one hour is game time divided by one thousand. Also, Minecraft
      // starts days at 6 o'clock, so we add those six hours. Thus:
      // timestamp = (time + 6000) * 60[kh] * 60[km] / 1000[s]
      lua.pushNumber((machine.worldTime + 6000) * 60 * 60 / 1000)
      1
    })
    lua.setField(-2, "time")

    // Pop the os table.
    lua.pop(1)
  }
}

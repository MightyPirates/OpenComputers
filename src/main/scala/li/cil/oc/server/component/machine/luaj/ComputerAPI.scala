package li.cil.oc.server.component.machine.luaj

import li.cil.oc.Settings
import li.cil.oc.api.network.Connector
import li.cil.oc.server.component.machine.LuaJLuaArchitecture
import li.cil.oc.util.ScalaClosure._
import org.luaj.vm3.{LuaValue, Varargs}

class ComputerAPI(owner: LuaJLuaArchitecture) extends LuaJAPI(owner) {
  override def initialize() {
    // Computer API, stuff that kinda belongs to os, but we don't want to
    // clutter it.
    val computer = LuaValue.tableOf()

    // Allow getting the real world time for timeouts.
    computer.set("realTime", (_: Varargs) => LuaValue.valueOf(System.currentTimeMillis() / 1000.0))

    // The time the computer has been running, as opposed to the CPU time.
    // World time is in ticks, and each second has 20 ticks. Since we
    // want uptime() to return real seconds, though, we'll divide it
    // accordingly.
    computer.set("uptime", (_: Varargs) => LuaValue.valueOf(machine.upTime()))

    // Allow the computer to figure out its own id in the component network.
    computer.set("address", (_: Varargs) => Option(node.address) match {
      case Some(address) => LuaValue.valueOf(address)
      case _ => LuaValue.NIL
    })

    // Get/set address of boot device.
    computer.set("getBootAddress", (_: Varargs) => owner.bootAddress match {
      case "" => LuaValue.NIL
      case address => LuaValue.valueOf(address)
    })

    computer.set("setBootAddress", (args: Varargs) => {
      if (args.isnoneornil(1)) owner.bootAddress = ""
      else owner.bootAddress = args.checkjstring(1)
      LuaValue.NIL
    })

    computer.set("freeMemory", (_: Varargs) => LuaValue.valueOf(owner.memory / 2))

    computer.set("totalMemory", (_: Varargs) => LuaValue.valueOf(owner.memory))

    computer.set("pushSignal", (args: Varargs) => LuaValue.valueOf(machine.signal(args.checkjstring(1), toSimpleJavaObjects(args, 2): _*)))

    // And it's /tmp address...
    computer.set("tmpAddress", (_: Varargs) => {
      val address = machine.tmpAddress
      if (address == null) LuaValue.NIL
      else LuaValue.valueOf(address)
    })

    // User management.
    computer.set("users", (_: Varargs) => LuaValue.varargsOf(machine.users.map(LuaValue.valueOf)))

    computer.set("addUser", (args: Varargs) => {
      machine.addUser(args.checkjstring(1))
      LuaValue.TRUE
    })

    computer.set("removeUser", (args: Varargs) => LuaValue.valueOf(machine.removeUser(args.checkjstring(1))))

    computer.set("energy", (_: Varargs) =>
      if (Settings.get.ignorePower)
        LuaValue.valueOf(Double.PositiveInfinity)
      else
        LuaValue.valueOf(node.asInstanceOf[Connector].globalBuffer))

    computer.set("maxEnergy", (_: Varargs) => LuaValue.valueOf(node.asInstanceOf[Connector].globalBufferSize))

    // Set the computer table.
    lua.set("computer", computer)
  }
}

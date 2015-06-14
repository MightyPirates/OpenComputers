package li.cil.oc.server.machine.luac

import li.cil.oc.Settings
import li.cil.oc.api.network.Connector
import li.cil.oc.util.ExtendedLuaState.extendLuaState

class ComputerAPI(owner: NativeLuaArchitecture) extends NativeLuaAPI(owner) {
  def initialize() {
    // Computer API, stuff that kinda belongs to os, but we don't want to
    // clutter it.
    lua.newTable()

    // Allow getting the real world time for timeouts.
    lua.pushScalaFunction(lua => {
      lua.pushNumber(System.currentTimeMillis() / 1000.0)
      1
    })
    lua.setField(-2, "realTime")

    // The time the computer has been running, as opposed to the CPU time.
    lua.pushScalaFunction(lua => {
      lua.pushNumber(machine.upTime())
      1
    })
    lua.setField(-2, "uptime")

    // Allow the computer to figure out its own id in the component network.
    lua.pushScalaFunction(lua => {
      Option(node.address) match {
        case None => lua.pushNil()
        case Some(address) => lua.pushString(address)
      }
      1
    })
    lua.setField(-2, "address")

    lua.pushScalaFunction(lua => {
      // This is *very* unlikely, but still: avoid this getting larger than
      // what we report as the total memory.
      lua.pushInteger(((lua.getFreeMemory min (lua.getTotalMemory - owner.kernelMemory)) / owner.ramScale).toInt)
      1
    })
    lua.setField(-2, "freeMemory")

    // Allow the system to read how much memory it uses and has available.
    lua.pushScalaFunction(lua => {
      lua.pushInteger(((lua.getTotalMemory - owner.kernelMemory) / owner.ramScale).toInt)
      1
    })
    lua.setField(-2, "totalMemory")

    lua.pushScalaFunction(lua => {
      lua.pushBoolean(machine.signal(lua.checkString(1), lua.toSimpleJavaObjects(2): _*))
      1
    })
    lua.setField(-2, "pushSignal")

    // And it's /tmp address...
    lua.pushScalaFunction(lua => {
      val address = machine.tmpAddress
      if (address == null) lua.pushNil()
      else lua.pushString(address)
      1
    })
    lua.setField(-2, "tmpAddress")

    // User management.
    lua.pushScalaFunction(lua => {
      val users = machine.users
      users.foreach(lua.pushString)
      users.length
    })
    lua.setField(-2, "users")

    lua.pushScalaFunction(lua => try {
      machine.addUser(lua.checkString(1))
      lua.pushBoolean(true)
      1
    } catch {
      case e: Throwable =>
        lua.pushNil()
        lua.pushString(Option(e.getMessage).getOrElse(e.toString))
        2
    })
    lua.setField(-2, "addUser")

    lua.pushScalaFunction(lua => {
      lua.pushBoolean(machine.removeUser(lua.checkString(1)))
      1
    })
    lua.setField(-2, "removeUser")

    lua.pushScalaFunction(lua => {
      if (Settings.get.ignorePower)
        lua.pushNumber(Double.PositiveInfinity)
      else
        lua.pushNumber(node.asInstanceOf[Connector].globalBuffer)
      1
    })
    lua.setField(-2, "energy")

    lua.pushScalaFunction(lua => {
      lua.pushNumber(node.asInstanceOf[Connector].globalBufferSize)
      1
    })
    lua.setField(-2, "maxEnergy")

    // Set the computer table.
    lua.setGlobal("computer")
  }
}

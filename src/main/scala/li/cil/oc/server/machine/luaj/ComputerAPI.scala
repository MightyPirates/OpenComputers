package li.cil.oc.server.machine.luaj

import li.cil.oc.Settings
import li.cil.oc.api
import li.cil.oc.api.driver.item.MutableProcessor
import li.cil.oc.api.driver.item.Processor
import li.cil.oc.api.network.Connector
import li.cil.oc.util.ScalaClosure._
import li.cil.repack.org.luaj.vm2.LuaValue
import li.cil.repack.org.luaj.vm2.Varargs

import scala.collection.convert.WrapAsScala._

class ComputerAPI(owner: LuaJLuaArchitecture) extends LuaJAPI(owner) {
  override def initialize() {
    // Computer API, stuff that kinda belongs to os, but we don't want to
    // clutter it.
    val computer = LuaValue.tableOf()

    // Allow getting the real world time for timeouts.
    computer.set("realTime", (_: Varargs) => LuaValue.valueOf(System.currentTimeMillis() / 1000.0))

    computer.set("uptime", (_: Varargs) => LuaValue.valueOf(machine.upTime()))

    // Allow the computer to figure out its own id in the component network.
    computer.set("address", (_: Varargs) => Option(node.address) match {
      case Some(address) => LuaValue.valueOf(address)
      case _ => LuaValue.NIL
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

    computer.set("getArchitectures", (args: Varargs) => {
      machine.host.internalComponents.map(stack => (stack, api.Driver.driverFor(stack))).collectFirst {
        case (stack, processor: MutableProcessor) => processor.allArchitectures.toSeq
        case (stack, processor: Processor) => Seq(processor.architecture(stack))
      } match {
        case Some(architectures) => LuaValue.listOf(architectures.map(api.Machine.getArchitectureName).map(LuaValue.valueOf).toArray)
        case _ => LuaValue.tableOf()
      }
    })

    computer.set("getArchitecture", (args: Varargs) => {
      machine.host.internalComponents.map(stack => (stack, api.Driver.driverFor(stack))).collectFirst {
        case (stack, processor: Processor) => LuaValue.valueOf(api.Machine.getArchitectureName(processor.architecture(stack)))
      }.getOrElse(LuaValue.NONE)
    })

    computer.set("setArchitecture", (args: Varargs) => {
      val archName = args.checkjstring(1)
      machine.host.internalComponents.map(stack => (stack, api.Driver.driverFor(stack))).collectFirst {
        case (stack, processor: MutableProcessor) => processor.allArchitectures.find(arch => api.Machine.getArchitectureName(arch) == archName) match {
          case Some(archClass) =>
            if (archClass != processor.architecture(stack)) {
              processor.setArchitecture(stack, archClass)
              LuaValue.TRUE
            }
            else {
              LuaValue.FALSE
            }
          case _ =>
            LuaValue.varargsOf(LuaValue.NIL, LuaValue.valueOf("unknown architecture"))
        }
      }.getOrElse(LuaValue.NONE)
    })

    // Set the computer table.
    lua.set("computer", computer)
  }
}

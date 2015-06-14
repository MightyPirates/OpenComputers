package li.cil.oc.integration.computercraft

import dan200.computercraft.api.lua.ILuaContext
import dan200.computercraft.api.lua.LuaException
import dan200.computercraft.api.peripheral.IComputerAccess
import dan200.computercraft.api.peripheral.IPeripheral
import li.cil.oc.Settings
import li.cil.oc.api
import li.cil.oc.api.machine.Context
import li.cil.oc.api.network.Component
import li.cil.oc.common.tileentity.AccessPoint
import li.cil.oc.common.tileentity.Switch
import li.cil.oc.util.ResultWrapper._
import net.minecraftforge.common.util.ForgeDirection

import scala.collection.convert.WrapAsJava._
import scala.collection.convert.WrapAsScala._
import scala.collection.mutable

class SwitchPeripheral(val switch: Switch) extends IPeripheral {
  private val methods = Map[String, (IComputerAccess, ILuaContext, Array[AnyRef]) => Array[AnyRef]](
    // Generic modem methods.
    "open" -> ((computer, context, arguments) => {
      val port = checkPort(arguments, 0)
      if (switch.openPorts(computer).size >= 128)
        throw new IllegalArgumentException("too many open channels")
      result(switch.openPorts(computer).add(port))
    }),
    "isOpen" -> ((computer, context, arguments) => {
      val port = checkPort(arguments, 0)
      result(switch.openPorts(computer).contains(port))
    }),
    "close" -> ((computer, context, arguments) => {
      val port = checkPort(arguments, 0)
      result(switch.openPorts(computer).remove(port))
    }),
    "closeAll" -> ((computer, context, arguments) => {
      switch.openPorts(computer).clear()
      null
    }),
    "transmit" -> ((computer, context, arguments) => {
      val sendPort = checkPort(arguments, 0)
      val answerPort = checkPort(arguments, 1)
      val data = arguments.drop(2) ++ Seq(Int.box(answerPort))
      val packet = api.Network.newPacket(s"cc${computer.getID}_${computer.getAttachmentName}", null, sendPort, data.toArray)
      result(switch.tryEnqueuePacket(None, packet))
    }),
    "isWireless" -> ((computer, context, arguments) => {
      // Let's pretend we're always wired, to allow accessing OC components
      // as remote peripherals when using an Access Point, too...
      result(false)
    }),

    // Undocumented modem messages.
    "callRemote" -> ((computer, context, arguments) => {
      val address = checkString(arguments, 0)
      visibleComponents.find(_.address == address) match {
        case Some(component) =>
          val method = checkString(arguments, 1)
          val fakeContext = new CCContext(computer, context)
          component.invoke(method, fakeContext, arguments.drop(2): _*)
        case _ => null
      }
    }),
    "getMethodsRemote" -> ((computer, context, arguments) => {
      val address = checkString(arguments, 0)
      visibleComponents.find(_.address == address) match {
        case Some(component) => result(mapAsJavaMap(component.methods.zipWithIndex.map(t => (t._2 + 1, t._1)).toMap))
        case _ => null
      }
    }),
    "getNamesRemote" -> ((computer, context, arguments) => {
      result(mapAsJavaMap(visibleComponents.map(_.address).zipWithIndex.map(t => (t._2 + 1, t._1)).toMap))
    }),
    "getTypeRemote" -> ((computer, context, arguments) => {
      val address = checkString(arguments, 0)
      visibleComponents.find(_.address == address) match {
        case Some(component) => result(component.name)
        case _ => null
      }
    }),
    "isPresentRemote" -> ((computer, context, arguments) => {
      val address = checkString(arguments, 0)
      result(visibleComponents.exists(_.address == address))
    }),

    // OC specific.
    "isAccessPoint" -> ((computer, context, arguments) => {
      result(switch.isInstanceOf[AccessPoint])
    }),
    "maxPacketSize" -> ((computer, context, arguments) => {
      result(Settings.get.maxNetworkPacketSize)
    })
  )

  private val methodNames = methods.keys.toArray.sorted

  override def getType = "modem"

  override def attach(computer: IComputerAccess) {
    switch.computers += computer
    switch.openPorts += computer -> mutable.Set.empty
  }

  override def detach(computer: IComputerAccess) {
    switch.computers -= computer
    switch.openPorts -= computer
  }

  override def getMethodNames = methodNames

  override def callMethod(computer: IComputerAccess, context: ILuaContext, method: Int, arguments: Array[AnyRef]) =
    try methods(methodNames(method))(computer, context, arguments) catch {
      case e: LuaException => throw e
      case t: Throwable => throw new LuaException(t.getMessage)
    }

  override def equals(other: IPeripheral) = other match {
    case peripheral: SwitchPeripheral => peripheral.switch == switch
    case _ => false
  }

  private def checkPort(args: Array[AnyRef], index: Int) = {
    if (args.length < index - 1 || !args(index).isInstanceOf[Double])
      throw new IllegalArgumentException(s"bad argument #${index + 1} (number expected)")
    val port = args(index).asInstanceOf[Double].toInt
    if (port < 0 || port > 0xFFFF)
      throw new IllegalArgumentException(s"bad argument #${index + 1} (number in [1, 65535] expected)")
    port
  }

  private def checkString(args: Array[AnyRef], index: Int) = {
    if (args.length < index - 1 || !args(index).isInstanceOf[String])
      throw new IllegalArgumentException(s"bad argument #${index + 1} (string expected)")
    args(index).asInstanceOf[String]
  }

  private def visibleComponents = {
    ForgeDirection.VALID_DIRECTIONS.flatMap(side => {
      val node = switch.sidedNode(side)
      node.reachableNodes.collect {
        case component: Component if component.canBeSeenFrom(node) => component
      }
    })
  }

  class CCContext(val computer: IComputerAccess, val context: ILuaContext) extends Context {
    override def node() = switch.node

    override def isPaused = false

    override def stop() = false

    override def canInteract(player: String) = true

    override def signal(name: String, args: AnyRef*) = {
      computer.queueEvent(name, args.toArray)
      true
    }

    override def pause(seconds: Double) = false

    override def isRunning = true

    override def start() = false
  }

}

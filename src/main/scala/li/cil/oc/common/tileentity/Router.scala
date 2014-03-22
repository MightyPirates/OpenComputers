package li.cil.oc.common.tileentity

import cpw.mods.fml.common.{Loader, Optional}
import dan200.computer.api.{ILuaContext, IComputerAccess, IPeripheral}
import li.cil.oc.api
import li.cil.oc.api.network.{Packet, Message}
import scala.collection.mutable

@Optional.Interface(iface = "dan200.computer.api.IPeripheral", modid = "ComputerCraft")
class Router extends traits.Hub with traits.NotAnalyzable with IPeripheral {
  // ----------------------------------------------------------------------- //
  // Peripheral

  private val computers = mutable.ArrayBuffer.empty[AnyRef]

  private val openPorts = mutable.Map.empty[AnyRef, mutable.Set[Int]]

  @Optional.Method(modid = "ComputerCraft")
  override def getType = "oc_adapter"

  @Optional.Method(modid = "ComputerCraft")
  override def attach(computer: IComputerAccess) {
    computers += computer
    openPorts += computer -> mutable.Set.empty
  }

  @Optional.Method(modid = "ComputerCraft")
  override def detach(computer: IComputerAccess) {
    computers -= computer
    openPorts -= computer
  }

  @Optional.Method(modid = "ComputerCraft")
  override def getMethodNames = Array("open", "isOpen", "close", "closeAll", "transmit", "isWireless")

  @Optional.Method(modid = "ComputerCraft")
  override def callMethod(computer: IComputerAccess, context: ILuaContext, method: Int, arguments: Array[AnyRef]) = getMethodNames()(method) match {
    case "open" =>
      val port = checkPort(arguments, 0)
      if (openPorts(computer).size >= 128)
        throw new IllegalArgumentException("too many open channels")
      Array(Boolean.box(openPorts(computer).add(port)))
    case "isOpen" =>
      val port = checkPort(arguments, 0)
      Array(Boolean.box(openPorts(computer).contains(port)))
    case "close" =>
      val port = checkPort(arguments, 0)
      Array(Boolean.box(openPorts(computer).remove(port)))
    case "closeAll" =>
      openPorts(computer).clear()
      null
    case "transmit" =>
      val sendPort = checkPort(arguments, 0)
      val answerPort = checkPort(arguments, 1)
      val data = Seq(Int.box(answerPort)) ++ arguments.drop(2)
      val packet = api.Network.newPacket(s"cc${computer.getID}_${computer.getAttachmentName}", null, sendPort, data.toArray)
      relayPacket(null, packet)
      null
    case "isWireless" => Array(java.lang.Boolean.FALSE)
    case _ => null
  }

  @Optional.Method(modid = "ComputerCraft")
  override def canAttachToSide(side: Int) = true

  protected def checkPort(args: Array[AnyRef], index: Int) = {
    if (args.length < index - 1 || !args(index).isInstanceOf[Double])
      throw new IllegalArgumentException("bad argument #%d (number expected)".format(index + 1))
    val port = args(index).asInstanceOf[Double].toInt
    if (port < 1 || port > 0xFFFF)
      throw new IllegalArgumentException("bad argument #%d (number in [1, 65535] expected)".format(index + 1))
    port
  }

  protected def queueMessage(source: String, destination: String, port: Int, answerPort: Int, args: Array[AnyRef]) {
    for (computer <- computers.map(_.asInstanceOf[IComputerAccess])) {
      val address = s"cc${computer.getID}_${computer.getAttachmentName}"
      if (source != address && Option(destination).forall(_ == address) && openPorts(computer).contains(port))
        computer.queueEvent("modem_message", Array(Seq(computer.getAttachmentName, Int.box(port), Int.box(answerPort)) ++ args.map {
          case x: Array[Byte] => new String(x, "UTF-8")
          case x => x
        }: _*))
    }
  }

  override protected def onPlugMessage(plug: Plug, message: Message) {
    super.onPlugMessage(plug, message)
    if (message.name == "network.message" && Loader.isModLoaded("ComputerCraft")) {
      message.data match {
        case Array(packet: Packet) =>
          packet.data.headOption match {
            case Some(answerPort: java.lang.Double) =>
              queueMessage(packet.source, packet.destination, packet.port, answerPort.toInt, packet.data.drop(1))
            case _ =>
              queueMessage(packet.source, packet.destination, packet.port, -1, packet.data)
          }
        case _ =>
      }
    }
  }
}

package li.cil.oc.common.tileentity

import cpw.mods.fml.common.{Loader, Optional}
import dan200.computer.api.{ILuaContext, IComputerAccess, IPeripheral}
import li.cil.oc.Blocks
import li.cil.oc.api.network.Message
import scala.collection.mutable

@Optional.Interface(iface = "dan200.computer.api.IPeripheral", modid = "ComputerCraft")
class Router extends Hub with IPeripheral with PassiveNode {

  override def canUpdate = false

  override def validate() {
    super.validate()
    worldObj.scheduleBlockUpdateFromLoad(xCoord, yCoord, zCoord, Blocks.router.parent.blockID, 0, 0)
  }

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
      plugs.foreach(_.node.sendToReachable("network.message", Seq(Int.box(sendPort), Int.box(answerPort)) ++ arguments.drop(2): _*))
      null
    case "isWireless" => Array(java.lang.Boolean.FALSE)
    case _ => null
  }

  @Optional.Method(modid = "ComputerCraft")
  override def canAttachToSide(side: Int) = true

  private def checkPort(args: Array[AnyRef], index: Int) = {
    if (args.length < index - 1 || !args(index).isInstanceOf[Double])
      throw new IllegalArgumentException("bad argument #%d (number expected)".format(index + 1))
    val port = args(index).asInstanceOf[Double].toInt
    if (port < 1 || port > 0xFFFF)
      throw new IllegalArgumentException("bad argument #%d (number in [1, 65535] expected)".format(index + 1))
    port
  }

  private def queueMessage(port: Int, answerPort: Int, args: Seq[AnyRef]) {
    for (computer <- computers.map(_.asInstanceOf[IComputerAccess])) {
      if (openPorts(computer).contains(port))
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
        case Array(port: Integer, answerPort: java.lang.Double, args@_*) =>
          queueMessage(port, answerPort.toInt, args)
        case Array(port: Integer, args@_*) =>
          queueMessage(port, -1, args)
        case _ =>
      }
    }
  }
}

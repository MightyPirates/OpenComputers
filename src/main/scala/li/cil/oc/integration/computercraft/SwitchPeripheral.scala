package li.cil.oc.integration.computercraft

import dan200.computercraft.api.lua.ILuaContext
import dan200.computercraft.api.peripheral.IComputerAccess
import dan200.computercraft.api.peripheral.IPeripheral
import li.cil.oc.Settings
import li.cil.oc.api
import li.cil.oc.common.tileentity.AccessPoint
import li.cil.oc.common.tileentity.Switch
import li.cil.oc.util.ResultWrapper._
import net.minecraftforge.common.util.ForgeDirection

import scala.collection.mutable

class SwitchPeripheral(val switch: Switch) extends IPeripheral {
  override def getType = "modem"

  override def attach(computer: IComputerAccess) {
    switch.computers += computer
    switch.openPorts += computer -> mutable.Set.empty
  }

  override def detach(computer: IComputerAccess) {
    switch.computers -= computer
    switch.openPorts -= computer
  }

  override def getMethodNames = Array("open", "isOpen", "close", "closeAll", "maxPacketSize", "transmit", "isWireless")

  override def callMethod(computer: IComputerAccess, context: ILuaContext, method: Int, arguments: Array[AnyRef]) = getMethodNames()(method) match {
    case "open" =>
      val port = checkPort(arguments, 0)
      if (switch.openPorts(computer).size >= 128)
        throw new IllegalArgumentException("too many open channels")
      result(switch.openPorts(computer).add(port))
    case "isOpen" =>
      val port = checkPort(arguments, 0)
      result(switch.openPorts(computer).contains(port))
    case "close" =>
      val port = checkPort(arguments, 0)
      result(switch.openPorts(computer).remove(port))
    case "closeAll" =>
      switch.openPorts(computer).clear()
      null
    case "maxPacketSize" =>
      result(Settings.get.maxNetworkPacketSize)
    case "transmit" =>
      val sendPort = checkPort(arguments, 0)
      val answerPort = checkPort(arguments, 1)
      val data = Seq(Int.box(answerPort)) ++ arguments.drop(2)
      val packet = api.Network.newPacket(s"cc${computer.getID}_${computer.getAttachmentName}", null, sendPort, data.toArray)
      result(switch.tryEnqueuePacket(None, packet))
    case "isWireless" => result(switch.isInstanceOf[AccessPoint])
    case _ => null
  }

  override def equals(other: IPeripheral) = other match {
    case peripheral: SwitchPeripheral => peripheral.switch == switch
    case _ => false
  }

  private def checkPort(args: Array[AnyRef], index: Int) = {
    if (args.length < index - 1 || !args(index).isInstanceOf[Double])
      throw new IllegalArgumentException("bad argument #%d (number expected)".format(index + 1))
    val port = args(index).asInstanceOf[Double].toInt
    if (port < 1 || port > 0xFFFF)
      throw new IllegalArgumentException("bad argument #%d (number in [1, 65535] expected)".format(index + 1))
    port
  }
}

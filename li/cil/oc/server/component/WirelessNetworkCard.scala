package li.cil.oc.server.component

import li.cil.oc.api.network._
import li.cil.oc.util.WirelessNetwork
import li.cil.oc.{Config, api}
import net.minecraft.nbt.NBTTagCompound
import net.minecraft.tileentity.TileEntity
import scala.collection.convert.WrapAsScala._
import scala.language.implicitConversions

class WirelessNetworkCard(val owner: TileEntity) extends NetworkCard {
  override val node = api.Network.newNode(this, Visibility.Network).
    withComponent("modem", Visibility.Neighbors).
    withConnector(Config.bufferWireless).
    create()

  var strength = 64.0

  // ----------------------------------------------------------------------- //

  @LuaCallback(value = "getStrength", direct = true)
  def getStrength(context: Context, args: Arguments): Array[AnyRef] = result(strength)

  @LuaCallback(value = "setStrength", direct = true)
  def setStrength(context: Context, args: Arguments): Array[AnyRef] = this.synchronized {
    strength = args.checkDouble(0) max 0
    result(strength)
  }

  override def isWireless(context: Context, args: Arguments): Array[AnyRef] = result(true)

  override def send(context: Context, args: Arguments) = this.synchronized {
    val address = args.checkString(0)
    val port = checkPort(args.checkInteger(1))
    if (strength > 0) {
      checkPower()
      for ((card, distance) <- WirelessNetwork.computeReachableFrom(this)
           if card.node.address == address && card.openPorts.contains(port)) {
        card.node.sendToReachable("computer.signal",
          Seq("modem_message", node.address, Int.box(port), Double.box(distance)) ++ args.drop(2): _*)
      }
    }
    super.send(context, args)
  }

  override def broadcast(context: Context, args: Arguments) = this.synchronized {
    val port = checkPort(args.checkInteger(0))
    if (strength > 0) {
      checkPower()
      for ((card, distance) <- WirelessNetwork.computeReachableFrom(this)
           if card.openPorts.contains(port)) {
        card.node.sendToReachable("computer.signal",
          Seq("modem_message", node.address, Int.box(port), Double.box(distance)) ++ args.drop(2): _*)
      }
    }
    super.broadcast(context, args)
  }

  private def checkPower() {
    val rate = Config.wirelessRangePerPower
    if (rate != 0 && !node.changeBuffer(-strength / rate))
      throw new Exception("not enough energy")
  }

  // ----------------------------------------------------------------------- //

  override def onConnect(node: Node) {
    super.onConnect(node)
    if (node == this.node) {
      WirelessNetwork.add(this)
    }
  }

  override def onDisconnect(node: Node) {
    super.onDisconnect(node)
    if (node == this.node) {
      WirelessNetwork.remove(this)
    }
  }

  // ----------------------------------------------------------------------- //

  override def load(nbt: NBTTagCompound) {
    super.load(nbt)
    if (nbt.hasKey(Config.namespace + "modem.strength")) {
      strength = nbt.getDouble(Config.namespace + "modem.strength")
    }
  }

  override def save(nbt: NBTTagCompound) {
    super.save(nbt)
    nbt.setDouble(Config.namespace + "modem.strength", strength)
  }
}

package li.cil.oc.server.component

import java.io.IOException
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
    withConnector().
    create()

  var strength = 0.0

  // ----------------------------------------------------------------------- //

  @LuaCallback(value = "getStrength", direct = true)
  def getStrength(context: Context, args: Arguments): Array[AnyRef] = result(strength)

  @LuaCallback("setStrength")
  def setStrength(context: Context, args: Arguments): Array[AnyRef] = {
    strength = args.checkDouble(0) max 0 min Config.maxWirelessRange
    result(strength)
  }

  override def isWireless(context: Context, args: Arguments): Array[AnyRef] = result(true)

  override def send(context: Context, args: Arguments) = {
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

  override def broadcast(context: Context, args: Arguments) = {
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
    val cost = Config.wirelessCostPerRange
    if (cost > 0) {
      if (node.globalBuffer < cost || !node.changeBuffer(-strength * cost)) {
        throw new IOException("not enough energy")
      }
    }
  }

  // ----------------------------------------------------------------------- //

  override def update() {
    super.update()
    WirelessNetwork.update(this)
  }

  override def onConnect(node: Node) {
    super.onConnect(node)
    if (node == this.node) {
      WirelessNetwork.add(this)
    }
  }

  override def onDisconnect(node: Node) {
    super.onDisconnect(node)
    if (node == this.node) {
      val removed = WirelessNetwork.remove(this)
      assert(removed)
    }
  }

  // ----------------------------------------------------------------------- //

  override def load(nbt: NBTTagCompound) {
    super.load(nbt)
    strength = nbt.getDouble("strength")
  }

  override def save(nbt: NBTTagCompound) {
    super.save(nbt)
    nbt.setDouble("strength", strength)
  }
}

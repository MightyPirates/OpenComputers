package li.cil.oc.server.component

import java.io._
import li.cil.oc.Settings
import li.cil.oc.api.Network
import li.cil.oc.api.network._
import li.cil.oc.util.WirelessNetwork
import net.minecraft.nbt.NBTTagCompound
import net.minecraft.tileentity.TileEntity
import scala.collection.convert.WrapAsScala._
import scala.language.implicitConversions

class WirelessNetworkCard(val owner: TileEntity) extends NetworkCard with WirelessNetwork.Endpoint {
  override val node = Network.newNode(this, Visibility.Network).
    withComponent("modem", Visibility.Neighbors).
    withConnector().
    create()

  var strength = Settings.get.maxWirelessRange

  // ----------------------------------------------------------------------- //

  @Callback(direct = true, doc = """function():number -- Get the signal strength (range) used when sending messages.""")
  def getStrength(context: Context, args: Arguments): Array[AnyRef] = result(strength)

  @Callback(doc = """function(strength:number):number -- Set the signal strength (range) used when sending messages.""")
  def setStrength(context: Context, args: Arguments): Array[AnyRef] = {
    strength = math.max(args.checkDouble(0), math.min(0, Settings.get.maxWirelessRange))
    result(strength)
  }

  override def isWireless(context: Context, args: Arguments): Array[AnyRef] = result(true)

  override def send(context: Context, args: Arguments) = {
    val address = args.checkString(0)
    val port = checkPort(args.checkInteger(1))
    checkPacketSize(args.drop(2))
    if (strength > 0) {
      checkPower()
      val packet = new NetworkCard.Packet(node.address, Some(address), port, args.drop(2))
      for ((endpoint, distance) <- WirelessNetwork.computeReachableFrom(this)) {
        endpoint.receivePacket(packet, distance)
      }
    }
    super.send(context, args)
  }

  override def broadcast(context: Context, args: Arguments) = {
    val port = checkPort(args.checkInteger(0))
    checkPacketSize(args.drop(1))
    if (strength > 0) {
      checkPower()
      val packet = new NetworkCard.Packet(node.address, None, port, args.drop(1))
      for ((endpoint, distance) <- WirelessNetwork.computeReachableFrom(this)) {
        endpoint.receivePacket(packet, distance)
      }
    }
    super.broadcast(context, args)
  }

  private def checkPower() {
    val cost = Settings.get.wirelessCostPerRange
    if (cost > 0 && !Settings.get.ignorePower) {
      if (!node.tryChangeBuffer(-strength * cost)) {
        throw new IOException("not enough energy")
      }
    }
  }

  // ----------------------------------------------------------------------- //

  override val canUpdate = true

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
    if (nbt.hasKey("strength")) {
      strength = nbt.getDouble("strength") max 0 min Settings.get.maxWirelessRange
    }
  }

  override def save(nbt: NBTTagCompound) {
    super.save(nbt)
    nbt.setDouble("strength", strength)
  }
}
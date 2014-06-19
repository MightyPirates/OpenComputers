package li.cil.oc.server.component

import li.cil.oc.Settings
import li.cil.oc.api.Network
import li.cil.oc.api.network._
import li.cil.oc.common.component
import li.cil.oc.server.network.QuantumNetwork
import net.minecraft.nbt.NBTTagCompound

import scala.collection.convert.WrapAsScala._

class LinkedCard extends component.ManagedComponent {
  val node = Network.newNode(this, Visibility.Network).
    withComponent("tunnel", Visibility.Neighbors).
    withConnector().
    create()

  var tunnel = "creative"

  // ----------------------------------------------------------------------- //

  @Callback(doc = """function(data...) -- Sends the specified data to the card this one is linked to.""")
  def send(context: Context, args: Arguments): Array[AnyRef] = {
    val endpoints = QuantumNetwork.getEndpoints(tunnel).filter(_ != this)
    val packet = Network.newPacket(node.address, null, 0, args.toArray)
    if (node.tryChangeBuffer(-(packet.size / 32.0 + Settings.get.wirelessCostPerRange * Settings.get.maxWirelessRange * 5))) {
      for (card <- endpoints) {
        card.receivePacket(packet)
      }
      result(true)
    }
    else result(Unit, "not enough energy")
  }

  @Callback(direct = true, doc = """function():number -- Gets the maximum packet size (config setting).""")
  def maxPacketSize(context: Context, args: Arguments): Array[AnyRef] = result(Settings.get.maxNetworkPacketSize)

  def receivePacket(packet: Packet) {
    val distance = 0
    node.sendToReachable("computer.signal", Seq("modem_message", packet.source, Int.box(packet.port), Double.box(distance)) ++ packet.data: _*)
  }

  // ----------------------------------------------------------------------- //

  override def onConnect(node: Node) {
    super.onConnect(node)
    if (node == this.node) {
      QuantumNetwork.add(this)
    }
  }

  override def onDisconnect(node: Node) {
    super.onDisconnect(node)
    if (node == this.node) {
      QuantumNetwork.remove(this)
    }
  }

  // ----------------------------------------------------------------------- //

  override def load(nbt: NBTTagCompound) {
    super.load(nbt)
    if (nbt.hasKey(Settings.namespace + "tunnel")) {
      tunnel = nbt.getString(Settings.namespace + "tunnel")
    }
  }

  override def save(nbt: NBTTagCompound) {
    super.save(nbt)
    nbt.setString(Settings.namespace + "tunnel", tunnel)
  }
}

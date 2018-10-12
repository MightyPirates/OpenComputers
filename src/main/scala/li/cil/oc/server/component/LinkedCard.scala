package li.cil.oc.server.component

import java.util

import li.cil.oc.Constants
import li.cil.oc.api.driver.DeviceInfo.DeviceAttribute
import li.cil.oc.api.driver.DeviceInfo.DeviceClass
import li.cil.oc.Settings
import li.cil.oc.api.machine.Arguments
import li.cil.oc.api.machine.Callback
import li.cil.oc.api.machine.Context
import li.cil.oc.api.network._
import li.cil.oc.api.Network
import li.cil.oc.api.driver.DeviceInfo
import li.cil.oc.api.prefab
import li.cil.oc.common.Tier
import li.cil.oc.server.network.QuantumNetwork
import net.minecraft.nbt.NBTTagCompound

import scala.collection.convert.WrapAsJava._
import scala.collection.convert.WrapAsScala._

class LinkedCard extends prefab.ManagedEnvironment with QuantumNetwork.QuantumNode with DeviceInfo with traits.WakeMessageAware {
  override val node = Network.newNode(this, Visibility.Network).
    withComponent("tunnel", Visibility.Neighbors).
    withConnector().
    create()

  var tunnel: String = "creative"

  // ----------------------------------------------------------------------- //

  private final lazy val deviceInfo = Map(
    DeviceAttribute.Class -> DeviceClass.Network,
    DeviceAttribute.Description -> "Quantumnet controller",
    DeviceAttribute.Vendor -> Constants.DeviceInfo.DefaultVendor,
    DeviceAttribute.Product -> "HyperLink IV: Ender Edition",
    DeviceAttribute.Capacity -> Settings.get.maxNetworkPacketSize.toString,
    DeviceAttribute.Width -> Settings.get.maxNetworkPacketParts.toString
  )

  override def getDeviceInfo: util.Map[String, String] = deviceInfo

  // ----------------------------------------------------------------------- //

  @Callback(doc = """function(data...) -- Sends the specified data to the card this one is linked to.""")
  def send(context: Context, args: Arguments): Array[AnyRef] = {
    val endpoints = QuantumNetwork.getEndpoints(tunnel).filter(_ != this)
    // Cast to iterable to use Scala's toArray instead of the Arguments' one (which converts byte arrays to Strings).
    val packet = Network.newPacket(node.address, null, 0, args.asInstanceOf[java.lang.Iterable[AnyRef]].toArray)
    if (node.tryChangeBuffer(-(packet.size / 32.0 + Settings.get.wirelessCostPerRange(Tier.Two) * Settings.get.maxWirelessRange(Tier.Two) * 5))) {
      for (endpoint <- endpoints) {
        endpoint.receivePacket(packet)
      }
      result(true)
    }
    else result(Unit, "not enough energy")
  }

  @Callback(direct = true, doc = "function():number -- Gets the maximum packet size (config setting).")
  def maxPacketSize(context: Context, args: Arguments): Array[AnyRef] = result(Settings.get.maxNetworkPacketSize)

  def receivePacket(packet: Packet): Unit = receivePacket(packet, 0, null)

  @Callback(direct = true, doc = "function():string -- Gets this link card's shared channel address")
  def getChannel(context: Context, args: Arguments): Array[AnyRef] = {
    result(this.tunnel)
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
    loadWakeMessage(nbt)
  }

  override def save(nbt: NBTTagCompound) {
    super.save(nbt)
    nbt.setString(Settings.namespace + "tunnel", tunnel)
    saveWakeMessage(nbt)
  }
}

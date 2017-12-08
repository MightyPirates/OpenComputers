package li.cil.oc.server.component

import java.io._
import java.util

import li.cil.oc.Constants
import li.cil.oc.api.driver.DeviceInfo.DeviceAttribute
import li.cil.oc.api.driver.DeviceInfo.DeviceClass
import li.cil.oc.Settings
import li.cil.oc.common.Tier
import li.cil.oc.api
import li.cil.oc.api.Network
import li.cil.oc.api.network.EnvironmentHost
import li.cil.oc.api.machine.Arguments
import li.cil.oc.api.machine.Callback
import li.cil.oc.api.machine.Context
import li.cil.oc.api.network._
import li.cil.oc.util.BlockPosition
import net.minecraft.nbt.NBTTagCompound

import scala.collection.convert.WrapAsJava._
import scala.language.implicitConversions

abstract class WirelessNetworkCard(host: EnvironmentHost) extends NetworkCard(host) with WirelessEndpoint {
  override val node = Network.newNode(this, Visibility.Network).
    withComponent("modem", Visibility.Neighbors).
    withConnector().
    create()

  protected def wirelessCostPerRange: Double
  
  protected def maxWirelessRange: Double
  
  protected def shouldSendWiredTraffic: Boolean
  
  var strength = maxWirelessRange
    
  override def x = BlockPosition(host).x

  override def y = BlockPosition(host).y

  override def z = BlockPosition(host).z

  override def world = host.world

  def receivePacket(packet: Packet, source: WirelessEndpoint) {
    val (dx, dy, dz) = ((source.x + 0.5) - host.xPosition, (source.y + 0.5) - host.yPosition, (source.z + 0.5) - host.zPosition)
    val distance = Math.sqrt(dx * dx + dy * dy + dz * dz)
    receivePacket(packet, distance)
  }

  // ----------------------------------------------------------------------- //
  
  @Callback(direct = true, doc = """function():number -- Get the signal strength (range) used when sending messages.""")
  def getStrength(context: Context, args: Arguments): Array[AnyRef] = result(strength)

  @Callback(doc = """function(strength:number):number -- Set the signal strength (range) used when sending messages.""")
  def setStrength(context: Context, args: Arguments): Array[AnyRef] = {
    strength = math.max(0, math.min(args.checkDouble(0), maxWirelessRange))
    result(strength)
  }

  override def isWireless(context: Context, args: Arguments): Array[AnyRef] = result(true)
  
  override def isWired(context: Context, args: Arguments): Array[AnyRef] = result(shouldSendWiredTraffic)
  
  override protected def doSend(packet: Packet) {
    if (strength > 0) {
      checkPower()
      api.Network.sendWirelessPacket(this, strength, packet)
    }
    if (shouldSendWiredTraffic)
      super.doSend(packet)
  }

  override protected def doBroadcast(packet: Packet) {
    if (strength > 0) {
      checkPower()
      api.Network.sendWirelessPacket(this, strength, packet)
    }
    if (shouldSendWiredTraffic)
      super.doBroadcast(packet)
  }
  
  private def checkPower() {
    val cost = wirelessCostPerRange
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
    if (world.getTotalWorldTime % 20 == 0) {
      api.Network.updateWirelessNetwork(this)
    }
  }

  override def onConnect(node: Node) {
    super.onConnect(node)
    if (node == this.node) {
      api.Network.joinWirelessNetwork(this)
    }
  }

  override def onDisconnect(node: Node) {
    super.onDisconnect(node)
    if (node == this.node || !world.blockExists(x, y, z)) {
      api.Network.leaveWirelessNetwork(this)
    }
  }

  // ----------------------------------------------------------------------- //

  override def load(nbt: NBTTagCompound) {
    super.load(nbt)
    if (nbt.hasKey("strength")) {
      strength = nbt.getDouble("strength") max 0 min maxWirelessRange
    }
  }

  override def save(nbt: NBTTagCompound) {
    super.save(nbt)
    nbt.setDouble("strength", strength)
  }
}

object WirelessNetworkCard {
  class Tier1(host: EnvironmentHost) extends WirelessNetworkCard(host) {
    override protected def wirelessCostPerRange = Settings.get.wirelessCostPerRange(Tier.One)
    
    override protected def maxWirelessRange = Settings.get.maxWirelessRange(Tier.One)
    
    // wired network card is before wireless cards in max port list
    override protected def maxOpenPorts = Settings.get.maxOpenPorts(Tier.One + 1)
    
    override protected def shouldSendWiredTraffic = false

    // ----------------------------------------------------------------------- //

    private final lazy val deviceInfo = Map(
      DeviceAttribute.Class -> DeviceClass.Network,
      DeviceAttribute.Description -> "Wireless ethernet controller",
      DeviceAttribute.Vendor -> Constants.DeviceInfo.DefaultVendor,
      DeviceAttribute.Product -> "39i110 (LPPW-01)",
      DeviceAttribute.Version -> "1.0",
      DeviceAttribute.Capacity -> Settings.get.maxNetworkPacketSize.toString,
      DeviceAttribute.Size -> maxOpenPorts.toString,
      DeviceAttribute.Width -> maxWirelessRange.toString
    )

    override def getDeviceInfo: util.Map[String, String] = deviceInfo
  }
  
  class Tier2(host: EnvironmentHost) extends Tier1(host) {
    override protected def wirelessCostPerRange = Settings.get.wirelessCostPerRange(Tier.Two)
    
    override protected def maxWirelessRange = Settings.get.maxWirelessRange(Tier.Two)
    
    // wired network card is before wireless cards in max port list
    override protected def maxOpenPorts = Settings.get.maxOpenPorts(Tier.Two + 1)
    
    override protected def shouldSendWiredTraffic = true

    // ----------------------------------------------------------------------- //

    private final lazy val deviceInfo = Map(
      DeviceAttribute.Class -> DeviceClass.Network,
      DeviceAttribute.Description -> "Wireless ethernet controller",
      DeviceAttribute.Vendor -> Constants.DeviceInfo.DefaultVendor,
      DeviceAttribute.Product -> "62i230 (MPW-01)",
      DeviceAttribute.Version -> "2.0",
      DeviceAttribute.Capacity -> Settings.get.maxNetworkPacketSize.toString,
      DeviceAttribute.Size -> maxOpenPorts.toString,
      DeviceAttribute.Width -> maxWirelessRange.toString
    )
    
    override def getDeviceInfo: util.Map[String, String] = deviceInfo
  }
}

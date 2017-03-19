package li.cil.oc.server.component

import java.io._
import java.util

import li.cil.oc.Constants
import li.cil.oc.api.driver.DeviceInfo.DeviceAttribute
import li.cil.oc.api.driver.DeviceInfo.DeviceClass
import li.cil.oc.Settings
import li.cil.oc.api
import li.cil.oc.api.Network
import li.cil.oc.api.machine.Arguments
import li.cil.oc.api.machine.Callback
import li.cil.oc.api.machine.Context
import li.cil.oc.api.network._
import li.cil.oc.api.util.Location
import li.cil.oc.util.BlockPosition
import li.cil.oc.util.ExtendedWorld._
import net.minecraft.nbt.NBTTagCompound

import scala.collection.convert.WrapAsJava._
import scala.language.implicitConversions

class WirelessNetworkCard(host: Location) extends NetworkCard(host) with WirelessEndpoint {
  override val getNode = Network.newNode(this, Visibility.NETWORK).
    withComponent("modem", Visibility.NEIGHBORS).
    withConnector().
    create()

  var strength = Settings.get.maxWirelessRange

  // ----------------------------------------------------------------------- //

  private final lazy val deviceInfo = Map(
    DeviceAttribute.Class -> DeviceClass.Network,
    DeviceAttribute.Description -> "Wireless ethernet controller",
    DeviceAttribute.Vendor -> Constants.DeviceInfo.DefaultVendor,
    DeviceAttribute.Product -> "62i230 (MPW-01)",
    DeviceAttribute.Capacity -> Settings.get.maxNetworkPacketSize.toString,
    DeviceAttribute.Width -> Settings.get.maxWirelessRange.toString
  )

  override def getDeviceInfo: util.Map[String, String] = deviceInfo

  // ----------------------------------------------------------------------- //

  def position = BlockPosition(host)

  override def getX = position.x

  override def getY = position.y

  override def getZ = position.z

  override def getWorld = host.getWorld

  def receivePacket(packet: Packet, source: WirelessEndpoint) {
    val (dx, dy, dz) = ((source.getX + 0.5) - host.xPosition, (source.getY + 0.5) - host.yPosition, (source.getZ + 0.5) - host.zPosition)
    val distance = Math.sqrt(dx * dx + dy * dy + dz * dz)
    receivePacket(packet, distance)
  }

  // ----------------------------------------------------------------------- //

  @Callback(direct = true, doc = """function():number -- Get the signal strength (range) used when sending messages.""")
  def getStrength(context: Context, args: Arguments): Array[AnyRef] = result(strength)

  @Callback(doc = """function(strength:number):number -- Set the signal strength (range) used when sending messages.""")
  def setStrength(context: Context, args: Arguments): Array[AnyRef] = {
    strength = math.max(0, math.min(args.checkDouble(0), Settings.get.maxWirelessRange))
    result(strength)
  }

  override def isWireless(context: Context, args: Arguments): Array[AnyRef] = result(true)

  override protected def doSend(packet: Packet) {
    if (strength > 0) {
      checkPower()
      api.Network.sendWirelessPacket(this, strength, packet)
    }
    super.doSend(packet)
  }

  override protected def doBroadcast(packet: Packet) {
    if (strength > 0) {
      checkPower()
      api.Network.sendWirelessPacket(this, strength, packet)
    }
    super.doBroadcast(packet)
  }

  private def checkPower() {
    val cost = Settings.Power.Cost.wirelessCostPerRange
    if (cost > 0 && !Settings.Power.ignorePower) {
      if (!getNode.tryChangeEnergy(-strength * cost)) {
        throw new IOException("not enough energy")
      }
    }
  }

  // ----------------------------------------------------------------------- //

  override val canUpdate = true

  override def update() {
    super.update()
    if (getWorld.getTotalWorldTime % 20 == 0) {
      api.Network.updateWirelessNetwork(this)
    }
  }

  override def onConnect(node: Node) {
    super.onConnect(node)
    if (node == this.getNode) {
      api.Network.joinWirelessNetwork(this)
    }
  }

  override def onDisconnect(node: Node) {
    super.onDisconnect(node)
    if (node == this.getNode || !getWorld.isBlockLoaded(position)) {
      api.Network.leaveWirelessNetwork(this)
    }
  }

  // ----------------------------------------------------------------------- //

  private final val StrengthTag = "strength"

  override def load(nbt: NBTTagCompound) {
    super.load(nbt)
    if (nbt.hasKey(StrengthTag)) {
      strength = nbt.getDouble(StrengthTag) max 0 min Settings.get.maxWirelessRange
    }
  }

  override def save(nbt: NBTTagCompound) {
    super.save(nbt)
    nbt.setDouble(StrengthTag, strength)
  }
}

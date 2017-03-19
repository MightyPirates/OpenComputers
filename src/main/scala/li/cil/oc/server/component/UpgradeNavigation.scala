package li.cil.oc.server.component

import java.util

import li.cil.oc.Constants
import li.cil.oc.api.driver.DeviceInfo.DeviceAttribute
import li.cil.oc.api.driver.DeviceInfo.DeviceClass
import li.cil.oc.Settings
import li.cil.oc.api
import li.cil.oc.api.Network
import li.cil.oc.api.driver.DeviceInfo
import li.cil.oc.api.internal
import li.cil.oc.api.machine.Arguments
import li.cil.oc.api.machine.Callback
import li.cil.oc.api.machine.Context
import li.cil.oc.api.network._
import li.cil.oc.api.prefab
import li.cil.oc.api.prefab.network
import li.cil.oc.api.prefab.network.AbstractManagedNodeContainer
import li.cil.oc.api.tileentity.Rotatable
import li.cil.oc.api.util.Location
import li.cil.oc.common.item.data.NavigationUpgradeData
import li.cil.oc.server.network.Waypoints
import li.cil.oc.util.BlockPosition
import net.minecraft.entity.player.EntityPlayer
import net.minecraft.item.ItemStack
import net.minecraft.nbt.NBTTagCompound
import net.minecraft.util.EnumFacing

import scala.collection.convert.WrapAsJava._

class UpgradeNavigation(val host: Location with Rotatable) extends AbstractManagedNodeContainer with DeviceInfo {
  override val getNode = Network.newNode(this, Visibility.NETWORK).
    withComponent("navigation", Visibility.NEIGHBORS).
    withConnector().
    create()

  val data = new NavigationUpgradeData()

  private final lazy val deviceInfo = Map(
    DeviceAttribute.Class -> DeviceClass.Generic,
    DeviceAttribute.Description -> "Navigation upgrade",
    DeviceAttribute.Vendor -> Constants.DeviceInfo.DefaultVendor,
    DeviceAttribute.Product -> "PathFinder v3",
    DeviceAttribute.Capacity -> data.getSize(host.getWorld).toString
  )

  override def getDeviceInfo: util.Map[String, String] = deviceInfo

  // ----------------------------------------------------------------------- //

  @Callback(doc = """function():number, number, number -- Get the current relative position of the robot.""")
  def getPosition(context: Context, args: Arguments): Array[AnyRef] = {
    val info = data.mapData(host.getWorld)
    val size = data.getSize(host.getWorld)
    val relativeX = host.xPosition - info.xCenter
    val relativeZ = host.zPosition - info.zCenter

    if (math.abs(relativeX) <= size / 2 && math.abs(relativeZ) <= size / 2)
      result(relativeX, host.yPosition, relativeZ)
    else
      result(Unit, "out of range")
  }

  @Callback(doc = """function():number -- Get the current orientation of the robot.""")
  def getFacing(context: Context, args: Arguments): Array[AnyRef] = result(host.getFacing.ordinal)

  @Callback(doc = """function():number -- Get the operational range of the navigation upgrade.""")
  def getRange(context: Context, args: Arguments): Array[AnyRef] = result(data.getSize(host.getWorld) / 2)

  @Callback(doc = """function(range:number):table -- Find waypoints in the specified range.""")
  def findWaypoints(context: Context, args: Arguments): Array[AnyRef] = {
    val range = args.checkDouble(0) max 0 min Settings.get.maxWirelessRange
    if (range <= 0) return result(Array.empty)
    if (!getNode.tryChangeEnergy(-range * Settings.Power.Cost.wirelessCostPerRange * 0.25)) return result(Unit, "not enough energy")
    context.pause(0.5)
    val position = BlockPosition(host)
    val positionVec = position.toVec3
    val rangeSq = range * range
    val waypoints = Waypoints.findWaypoints(position, range).
      filter(waypoint => waypoint.getDistanceSq(positionVec.xCoord, positionVec.yCoord, positionVec.zCoord) <= rangeSq)
    result(waypoints.map(waypoint => {
      val delta = waypoint.position.offset(waypoint.facing).toVec3.subtract(positionVec)
      Map(
        "position" -> Array(delta.xCoord, delta.yCoord, delta.zCoord),
        "redstone" -> waypoint.maxInput,
        "label" -> waypoint.label
      )
    }).toArray)
  }

  override def onMessage(message: Message): Unit = {
    super.onMessage(message)
    if (message.getName == "tablet.use") message.getSource.getContainer match {
      case machine: api.machine.Machine => (machine.host, message.getData) match {
        case (tablet: internal.Tablet, Array(nbt: NBTTagCompound, stack: ItemStack, player: EntityPlayer, blockPos: BlockPosition, side: EnumFacing, hitX: java.lang.Float, hitY: java.lang.Float, hitZ: java.lang.Float)) =>
          val info = data.mapData(host.getWorld)
          nbt.setInteger("posX", blockPos.x - info.xCenter)
          nbt.setInteger("posY", blockPos.y)
          nbt.setInteger("posZ", blockPos.z - info.zCenter)
        case _ => // Ignore.
      }
      case _ => // Ignore.
    }
  }

  // ----------------------------------------------------------------------- //

  override def load(nbt: NBTTagCompound) {
    super.load(nbt)
    data.load(nbt)
  }

  override def save(nbt: NBTTagCompound) {
    super.save(nbt)
    data.save(nbt)
  }
}

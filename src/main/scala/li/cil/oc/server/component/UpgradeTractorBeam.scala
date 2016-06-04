package li.cil.oc.server.component

import java.util

import li.cil.oc.Constants
import li.cil.oc.Constants.DeviceInfo.DeviceAttribute
import li.cil.oc.Constants.DeviceInfo.DeviceClass
import li.cil.oc.Settings
import li.cil.oc.api.Network
import li.cil.oc.api.driver.DeviceInfo
import li.cil.oc.api.network.EnvironmentHost
import li.cil.oc.api.internal
import li.cil.oc.api.machine.Arguments
import li.cil.oc.api.machine.Callback
import li.cil.oc.api.machine.Context
import li.cil.oc.api.network.Visibility
import li.cil.oc.api.prefab
import li.cil.oc.util.BlockPosition
import li.cil.oc.util.InventoryUtils
import net.minecraft.entity.item.EntityItem
import net.minecraft.entity.player.EntityPlayer
import net.minecraft.util.math.BlockPos

import scala.collection.convert.WrapAsJava._
import scala.collection.convert.WrapAsScala._

object UpgradeTractorBeam {

  abstract class Common extends prefab.ManagedEnvironment with DeviceInfo {
  override val node = Network.newNode(this, Visibility.Network).
    withComponent("tractor_beam").
    create()

  private val pickupRadius = 3

    private final lazy val deviceInfo = Map(
      DeviceAttribute.Class -> DeviceClass.Generic,
      DeviceAttribute.Description -> "Tractor beam",
      DeviceAttribute.Vendor -> Constants.DeviceInfo.DefaultVendor,
      DeviceAttribute.Product -> "T313-K1N.3515"
    )

    override def getDeviceInfo: util.Map[String, String] = deviceInfo

  protected def position: BlockPosition

  protected def collectItem(item: EntityItem): Unit

  private def world = position.world.get

  @Callback(doc = """function():boolean -- Tries to pick up a random item in the robots' vicinity.""")
  def suck(context: Context, args: Arguments): Array[AnyRef] = {
    val items = world.getEntitiesWithinAABB(classOf[EntityItem], position.bounds.expand(pickupRadius, pickupRadius, pickupRadius))
      .filter(item => item.isEntityAlive && !item.cannotPickup)
    if (items.nonEmpty) {
      val item = items(world.rand.nextInt(items.size))
      val stack = item.getEntityItem
      val size = stack.stackSize
      collectItem(item)
      if (stack.stackSize < size || item.isDead) {
        context.pause(Settings.get.suckDelay)
        world.playEvent(2003, new BlockPos(math.floor(item.posX).toInt, math.floor(item.posY).toInt, math.floor(item.posZ).toInt), 0)
        return result(true)
      }
    }
    result(false)
  }
  }

  class Player(val owner: EnvironmentHost, val player: () => EntityPlayer) extends Common {
    override protected def position = BlockPosition(owner)

    override protected def collectItem(item: EntityItem) = item.onCollideWithPlayer(player())
  }

  class Drone(val owner: internal.Agent) extends Common {
    override protected def position = BlockPosition(owner)

    override protected def collectItem(item: EntityItem) = {
      InventoryUtils.insertIntoInventory(item.getEntityItem, owner.mainInventory, None, 64, simulate = false, Some(insertionSlots))
    }

    private def insertionSlots = (owner.selectedSlot until owner.mainInventory.getSizeInventory) ++ (0 until owner.selectedSlot)
  }

}

package li.cil.oc.server.component

import li.cil.oc.Settings
import li.cil.oc.api.Network
import li.cil.oc.api.machine.Robot
import li.cil.oc.api.network.{Arguments, Callback, Context, Visibility}
import li.cil.oc.common.component
import net.minecraft.entity.item.EntityItem
import net.minecraft.util.AxisAlignedBB

import scala.collection.convert.WrapAsScala._

class UpgradeTractorBeam(owner: Robot) extends component.ManagedComponent {
  val node = Network.newNode(this, Visibility.Network).
    withComponent("tractor_beam").
    create()

  private val pickupRadius = 3

  private def world = owner.player.getEntityWorld

  @Callback(doc = """function():boolean -- Tries to pick up a random item in the robots' vicinity.""")
  def suck(context: Context, args: Arguments): Array[AnyRef] = {
    val items = world.getEntitiesWithinAABB(classOf[EntityItem], pickupBounds)
      .map(_.asInstanceOf[EntityItem])
      .filter(item => item.isEntityAlive && item.delayBeforeCanPickup <= 0)
    if (items.nonEmpty) {
      val item = items(world.rand.nextInt(items.size))
      val stack = item.getEntityItem
      val size = stack.stackSize
      item.onCollideWithPlayer(owner.player)
      if (stack.stackSize < size || item.isDead) {
        context.pause(Settings.get.suckDelay)
        return result(true)
      }
    }
    result(false)
  }

  private def pickupBounds = {
    val player = owner.player
    val x = player.posX
    val y = player.posY
    val z = player.posZ
    AxisAlignedBB.getBoundingBox(
      x - pickupRadius, y - pickupRadius, z - pickupRadius,
      x + pickupRadius, y + pickupRadius, z + pickupRadius)
  }
}

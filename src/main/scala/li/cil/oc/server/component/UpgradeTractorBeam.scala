package li.cil.oc.server.component

import li.cil.oc.Settings
import li.cil.oc.api.Network
import li.cil.oc.api.driver.EnvironmentHost
import li.cil.oc.api.machine.{Arguments, Callback, Context}
import li.cil.oc.api.network.Visibility
import li.cil.oc.common.component
import net.minecraft.entity.item.EntityItem
import net.minecraft.entity.player.EntityPlayer
import net.minecraft.util.AxisAlignedBB

import scala.collection.convert.WrapAsScala._

class UpgradeTractorBeam(owner: EnvironmentHost, player: () => EntityPlayer) extends component.ManagedComponent {
  val node = Network.newNode(this, Visibility.Network).
    withComponent("tractor_beam").
    create()

  private val pickupRadius = 3

  private def world = owner.world

  @Callback(doc = """function():boolean -- Tries to pick up a random item in the robots' vicinity.""")
  def suck(context: Context, args: Arguments): Array[AnyRef] = {
    val items = world.getEntitiesWithinAABB(classOf[EntityItem], pickupBounds)
      .map(_.asInstanceOf[EntityItem])
      .filter(item => item.isEntityAlive && item.delayBeforeCanPickup <= 0)
    if (items.nonEmpty) {
      val item = items(world.rand.nextInt(items.size))
      val stack = item.getEntityItem
      val size = stack.stackSize
      item.onCollideWithPlayer(player())
      if (stack.stackSize < size || item.isDead) {
        context.pause(Settings.get.suckDelay)
        world.playAuxSFX(2003, math.floor(item.posX).toInt, math.floor(item.posY).toInt, math.floor(item.posZ).toInt, 0)
        return result(true)
      }
    }
    result(false)
  }

  private def pickupBounds = {
    val player = this.player()
    val x = player.posX
    val y = player.posY
    val z = player.posZ
    AxisAlignedBB.getBoundingBox(
      x - pickupRadius, y - pickupRadius, z - pickupRadius,
      x + pickupRadius, y + pickupRadius, z + pickupRadius)
  }
}

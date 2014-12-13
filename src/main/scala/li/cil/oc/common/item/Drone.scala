package li.cil.oc.common.item

import li.cil.oc.common.entity
import net.minecraft.entity.player.EntityPlayer
import net.minecraft.item.ItemStack
import net.minecraft.world.World

class Drone(val parent: Delegator) extends Delegate {
  override def onItemUse(stack: ItemStack, player: EntityPlayer, world: World, x: Int, y: Int, z: Int, side: Int, hitX: Float, hitY: Float, hitZ: Float) = {
    if (!world.isRemote) {
      val drone = new entity.Drone(world)
      drone.info.load(stack)
      drone.setPosition(x + hitX, y + hitY, z + hitZ)
      world.spawnEntityInWorld(drone)
    }
    stack.stackSize -= 1
    true
  }
}

package li.cil.oc.common.item

import li.cil.oc.common.entity.Drone
import net.minecraft.entity.player.EntityPlayer
import net.minecraft.item.ItemStack
import net.minecraft.world.World

class Transistor(val parent: Delegator) extends Delegate {
  override def onItemRightClick(stack: ItemStack, world: World, player: EntityPlayer) = {

    if (!world.isRemote) {
      val drone = new Drone(world)
      drone.setPosition(player.posX, player.posY, player.posZ)
      world.spawnEntityInWorld(drone)
    }

    stack
  }
}

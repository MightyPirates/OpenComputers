package li.cil.oc.common.item

import li.cil.oc.api
import li.cil.oc.common.Loot
import li.cil.oc.common.entity
import li.cil.oc.common.init.Items
import net.minecraft.entity.player.EntityPlayer
import net.minecraft.item.ItemStack
import net.minecraft.world.World

class Drone(val parent: Delegator) extends Delegate {
  override def onItemRightClick(stack: ItemStack, world: World, player: EntityPlayer) = {
    if (!world.isRemote) {
      val drone = new entity.Drone(world)

      drone.info.components = Array(
        api.Items.get("cpu1").createItemStack(1),
        api.Items.get("ram2").createItemStack(1),
        Items.createLuaBios(),
        Loot.createLootDisk("drone", "drone")
      )
      // TODO drone.info.load(stack)

      drone.setPosition(player.posX, player.posY, player.posZ)
      world.spawnEntityInWorld(drone)
    }

    stack.stackSize -= 1
    stack
  }
}

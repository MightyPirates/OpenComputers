package li.cil.oc.common.container

import li.cil.oc.common.item.TabletWrapper
import li.cil.oc.integration.opencomputers.DriverScreen
import net.minecraft.entity.player.PlayerEntity
import net.minecraft.entity.player.PlayerInventory
import net.minecraft.inventory.IInventory
import net.minecraft.inventory.container.ContainerType
import net.minecraft.item.ItemStack

class Tablet(selfType: ContainerType[_ <: Tablet], id: Int, playerInventory: PlayerInventory, val stack: ItemStack, tablet: IInventory, slot1: String, tier1: Int)
  extends Player(selfType, id, playerInventory, tablet) {

  override protected def getHostClass = classOf[TabletWrapper]

  addSlot(new StaticComponentSlot(this, otherInventory, otherInventory.getContainerSize - 1, 80, 35, getHostClass, slot1, tier1) {
    override def mayPlace(stack: ItemStack): Boolean = {
      if (DriverScreen.worksWith(stack, getHostClass)) return false
      super.mayPlace(stack)
    }
  })

  addPlayerInventorySlots(8, 84)

  override def stillValid(player: PlayerEntity) = player == playerInventory.player
}

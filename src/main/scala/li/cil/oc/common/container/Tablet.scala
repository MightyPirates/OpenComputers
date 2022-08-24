package li.cil.oc.common.container

import li.cil.oc.common.item.TabletWrapper
import net.minecraft.entity.player.PlayerEntity
import net.minecraft.entity.player.PlayerInventory
import net.minecraft.inventory.IInventory
import net.minecraft.inventory.container.ContainerType
import net.minecraft.item.ItemStack

class Tablet(selfType: ContainerType[_ <: Tablet], id: Int, playerInventory: PlayerInventory, val stack: ItemStack, tablet: IInventory, slot1: String, tier1: Int)
  extends Player(selfType, id, playerInventory, tablet) {

  addSlot(new StaticComponentSlot(this, otherInventory, otherInventory.getContainerSize - 1, 80, 35, slot1, tier1))

  addPlayerInventorySlots(8, 84)

  override def stillValid(player: PlayerEntity) = player == playerInventory.player
}

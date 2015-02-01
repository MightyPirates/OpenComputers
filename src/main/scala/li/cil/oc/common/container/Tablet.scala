package li.cil.oc.common.container

import li.cil.oc.common.item.TabletWrapper
import net.minecraft.entity.player.EntityPlayer
import net.minecraft.entity.player.InventoryPlayer

class Tablet(playerInventory: InventoryPlayer, tablet: TabletWrapper) extends Player(playerInventory, tablet) {
  addSlotToContainer(new StaticComponentSlot(this, otherInventory, otherInventory.getSizeInventory - 1, 80, 35, tablet.containerSlotType, tablet.containerSlotTier))

  addPlayerInventorySlots(8, 84)

  override def canInteractWith(player: EntityPlayer) = player == playerInventory.player
}

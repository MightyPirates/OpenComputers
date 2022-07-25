package li.cil.oc.common.container

import li.cil.oc.common.item.TabletWrapper
import net.minecraft.entity.player.PlayerEntity
import net.minecraft.entity.player.PlayerInventory

class Tablet(id: Int, playerInventory: PlayerInventory, tablet: TabletWrapper) extends Player(null, id, playerInventory, tablet) {
  addSlot(new StaticComponentSlot(this, otherInventory, otherInventory.getContainerSize - 1, 80, 35, tablet.containerSlotType, tablet.containerSlotTier))

  addPlayerInventorySlots(8, 84)

  override def stillValid(player: PlayerEntity) = player == playerInventory.player
}

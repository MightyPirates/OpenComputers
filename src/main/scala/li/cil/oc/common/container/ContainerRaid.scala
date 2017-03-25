package li.cil.oc.common.container

import li.cil.oc.common.Slot
import li.cil.oc.common.Tier
import li.cil.oc.common.tileentity
import net.minecraft.entity.player.InventoryPlayer

class ContainerRaid(playerInventory: InventoryPlayer, raid: tileentity.Raid) extends AbstractContainerPlayer(playerInventory, raid) {
  addSlotToContainer(60, 23, Slot.HDD, Tier.Three)
  addSlotToContainer(80, 23, Slot.HDD, Tier.Three)
  addSlotToContainer(100, 23, Slot.HDD, Tier.Three)
  addPlayerInventorySlots(8, 84)
}

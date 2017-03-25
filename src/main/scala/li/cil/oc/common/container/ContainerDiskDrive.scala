package li.cil.oc.common.container

import li.cil.oc.common.Slot
import li.cil.oc.common.tileentity
import net.minecraft.entity.player.InventoryPlayer

class ContainerDiskDrive(playerInventory: InventoryPlayer, drive: tileentity.TileEntityDiskDrive) extends AbstractContainerPlayer(playerInventory, drive) {
  addSlotToContainer(80, 35, Slot.Floppy)
  addPlayerInventorySlots(8, 84)
}

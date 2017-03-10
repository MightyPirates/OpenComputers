package li.cil.oc.common.container

import li.cil.oc.common.Slot
import li.cil.oc.common.tileentity
import net.minecraft.entity.player.InventoryPlayer

class DiskDrive(playerInventory: InventoryPlayer, drive: tileentity.TileEntityDiskDrive) extends Player(playerInventory, drive) {
  addSlotToContainer(80, 35, Slot.Floppy)
  addPlayerInventorySlots(8, 84)
}

package li.cil.oc.common.container

import li.cil.oc.common.tileentity
import net.minecraft.entity.player.InventoryPlayer
import net.minecraft.inventory.Slot
import net.minecraft.item.ItemStack

class DiskDrive(playerInventory: InventoryPlayer, drive: tileentity.DiskDrive) extends Player(playerInventory, drive) {
  // Floppy slot.
  addSlotToContainer(new Slot(drive, 0, 80, 35) {
    override def isItemValid(item: ItemStack) = {
      drive.isItemValidForSlot(0, item)
    }
  })

  // Show the player's inventory.
  addPlayerInventorySlots(8, 84)
}

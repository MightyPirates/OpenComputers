package li.cil.oc.common.container

import li.cil.oc.api
import li.cil.oc.common.tileentity
import net.minecraft.entity.player.InventoryPlayer

class DiskDrive(playerInventory: InventoryPlayer, drive: tileentity.DiskDrive) extends Player(playerInventory, drive) {
  addSlotToContainer(80, 35, api.driver.Slot.Disk)
  addPlayerInventorySlots(8, 84)
}

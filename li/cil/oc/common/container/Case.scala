package li.cil.oc.common.container

import li.cil.oc.api
import li.cil.oc.common.tileentity
import net.minecraft.entity.player.{EntityPlayer, InventoryPlayer}

class Case(playerInventory: InventoryPlayer, `case`: tileentity.Case) extends Player(playerInventory, `case`) {
  for (i <- 0 to 2) {
    addSlotToContainer(98, 16 + i * slotSize, api.driver.Slot.Card)
  }

  for (i <- 0 to 1) {
    addSlotToContainer(120, 16 + i * slotSize, api.driver.Slot.Memory)
  }

  for (i <- 0 to 1) {
    addSlotToContainer(142, 16 + i * slotSize, api.driver.Slot.HardDiskDrive)
  }

  addSlotToContainer(142, 16 + 2 * slotSize, api.driver.Slot.Disk)

  // Show the player's inventory.
  addPlayerInventorySlots(8, 84)

  override def canInteractWith(player: EntityPlayer) =
    super.canInteractWith(player) && `case`.computer.canInteract(player.getCommandSenderName)
}
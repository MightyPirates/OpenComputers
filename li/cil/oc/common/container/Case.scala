package li.cil.oc.common.container

import li.cil.oc.api
import li.cil.oc.common.tileentity
import net.minecraft.entity.player.{EntityPlayer, InventoryPlayer}

class Case(playerInventory: InventoryPlayer, computer: tileentity.Case) extends Player(playerInventory, computer) {
  for (i <- 0 to (if (computer.tier == 2) 2 else 1)) {
    addSlotToContainer(98, 16 + i * slotSize, api.driver.Slot.Card)
  }

  for (i <- 0 to (if (computer.tier == 0) 0 else 1)) {
    addSlotToContainer(120, 16 + i * slotSize, api.driver.Slot.Memory)
  }

  for (i <- 0 to (if (computer.tier == 0) 0 else 1)) {
    addSlotToContainer(142, 16 + i * slotSize, api.driver.Slot.HardDiskDrive)
  }

  if (computer.tier == 2) {
    addSlotToContainer(142, 16 + 2 * slotSize, api.driver.Slot.Disk)
  }

  // Show the player's inventory.
  addPlayerInventorySlots(8, 84)

  override def canInteractWith(player: EntityPlayer) =
    super.canInteractWith(player) && computer.computer.canInteract(player.getCommandSenderName)
}
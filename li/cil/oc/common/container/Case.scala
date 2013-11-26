package li.cil.oc.common.container

import li.cil.oc.api
import li.cil.oc.common.tileentity
import net.minecraft.entity.player.{EntityPlayer, InventoryPlayer}

class Case(playerInventory: InventoryPlayer, `case`: tileentity.Case) extends Player(playerInventory, `case`) {
  addSlotToContainer(58, 17, api.driver.Slot.Power)

  for (i <- 0 to 2) {
    addSlotToContainer(80, 17 + i * slotSize, api.driver.Slot.Card)
  }

  for (i <- 0 to 1) {
    addSlotToContainer(102, 17 + i * slotSize, api.driver.Slot.Memory)
  }

  for (i <- 0 to 1) {
    addSlotToContainer(124, 17 + i * slotSize, api.driver.Slot.HardDiskDrive)
  }

  // Show the player's inventory.
  addPlayerInventorySlots(8, 84)

  override def canInteractWith(player: EntityPlayer) =
    super.canInteractWith(player) && `case`.computer.canInteract(player.getCommandSenderName)
}
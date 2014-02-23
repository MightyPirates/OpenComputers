package li.cil.oc.common.container

import li.cil.oc.api
import li.cil.oc.common.inventory.ServerInventory
import net.minecraft.entity.player.{EntityPlayer, InventoryPlayer}

class Server(playerInventory: InventoryPlayer, serverInventory: ServerInventory) extends Player(playerInventory, serverInventory) {
  for (i <- 0 to 1) {
    addSlotToContainer(76, 7 + i * slotSize, api.driver.Slot.Card, 2 - i)
  }

  for (i <- 0 to 1 + serverInventory.tier) {
    addSlotToContainer(100, 7 + i * slotSize, api.driver.Slot.Processor, 2)
  }

  for (i <- 0 to 1 + serverInventory.tier) {
    addSlotToContainer(124, 7 + i * slotSize, api.driver.Slot.Memory, 2)
  }

  for (i <- 0 to 1 + serverInventory.tier) {
    addSlotToContainer(148, 7 + i * slotSize, api.driver.Slot.HardDiskDrive, 2)
  }

  // Show the player's inventory.
  addPlayerInventorySlots(8, 84)

  override def canInteractWith(player: EntityPlayer) = player == playerInventory.player
}


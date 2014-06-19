package li.cil.oc.common.container

import li.cil.oc.common.InventorySlots
import li.cil.oc.common.inventory.ServerInventory
import net.minecraft.entity.player.{EntityPlayer, InventoryPlayer}

class Server(playerInventory: InventoryPlayer, serverInventory: ServerInventory) extends Player(playerInventory, serverInventory) {
  for (i <- 0 to 1) {
    val slot = InventorySlots.server(serverInventory.tier)(getInventory.size)
    addSlotToContainer(76, 7 + i * slotSize, slot.slot, slot.tier)
  }

  for (i <- 0 to 1 + serverInventory.tier) {
    val slot = InventorySlots.server(serverInventory.tier)(getInventory.size)
    addSlotToContainer(100, 7 + i * slotSize, slot.slot, slot.tier)
  }

  for (i <- 0 to 1 + serverInventory.tier) {
    val slot = InventorySlots.server(serverInventory.tier)(getInventory.size)
    addSlotToContainer(124, 7 + i * slotSize, slot.slot, slot.tier)
  }

  for (i <- 0 to 1 + serverInventory.tier) {
    val slot = InventorySlots.server(serverInventory.tier)(getInventory.size)
    addSlotToContainer(148, 7 + i * slotSize, slot.slot, slot.tier)
  }

  for (i <- 1 to serverInventory.tier) {
    val slot = InventorySlots.server(serverInventory.tier)(getInventory.size)
    addSlotToContainer(76, 7 + (i + 1) * slotSize, slot.slot, slot.tier)
  }

  // Show the player's inventory.
  addPlayerInventorySlots(8, 84)

  override def canInteractWith(player: EntityPlayer) = player == playerInventory.player
}


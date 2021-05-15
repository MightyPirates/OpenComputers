package li.cil.oc.common.container

import li.cil.oc.common.inventory.DatabaseInventory
import net.minecraft.entity.player.EntityPlayer
import net.minecraft.entity.player.InventoryPlayer
import net.minecraft.inventory._
import net.minecraft.item.ItemStack

class Database(playerInventory: InventoryPlayer, databaseInventory: DatabaseInventory) extends Player(playerInventory, databaseInventory) {
  val rows = math.sqrt(databaseInventory.getSizeInventory).ceil.toInt
  val offset = 8 + Array(3, 2, 0)(databaseInventory.tier) * slotSize

  for (row <- 0 until rows; col <- 0 until rows) {
    addSlotToContainer(offset + col * slotSize, offset + row * slotSize)
  }

  // Show the player's inventory.
  addPlayerInventorySlots(8, 174)

  override def canInteractWith(player: EntityPlayer) = player == playerInventory.player

  override def slotClick(slot: Int, dragType: Int, clickType: ClickType, player: EntityPlayer): ItemStack = {
    if (slot >= databaseInventory.getSizeInventory() || slot < 0) {
      // if the slot interaction is with the user inventory use
      // default behavior
      return super.slotClick(slot, dragType, clickType, player)
    }
    // remove the ghost item
    val ghostSlot = this.inventorySlots.get(slot);
    if (ghostSlot != null) {
      val inventoryPlayer = player.inventory
      val hand = inventoryPlayer.getItemStack()
      var itemToAdd = ItemStack.EMPTY
      // if the player is holding an item, place a copy
      if (!hand.isEmpty()) {
        itemToAdd = hand.copy()
      }
      ghostSlot.putStack(itemToAdd)
    }
    ItemStack.EMPTY
  }

  override protected def tryTransferStackInSlot(from: Slot, intoPlayerInventory: Boolean) {
    if (intoPlayerInventory) {
      from.onSlotChanged()
      return
    }
  
    val fromStack = from.getStack().copy()
    if (fromStack.isEmpty) {
      return
    }

    fromStack.setCount(1)
    val (begin, end) = (0, inventorySlots.size - 1)

    for (i <- begin to end) {
      val intoSlot = inventorySlots.get(i)
      if (intoSlot.inventory != from.inventory) {
        if (!intoSlot.getHasStack && intoSlot.isItemValid(fromStack)) {
          if (intoSlot.getSlotStackLimit > 0) {
            intoSlot.putStack(fromStack)
            return
          }
        }
      }
    }
  }
}


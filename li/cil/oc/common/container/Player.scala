package li.cil.oc.common.container

import li.cil.oc.api
import li.cil.oc.client.gui.Icons
import net.minecraft.entity.player.EntityPlayer
import net.minecraft.entity.player.InventoryPlayer
import net.minecraft.inventory.Container
import net.minecraft.inventory.IInventory
import net.minecraft.inventory.Slot
import net.minecraft.item.ItemStack

/** Utility for inventory containers providing basic re-usable functionality. */
abstract class Player(protected val playerInventory: InventoryPlayer, val otherInventory: IInventory) extends Container {
  /** Number of player inventory slots to display horizontally. */
  protected val playerInventorySizeX = InventoryPlayer.getHotbarSize

  /** Subtract four for armor slots. */
  protected val playerInventorySizeY = (playerInventory.getSizeInventory - 4) / playerInventorySizeX

  /** Render size of slots (width and height). */
  protected val slotSize = 18

  def canInteractWith(player: EntityPlayer) = otherInventory.isUseableByPlayer(player)

  override def transferStackInSlot(player: EntityPlayer, index: Int): ItemStack = {
    val slot = Option(inventorySlots.get(index)).map(_.asInstanceOf[Slot]).orNull
    if (slot != null && slot.getHasStack) {
      // Get search range and direction for checking for merge options.
      val playerInventorySize = 4 * 9
      val (begin, length, direction) =
        if (index < otherInventory.getSizeInventory) {
          // Merge the item into the player inventory.
          (otherInventory.getSizeInventory, playerInventorySize, true)
        }
        else {
          // Merge the item into the container inventory.
          (0, otherInventory.getSizeInventory, false)
        }

      val stack = slot.getStack
      val originalStack = stack.copy()
      if (tryTransferStackInSlot(stack, begin, length, direction)) {
        if (stack.stackSize == 0) {
          // We could move everything, clear the slot.
          slot.putStack(null)
        }
        else {
          // Partial move, signal change.
          slot.onSlotChanged()
        }

        if (stack.stackSize != originalStack.stackSize) {
          slot.onPickupFromSlot(player, stack)
          return originalStack
        }
        // else: Nothing changed.
      }
      // else: Merge failed.
    }
    // else: Empty slot.
    null
  }

  private def tryTransferStackInSlot(stack: ItemStack, offset: Int, length: Int, intoPlayerInventory: Boolean) = {
    var somethingChanged = false
    val step = if (intoPlayerInventory) -1 else 1
    val (begin, end) =
      if (intoPlayerInventory) (offset + length - 1, offset - 1)
      else (offset, offset + length)
    for (i <- begin until end by step if stack.isStackable && stack.stackSize > 0) {
      val slot = inventorySlots.get(i).asInstanceOf[Slot]
      if (slot.getHasStack) {
        val slotStack = slot.getStack
        val itemsAreEqual = stack.isItemEqual(slotStack) && ItemStack.areItemStackTagsEqual(stack, slotStack)
        val slotHasCapacity = slotStack.stackSize < slotStack.getMaxStackSize
        if (itemsAreEqual && slotHasCapacity) {
          val slotWouldOverflow = stack.stackSize + slotStack.stackSize > slotStack.getMaxStackSize
          if (slotWouldOverflow) {
            val itemsMoved = slotStack.getMaxStackSize - slotStack.stackSize
            stack.stackSize -= itemsMoved
            slotStack.stackSize = slotStack.getMaxStackSize
            slot.onSlotChanged()
            somethingChanged = true
          }
          else {
            slotStack.stackSize += stack.stackSize
            stack.stackSize = 0
            slot.onSlotChanged()
            somethingChanged = true
          }
        }
      }
    }
    for (i <- begin until end by step if stack.stackSize > 0) {
      val slot = inventorySlots.get(i).asInstanceOf[Slot]
      // The isItemValid is the only reason for the reimplementation of
      // mergeItemStack... no idea why it doesn't do that itself.
      if (!slot.getHasStack && slot.isItemValid(stack)) {
        slot.putStack(stack.copy())
        stack.stackSize = 0
        slot.onSlotChanged()
        somethingChanged = true
      }
    }
    somethingChanged
  }

  def addSlotToContainer(x: Int, y: Int, slot: api.driver.Slot = api.driver.Slot.None) {
    val index = getInventory.size
    addSlotToContainer(new Slot(otherInventory, index, x, y) {
      setBackgroundIcon(Icons.get(slot))

      override def isItemValid(item: ItemStack) = {
        otherInventory.isItemValidForSlot(index, item)
      }
    })
  }

  /** Render player inventory at the specified coordinates. */
  protected def addPlayerInventorySlots(left: Int, top: Int) = {
    // Show the inventory proper. Start at plus one to skip hot bar.
    for (slotY <- 1 until playerInventorySizeY) {
      for (slotX <- 0 until playerInventorySizeX) {
        val index = slotX + slotY * playerInventorySizeX
        val x = left + slotX * slotSize
        // Compensate for hot bar offset.
        val y = top + (slotY - 1) * slotSize
        addSlotToContainer(new Slot(playerInventory, index, x, y))
      }
    }

    // Show the quick slot bar below the internal inventory.
    val quickBarSpacing = 4
    for (index <- 0 until InventoryPlayer.getHotbarSize) {
      val x = left + index * slotSize
      val y = top + slotSize * (playerInventorySizeY - 1) + quickBarSpacing
      addSlotToContainer(new Slot(playerInventory, index, x, y))
    }
  }
}
package li.cil.oc.common.container

import li.cil.oc.api
import li.cil.oc.client.gui.Icons
import net.minecraft.entity.player.EntityPlayer
import net.minecraft.entity.player.InventoryPlayer
import net.minecraft.inventory.Container
import net.minecraft.inventory.IInventory
import net.minecraft.inventory.Slot
import net.minecraft.item.ItemStack
import cpw.mods.fml.common.FMLCommonHandler

abstract class Player(protected val playerInventory: InventoryPlayer, val otherInventory: IInventory) extends Container {
  /** Number of player inventory slots to display horizontally. */
  protected val playerInventorySizeX = InventoryPlayer.getHotbarSize

  /** Subtract four for armor slots. */
  protected val playerInventorySizeY = (playerInventory.getSizeInventory - 4) / playerInventorySizeX

  /** Render size of slots (width and height). */
  protected val slotSize = 18

  def canInteractWith(player: EntityPlayer) = otherInventory.isUseableByPlayer(player)

  override def slotClick(slot: Int, mouseClick: Int, holdingShift: Int, player: EntityPlayer) = {
    val result = super.slotClick(slot, mouseClick, holdingShift, player)
    if (FMLCommonHandler.instance.getEffectiveSide.isServer) {
      detectAndSendChanges() // We have to enforce this more than MC does itself
      // because stacks can change their... "character" just by being inserted in
      // certain containers - by being assigned an address.
    }
    result
  }

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

      tryTransferStackInSlot(slot, begin, length, direction)
      if (FMLCommonHandler.instance.getEffectiveSide.isServer) {
        detectAndSendChanges()
      }
    }
    null
  }

  private def tryTransferStackInSlot(from: Slot, offset: Int, length: Int, intoPlayerInventory: Boolean) {
    val fromStack = from.getStack
    var somethingChanged = false

    val step = if (intoPlayerInventory) -1 else 1
    val (begin, end) =
      if (intoPlayerInventory) (offset + length - 1, offset - 1)
      else (offset, offset + length)

    if (fromStack.isStackable) for (i <- begin until end by step if from.getHasStack && from.getStack.stackSize > 0) {
      val intoSlot = inventorySlots.get(i).asInstanceOf[Slot]
      if (intoSlot.getHasStack) {
        val intoStack = intoSlot.getStack
        val itemsAreEqual = fromStack.isItemEqual(intoStack) && ItemStack.areItemStackTagsEqual(fromStack, intoStack)
        val maxStackSize = fromStack.getMaxStackSize min intoSlot.getSlotStackLimit
        val slotHasCapacity = intoStack.stackSize < maxStackSize
        if (itemsAreEqual && slotHasCapacity) {
          val itemsMoved = (maxStackSize - intoStack.stackSize) min fromStack.stackSize
          if (itemsMoved > 0) {
            intoStack.stackSize += from.decrStackSize(itemsMoved).stackSize
            intoSlot.onSlotChanged()
            somethingChanged = true
          }
        }
      }
    }

    for (i <- begin until end by step if from.getHasStack && from.getStack.stackSize > 0) {
      val intoSlot = inventorySlots.get(i).asInstanceOf[Slot]
      if (!intoSlot.getHasStack && intoSlot.isItemValid(fromStack)) {
        val maxStackSize = fromStack.getMaxStackSize min intoSlot.getSlotStackLimit
        val itemsMoved = maxStackSize min fromStack.stackSize
        intoSlot.putStack(from.decrStackSize(itemsMoved))
        somethingChanged = true
      }
    }

    if (somethingChanged) {
      from.onSlotChanged()
    }
  }

  def addSlotToContainer(x: Int, y: Int, slot: api.driver.Slot = api.driver.Slot.None) {
    val index = getInventory.size
    addSlotToContainer(new Slot(otherInventory, index, x, y) {
      setBackgroundIcon(Icons.get(slot))

      override def getSlotStackLimit =
        slot match {
          case api.driver.Slot.Tool | api.driver.Slot.None => super.getSlotStackLimit
          case _ => 1
        }

      override def isItemValid(stack: ItemStack) = {
        otherInventory.isItemValidForSlot(index, stack)
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
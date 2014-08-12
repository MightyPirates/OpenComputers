package li.cil.oc.common.container

import li.cil.oc.common
import li.cil.oc.common.InventorySlots.InventorySlot
import li.cil.oc.util.SideTracker
import net.minecraft.entity.player.{EntityPlayer, InventoryPlayer}
import net.minecraft.inventory.{Container, ICrafting, IInventory, Slot}
import net.minecraft.item.ItemStack

import scala.collection.convert.WrapAsScala._

abstract class Player(val playerInventory: InventoryPlayer, val otherInventory: IInventory) extends Container {
  /** Number of player inventory slots to display horizontally. */
  protected val playerInventorySizeX = InventoryPlayer.getHotbarSize

  /** Subtract four for armor slots. */
  protected val playerInventorySizeY = (playerInventory.getSizeInventory - 4) / playerInventorySizeX

  /** Render size of slots (width and height). */
  protected val slotSize = 18

  override def canInteractWith(player: EntityPlayer) = otherInventory.isUseableByPlayer(player)

  override def slotClick(slot: Int, mouseClick: Int, holdingShift: Int, player: EntityPlayer) = {
    val result = super.slotClick(slot, mouseClick, holdingShift, player)
    if (SideTracker.isServer) {
      detectAndSendChanges() // We have to enforce this more than MC does itself
      // because stacks can change their... "character" just by being inserted in
      // certain containers - by being assigned an address.
    }
    if (result != null && result.stackSize > 0) result
    else null
  }

  override def transferStackInSlot(player: EntityPlayer, index: Int): ItemStack = {
    val slot = Option(inventorySlots.get(index)).map(_.asInstanceOf[Slot]).orNull
    if (slot != null && slot.getHasStack) {
      tryTransferStackInSlot(slot, slot.inventory == otherInventory)
      if (SideTracker.isServer) {
        detectAndSendChanges()
      }
    }
    null
  }

  protected def tryTransferStackInSlot(from: Slot, intoPlayerInventory: Boolean) {
    val fromStack = from.getStack
    var somethingChanged = false

    val step = if (intoPlayerInventory) -1 else 1
    val (begin, end) =
      if (intoPlayerInventory) (inventorySlots.size - 1, 0)
      else (0, inventorySlots.size - 1)

    if (fromStack.getMaxStackSize > 1) for (i <- begin to end by step if i >= 0 && i < inventorySlots.size && from.getHasStack && from.getStack.stackSize > 0) {
      val intoSlot = inventorySlots.get(i).asInstanceOf[Slot]
      if (intoSlot.inventory != from.inventory && intoSlot.getHasStack) {
        val intoStack = intoSlot.getStack
        val itemsAreEqual = fromStack.isItemEqual(intoStack) && ItemStack.areItemStackTagsEqual(fromStack, intoStack)
        val maxStackSize = math.min(fromStack.getMaxStackSize, intoSlot.getSlotStackLimit)
        val slotHasCapacity = intoStack.stackSize < maxStackSize
        if (itemsAreEqual && slotHasCapacity) {
          val itemsMoved = math.min(maxStackSize - intoStack.stackSize, fromStack.stackSize)
          if (itemsMoved > 0) {
            intoStack.stackSize += from.decrStackSize(itemsMoved).stackSize
            intoSlot.onSlotChanged()
            somethingChanged = true
          }
        }
      }
    }

    for (i <- begin to end by step if i >= 0 && i < inventorySlots.size && from.getHasStack && from.getStack.stackSize > 0) {
      val intoSlot = inventorySlots.get(i).asInstanceOf[Slot]
      if (intoSlot.inventory != from.inventory && !intoSlot.getHasStack && intoSlot.isItemValid(fromStack)) {
        val maxStackSize = math.min(fromStack.getMaxStackSize, intoSlot.getSlotStackLimit)
        val itemsMoved = math.min(maxStackSize, fromStack.stackSize)
        intoSlot.putStack(from.decrStackSize(itemsMoved))
        somethingChanged = true
      }
    }

    if (somethingChanged) {
      from.onSlotChanged()
    }
  }

  def addSlotToContainer(x: Int, y: Int, slot: String = common.Slot.None, tier: Int = common.Tier.Any) {
    val index = inventorySlots.size
    addSlotToContainer(new StaticComponentSlot(this, otherInventory, index, x, y, slot, tier))
  }

  def addSlotToContainer(x: Int, y: Int, info: Array[Array[InventorySlot]], tierGetter: () => Int) {
    val index = inventorySlots.size
    addSlotToContainer(new DynamicComponentSlot(this, otherInventory, index, x, y, info, tierGetter))
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

  protected def sendProgressBarUpdate(id: Int, value: Int) {
    for (entry <- crafters) entry match {
      case player: ICrafting => player.sendProgressBarUpdate(this, id, value)
      case _ =>
    }
  }
}
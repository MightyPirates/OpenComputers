package li.cil.oc.common.inventory

import net.minecraft.entity.player.EntityPlayer
import net.minecraft.inventory.IInventory
import net.minecraft.item.ItemStack
import net.minecraft.util.text.ITextComponent

trait InventoryProxy extends IInventory {
  def inventory: IInventory

  def offset = 0

  override def isEmpty: Boolean = inventory.isEmpty

  override def getSizeInventory: Int = inventory.getSizeInventory

  override def getInventoryStackLimit: Int = inventory.getInventoryStackLimit

  override def getName: String = inventory.getName

  override def getDisplayName: ITextComponent = inventory.getDisplayName

  override def hasCustomName: Boolean = inventory.hasCustomName

  override def isUsableByPlayer(player: EntityPlayer): Boolean = inventory.isUsableByPlayer(player)

  override def isItemValidForSlot(slot: Int, stack: ItemStack): Boolean = {
    val offsetSlot = slot + offset
    isValidSlot(offsetSlot) && inventory.isItemValidForSlot(offsetSlot, stack)
  }

  override def getStackInSlot(slot: Int): ItemStack = {
    val offsetSlot = slot + offset
    if (isValidSlot(offsetSlot)) inventory.getStackInSlot(offsetSlot)
    else ItemStack.EMPTY
  }

  override def decrStackSize(slot: Int, amount: Int): ItemStack = {
    val offsetSlot = slot + offset
    if (isValidSlot(offsetSlot)) inventory.decrStackSize(offsetSlot, amount)
    else ItemStack.EMPTY
  }

  override def removeStackFromSlot(slot: Int): ItemStack = {
    val offsetSlot = slot + offset
    if (isValidSlot(offsetSlot)) inventory.removeStackFromSlot(offsetSlot)
    else ItemStack.EMPTY
  }

  override def setInventorySlotContents(slot: Int, stack: ItemStack): Unit = {
    val offsetSlot = slot + offset
    if (isValidSlot(offsetSlot)) inventory.setInventorySlotContents(offsetSlot, stack)
  }

  override def markDirty(): Unit = inventory.markDirty()

  override def openInventory(player: EntityPlayer): Unit = inventory.openInventory(player)

  override def closeInventory(player: EntityPlayer): Unit = inventory.closeInventory(player)

  override def setField(id: Int, value: Int): Unit = inventory.setField(id, value)

  override def clear(): Unit = inventory.clear()

  override def getFieldCount: Int = inventory.getFieldCount

  override def getField(id: Int): Int = inventory.getField(id)

  private def isValidSlot(slot: Int) = slot >= offset && slot < getSizeInventory + offset
}

package li.cil.oc.common.inventory

import net.minecraft.entity.player.EntityPlayer
import net.minecraft.inventory.IInventory
import net.minecraft.item.ItemStack

trait InventoryProxy extends IInventory {
  def inventory: IInventory

  def offset = 0

  override def getSizeInventory = inventory.getSizeInventory

  override def getInventoryStackLimit = inventory.getInventoryStackLimit

  override def getName = inventory.getName

  override def getDisplayName = inventory.getDisplayName

  override def hasCustomName = inventory.hasCustomName

  override def isUseableByPlayer(player: EntityPlayer) = inventory.isUseableByPlayer(player)

  override def isItemValidForSlot(slot: Int, stack: ItemStack) = {
    val offsetSlot = slot + offset
    isValidSlot(offsetSlot) && inventory.isItemValidForSlot(offsetSlot, stack)
  }

  override def getStackInSlot(slot: Int) = {
    val offsetSlot = slot + offset
    if (isValidSlot(offsetSlot)) inventory.getStackInSlot(offsetSlot)
    else null
  }

  override def decrStackSize(slot: Int, amount: Int) = {
    val offsetSlot = slot + offset
    if (isValidSlot(offsetSlot)) inventory.decrStackSize(offsetSlot, amount)
    else null
  }

  override def removeStackFromSlot(slot: Int) = {
    val offsetSlot = slot + offset
    if (isValidSlot(offsetSlot)) inventory.removeStackFromSlot(offsetSlot)
    else null
  }

  override def setInventorySlotContents(slot: Int, stack: ItemStack) = {
    val offsetSlot = slot + offset
    if (isValidSlot(offsetSlot)) inventory.setInventorySlotContents(offsetSlot, stack)
  }

  override def markDirty() = inventory.markDirty()

  override def openInventory(player: EntityPlayer) = inventory.openInventory(player)

  override def closeInventory(player: EntityPlayer) = inventory.closeInventory(player)

  override def setField(id: Int, value: Int) = inventory.setField(id, value)

  override def clear() = inventory.clear()

  override def getFieldCount = inventory.getFieldCount

  override def getField(id: Int) = inventory.getField(id)

  private def isValidSlot(slot: Int) = slot >= offset && slot < getSizeInventory + offset
}

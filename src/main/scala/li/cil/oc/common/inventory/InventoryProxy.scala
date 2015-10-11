package li.cil.oc.common.inventory

import net.minecraft.entity.player.EntityPlayer
import net.minecraft.inventory.IInventory
import net.minecraft.item.ItemStack

trait InventoryProxy extends IInventory {
  def inventory: IInventory

  def offset = 0

  override def getSizeInventory = inventory.getSizeInventory

  override def getInventoryStackLimit = inventory.getInventoryStackLimit

  override def getInventoryName = inventory.getInventoryName

  override def hasCustomInventoryName = inventory.hasCustomInventoryName

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

  override def getStackInSlotOnClosing(slot: Int) = {
    val offsetSlot = slot + offset
    if (isValidSlot(offsetSlot)) inventory.getStackInSlotOnClosing(offsetSlot)
    else null
  }

  override def setInventorySlotContents(slot: Int, stack: ItemStack) = {
    val offsetSlot = slot + offset
    if (isValidSlot(offsetSlot)) inventory.setInventorySlotContents(offsetSlot, stack)
  }

  override def markDirty() = inventory.markDirty()

  override def openInventory() = inventory.openInventory()

  override def closeInventory() = inventory.closeInventory()

  private def isValidSlot(slot: Int) = slot >= offset && slot < getSizeInventory + offset
}

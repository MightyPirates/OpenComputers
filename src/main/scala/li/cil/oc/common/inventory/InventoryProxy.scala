package li.cil.oc.common.inventory

import net.minecraft.entity.player.EntityPlayer
import net.minecraft.inventory.IInventory
import net.minecraft.item.ItemStack

trait InventoryProxy extends IInventory {
  def proxiedInventory: IInventory

  override def getSizeInventory = proxiedInventory.getSizeInventory

  override def getInventoryStackLimit = proxiedInventory.getInventoryStackLimit

  override def hasCustomInventoryName = proxiedInventory.hasCustomInventoryName

  override def getInventoryName = proxiedInventory.getInventoryName

  override def markDirty() = proxiedInventory.markDirty()

  override def getStackInSlot(slot: Int) = proxiedInventory.getStackInSlot(slot)

  override def setInventorySlotContents(slot: Int, stack: ItemStack) = proxiedInventory.setInventorySlotContents(slot, stack)

  override def decrStackSize(slot: Int, amount: Int) = proxiedInventory.decrStackSize(slot, amount)

  override def openInventory() = proxiedInventory.openInventory()

  override def closeInventory() = proxiedInventory.closeInventory()

  override def getStackInSlotOnClosing(slot: Int) = proxiedInventory.getStackInSlotOnClosing(slot)

  override def isItemValidForSlot(slot: Int, stack: ItemStack) = proxiedInventory.isItemValidForSlot(slot, stack)

  override def isUseableByPlayer(player: EntityPlayer) = proxiedInventory.isUseableByPlayer(player)
}

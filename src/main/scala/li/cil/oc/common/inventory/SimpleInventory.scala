package li.cil.oc.common.inventory

import net.minecraft.inventory.IInventory
import net.minecraft.item.ItemStack

trait SimpleInventory extends IInventory {
  override def hasCustomInventoryName = false

  override def getInventoryStackLimit = 64

  // Items required in a slot before it's set to null (for ghost stacks).
  def getInventoryStackRequired = 1

  override def openInventory(): Unit = {}

  override def closeInventory(): Unit = {}

  override def decrStackSize(slot: Int, amount: Int): ItemStack = {
    if (slot >= 0 && slot < getSizeInventory) {
      (getStackInSlot(slot) match {
        case stack: ItemStack if stack.stackSize - amount < getInventoryStackRequired =>
          setInventorySlotContents(slot, null)
          stack
        case stack: ItemStack =>
          val result = stack.splitStack(amount)
          markDirty()
          result
        case _ => null
      }) match {
        case stack: ItemStack if stack.stackSize > 0 => stack
        case _ => null
      }
    }
    else null
  }

  override def getStackInSlotOnClosing(slot: Int) = {
    if (slot >= 0 && slot < getSizeInventory) {
      val stack = getStackInSlot(slot)
      setInventorySlotContents(slot, null)
      stack
    }
    else null
  }
}

package li.cil.oc.common.inventory

import li.cil.oc.Localization
import net.minecraft.entity.player.EntityPlayer
import net.minecraft.inventory.IInventory
import net.minecraft.item.ItemStack
import net.minecraft.util.text.ITextComponent

trait SimpleInventory extends IInventory {
  override def hasCustomName = false

  override def getDisplayName: ITextComponent = Localization.localizeLater(getName)

  override def getInventoryStackLimit = 64

  // Items required in a slot before it's set to null (for ghost stacks).
  def getInventoryStackRequired = 1

  override def openInventory(player: EntityPlayer): Unit = {}

  override def closeInventory(player: EntityPlayer): Unit = {}

  override def decrStackSize(slot: Int, amount: Int): ItemStack = {
    if (slot >= 0 && slot < getSizeInventory) {
      (getStackInSlot(slot) match {
        case stack: ItemStack if stack.getCount - amount < getInventoryStackRequired =>
          setInventorySlotContents(slot, null)
          stack
        case stack: ItemStack =>
          val result = stack.splitStack(amount)
          markDirty()
          result
        case _ => null
      }) match {
        case stack: ItemStack if stack.getCount > 0 => stack
        case _ => null
      }
    }
    else null
  }

  override def removeStackFromSlot(slot: Int) = {
    if (slot >= 0 && slot < getSizeInventory) {
      val stack = getStackInSlot(slot)
      setInventorySlotContents(slot, null)
      stack
    }
    else null
  }

  override def clear(): Unit = {
    for (slot <- 0 until getSizeInventory) {
      setInventorySlotContents(slot, null)
    }
  }

  override def getField(id: Int) = 0

  override def setField(id: Int, value: Int) {}

  override def getFieldCount = 0
}

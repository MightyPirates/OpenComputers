package li.cil.oc.common.inventory

import li.cil.oc.Settings
import li.cil.oc.util.ExtendedNBT._
import net.minecraft.inventory.IInventory
import net.minecraft.item.ItemStack
import net.minecraft.nbt.NBTTagCompound

trait Inventory extends IInventory {
  def items: Array[Option[ItemStack]]

  // ----------------------------------------------------------------------- //

  def getStackInSlot(slot: Int) = items(slot).orNull

  def decrStackSize(slot: Int, amount: Int) = items(slot) match {
    case Some(stack) if stack.stackSize - amount < getInventoryStackRequired =>
      setInventorySlotContents(slot, null)
      stack
    case Some(stack) =>
      val result = stack.splitStack(amount)
      onInventoryChanged()
      result
    case _ => null
  }

  def setInventorySlotContents(slot: Int, stack: ItemStack) = {
    if (items(slot).isDefined) {
      onItemRemoved(slot, items(slot).get)
    }

    if (stack == null || stack.stackSize < getInventoryStackRequired) {
      items(slot) = None
    }
    else {
      items(slot) = Some(stack)
    }
    if (stack != null && stack.stackSize > getInventoryStackLimit) {
      stack.stackSize = getInventoryStackLimit
    }

    if (items(slot).isDefined) {
      onItemAdded(slot, items(slot).get)
    }

    onInventoryChanged()
  }

  def getInventoryStackRequired = 1

  def getStackInSlotOnClosing(slot: Int) = null

  def openChest() {}

  def closeChest() {}

  def isInvNameLocalized = false

  // ----------------------------------------------------------------------- //

  def load(nbt: NBTTagCompound) {
    nbt.getTagList(Settings.namespace + "items").foreach[NBTTagCompound](slotNbt => {
      val slot = slotNbt.getByte("slot")
      if (slot >= 0 && slot < items.length) {
        items(slot) = Option(ItemStack.loadItemStackFromNBT(slotNbt.getCompoundTag("item")))
      }
    })
  }

  def save(nbt: NBTTagCompound) {
    nbt.setNewTagList(Settings.namespace + "items",
      items.zipWithIndex collect {
        case (Some(stack), slot) => (stack, slot)
      } map {
        case (stack, slot) =>
          val slotNbt = new NBTTagCompound()
          slotNbt.setByte("slot", slot.toByte)
          slotNbt.setNewCompoundTag("item", stack.writeToNBT)
      })
  }

  // ----------------------------------------------------------------------- //

  protected def onItemAdded(slot: Int, stack: ItemStack) {}

  protected def onItemRemoved(slot: Int, stack: ItemStack) {}
}

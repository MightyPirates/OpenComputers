package li.cil.oc.common.inventory

import li.cil.oc.Settings
import li.cil.oc.util.ExtendedNBT._
import net.minecraft.inventory.IInventory
import net.minecraft.item.ItemStack
import net.minecraft.nbt.NBTTagCompound
import net.minecraftforge.common.util.Constants.NBT

trait Inventory extends IInventory {
  def items: Array[Option[ItemStack]]

  def updateItems(slot: Int, stack: ItemStack) = items(slot) = Option(stack)

  // ----------------------------------------------------------------------- //

  override def getStackInSlot(slot: Int) =
    if (slot >= 0 && slot < getSizeInventory) items(slot).orNull
    else null

  override def decrStackSize(slot: Int, amount: Int) =
    if (slot >= 0 && slot < getSizeInventory) {
      (items(slot) match {
        case Some(stack) if stack.stackSize - amount < getInventoryStackRequired =>
          setInventorySlotContents(slot, null)
          stack
        case Some(stack) =>
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

  override def setInventorySlotContents(slot: Int, stack: ItemStack): Unit = {
    if (slot >= 0 && slot < getSizeInventory) {
      if (stack == null && items(slot).isEmpty) {
        return
      }
      if (items(slot).contains(stack)) {
        return
      }

      val oldStack = items(slot)
      updateItems(slot, null)
      if (oldStack.isDefined) {
        onItemRemoved(slot, oldStack.get)
      }
      if (stack != null && stack.stackSize >= getInventoryStackRequired) {
        if (stack.stackSize > getInventoryStackLimit) {
          stack.stackSize = getInventoryStackLimit
        }
        updateItems(slot, stack)
      }

      if (items(slot).isDefined) {
        onItemAdded(slot, items(slot).get)
      }

      markDirty()
    }
  }

  def getInventoryStackRequired = 1

  override def getStackInSlotOnClosing(slot: Int) = null

  override def openInventory() {}

  override def closeInventory() {}

  override def hasCustomInventoryName = false

  override def getInventoryName = Settings.namespace + "container." + inventoryName

  protected def inventoryName = getClass.getSimpleName

  // ----------------------------------------------------------------------- //

  def load(nbt: NBTTagCompound) {
    nbt.getTagList(Settings.namespace + "items", NBT.TAG_COMPOUND).foreach((slotNbt: NBTTagCompound) => {
      val slot = slotNbt.getByte("slot")
      if (slot >= 0 && slot < items.length) {
        updateItems(slot, ItemStack.loadItemStackFromNBT(slotNbt.getCompoundTag("item")))
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

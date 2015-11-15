package li.cil.oc.common.inventory

import li.cil.oc.Settings
import li.cil.oc.util.ExtendedNBT._
import net.minecraft.item.ItemStack
import net.minecraft.nbt.NBTTagCompound
import net.minecraftforge.common.util.Constants.NBT

trait Inventory extends SimpleInventory {
  def items: Array[Option[ItemStack]]

  def updateItems(slot: Int, stack: ItemStack) = items(slot) = Option(stack)

  // ----------------------------------------------------------------------- //

  override def getStackInSlot(slot: Int) =
    if (slot >= 0 && slot < getSizeInventory) items(slot).orNull
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

  override def getInventoryName = Settings.namespace + "container." + inventoryName

  protected def inventoryName = getClass.getSimpleName

  // ----------------------------------------------------------------------- //

  def load(nbt: NBTTagCompound) {
    // Implicit slot numbers are compatibility code for loading old server save format.
    // TODO 1.7 remove compat code.
    var count = 0
    nbt.getTagList(Settings.namespace + "items", NBT.TAG_COMPOUND).foreach((tag: NBTTagCompound) => {
      if (tag.hasKey("slot")) {
        val slot = tag.getByte("slot")
        if (slot >= 0 && slot < items.length) {
          updateItems(slot, ItemStack.loadItemStackFromNBT(tag.getCompoundTag("item")))
        }
      }
      else {
        val slot = count
        if (slot >= 0 && slot < items.length) {
          updateItems(slot, ItemStack.loadItemStackFromNBT(tag))
        }
      }
      count += 1
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

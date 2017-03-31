package li.cil.oc.common.inventory

import li.cil.oc.Settings
import li.cil.oc.util.ExtendedNBT._
import net.minecraft.item.ItemStack
import net.minecraft.nbt.NBTTagCompound
import net.minecraftforge.common.util.Constants.NBT

trait Inventory extends SimpleInventory {
  def items: Array[ItemStack]

  def updateItems(slot: Int, stack: ItemStack): Unit = items(slot) = stack

  // ----------------------------------------------------------------------- //

  override def getStackInSlot(slot: Int): ItemStack =
    if (slot >= 0 && slot < getSizeInventory) items(slot)
    else ItemStack.EMPTY

  override def setInventorySlotContents(slot: Int, stack: ItemStack): Unit = {
    if (slot >= 0 && slot < getSizeInventory) {
      if (stack.isEmpty && items(slot).isEmpty) {
        return
      }
      if (items(slot) == stack) {
        return
      }

      val oldStack = items(slot)
      updateItems(slot, ItemStack.EMPTY)
      if (!oldStack.isEmpty) {
        onItemRemoved(slot, oldStack)
      }
      if (!stack.isEmpty && stack.getCount >= getInventoryStackRequired) {
        if (stack.getCount > getInventoryStackLimit) {
          stack.setCount(getInventoryStackLimit)
        }
        updateItems(slot, stack)
      }

      if (!items(slot).isEmpty) {
        onItemAdded(slot, items(slot))
      }

      markDirty()
    }
  }

  override def getName: String = Settings.namespace + "container." + inventoryName

  protected def inventoryName: String = getClass.getSimpleName

  override def isEmpty: Boolean = items.forall(_.isEmpty)

  // ----------------------------------------------------------------------- //

  private final val ItemsTag = Settings.namespace + "items"
  private final val SlotTag = "slot"
  private final val ItemTag = "item"

  def load(nbt: NBTTagCompound) {
    nbt.getTagList(ItemsTag, NBT.TAG_COMPOUND).foreach((tag: NBTTagCompound) => {
      if (tag.hasKey(SlotTag)) {
        val slot = tag.getByte(SlotTag)
        if (slot >= 0 && slot < items.length) {
          updateItems(slot, new ItemStack(tag.getCompoundTag(ItemTag)))
        }
      }
    })
  }

  def save(nbt: NBTTagCompound) {
    nbt.setNewTagList(ItemsTag,
      items.zipWithIndex collect {
        case (stack, slot) if !stack.isEmpty => (stack, slot)
      } map {
        case (stack, slot) =>
          val slotNbt = new NBTTagCompound()
          slotNbt.setByte(SlotTag, slot.toByte)
          slotNbt.setNewCompoundTag(ItemTag, stack.writeToNBT)
      })
  }

  // ----------------------------------------------------------------------- //

  protected def onItemAdded(slot: Int, stack: ItemStack) {}

  protected def onItemRemoved(slot: Int, stack: ItemStack) {}
}

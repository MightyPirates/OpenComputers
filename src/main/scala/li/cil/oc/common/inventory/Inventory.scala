package li.cil.oc.common.inventory

import li.cil.oc.Settings
import li.cil.oc.util.ExtendedNBT._
import li.cil.oc.util.StackOption
import net.minecraft.item.ItemStack
import net.minecraft.nbt.CompoundNBT
import net.minecraft.util.text.ITextComponent
import net.minecraft.util.text.TranslationTextComponent
import net.minecraftforge.common.util.Constants.NBT

trait Inventory extends SimpleInventory {
  def items: Array[ItemStack]

  def updateItems(slot: Int, stack: ItemStack): Unit = items(slot) = StackOption(stack).orEmpty

  // ----------------------------------------------------------------------- //

  override def getItem(slot: Int): ItemStack =
    if (slot >= 0 && slot < getContainerSize) items(slot)
    else ItemStack.EMPTY

  override def setItem(slot: Int, stack: ItemStack): Unit = {
    if (slot >= 0 && slot < getContainerSize) {
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
        if (stack.getCount > getMaxStackSize) {
          stack.setCount(getMaxStackSize)
        }
        updateItems(slot, stack)
      }

      if (!items(slot).isEmpty) {
        onItemAdded(slot, items(slot))
      }

      setChanged()
    }
  }

  override def getName: ITextComponent = new TranslationTextComponent(Settings.namespace + "container." + inventoryName)

  protected def inventoryName: String = getClass.getSimpleName.toLowerCase

  override def isEmpty: Boolean = items.forall(_.isEmpty)

  // ----------------------------------------------------------------------- //

  private final val ItemsTag = Settings.namespace + "items"
  private final val SlotTag = "slot"
  private final val ItemTag = "item"

  def loadData(nbt: CompoundNBT) {
    nbt.getList(ItemsTag, NBT.TAG_COMPOUND).foreach((tag: CompoundNBT) => {
      if (tag.contains(SlotTag)) {
        val slot = tag.getByte(SlotTag)
        if (slot >= 0 && slot < items.length) {
          updateItems(slot, ItemStack.of(tag.getCompound(ItemTag)))
        }
      }
    })
  }

  def saveData(nbt: CompoundNBT) {
    nbt.setNewTagList(ItemsTag,
      items.zipWithIndex collect {
        case (stack, slot) if !stack.isEmpty => (stack, slot)
      } map {
        case (stack, slot) =>
          val slotNbt = new CompoundNBT()
          slotNbt.putByte(SlotTag, slot.toByte)
          slotNbt.setNewCompoundTag(ItemTag, stack.save)
      })
  }

  // ----------------------------------------------------------------------- //

  protected def onItemAdded(slot: Int, stack: ItemStack) {}

  protected def onItemRemoved(slot: Int, stack: ItemStack) {}
}

package li.cil.oc.common.inventory

import net.minecraft.item.ItemStack
import net.minecraft.nbt.NBTTagCompound

trait ItemStackInventory extends Inventory {
  // The item stack that provides the inventory.
  def container: ItemStack

  private lazy val inventory = Array.fill[ItemStack](getSizeInventory)(ItemStack.EMPTY)

  override def items = inventory

  // Initialize the list automatically if we have a container.
  {
    val _container = container
    if (_container != null && !_container.isEmpty) {
      reinitialize()
    }
  }

  // Load items from tag.
  def reinitialize() {
    for (i <- items.indices) {
      updateItems(i, ItemStack.EMPTY)
    }
    if (!container.hasTagCompound) {
      container.setTagCompound(new NBTTagCompound())
    }
    load(container.getTagCompound)
  }

  // Write items back to tag.
  override def markDirty() {
    if (!container.hasTagCompound) {
      container.setTagCompound(new NBTTagCompound())
    }
    save(container.getTagCompound)
  }
}

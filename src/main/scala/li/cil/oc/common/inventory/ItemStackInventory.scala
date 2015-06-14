package li.cil.oc.common.inventory

import li.cil.oc.Settings
import net.minecraft.item.ItemStack
import net.minecraft.nbt.NBTTagCompound
import net.minecraft.nbt.NBTTagList
import net.minecraftforge.common.util.Constants.NBT

trait ItemStackInventory extends Inventory {
  // The item stack that provides the inventory.
  def container: ItemStack

  private lazy val inventory = Array.fill[Option[ItemStack]](getSizeInventory)(None)

  override def items = inventory

  // Initialize the list automatically if we have a container.
  if (container != null) {
    reinitialize()
  }

  // Load items from tag.
  def reinitialize() {
    if (!container.hasTagCompound) {
      container.setTagCompound(new NBTTagCompound())
    }
    for (i <- items.indices) {
      updateItems(i, null)
    }
    if (container.getTagCompound.hasKey(Settings.namespace + "items")) {
      val list = container.getTagCompound.getTagList(Settings.namespace + "items", NBT.TAG_COMPOUND)
      for (i <- 0 until (list.tagCount min items.length)) {
        val tag = list.getCompoundTagAt(i)
        if (!tag.hasNoTags) {
          updateItems(i, ItemStack.loadItemStackFromNBT(tag))
        }
      }
    }
  }

  // Write items back to tag.
  override def markDirty() {
    val list = new NBTTagList()
    for (i <- items.indices) {
      val tag = new NBTTagCompound()
      items(i) match {
        case Some(stack) => stack.writeToNBT(tag)
        case _ =>
      }
      list.appendTag(tag)
    }
    container.getTagCompound.setTag(Settings.namespace + "items", list)
  }
}

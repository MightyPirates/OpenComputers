package li.cil.oc.common.inventory

import li.cil.oc.Settings
import net.minecraft.item.ItemStack
import net.minecraft.nbt.{NBTTagCompound, NBTTagList}

trait ItemStackInventory extends Inventory {
  // The item stack that provides the inventory.
  def container: ItemStack

  lazy val items = Array.fill[Option[ItemStack]](getSizeInventory)(None)

  // Initialize the list automatically if we have a container.
  if (container != null) {
    reinitialize()
  }

  // Load items from tag.
  def reinitialize() {
    if (!container.hasTagCompound) {
      container.setTagCompound(new NBTTagCompound("tag"))
    }
    for (i <- 0 until items.length) {
      items(i) = None
    }
    if (container.getTagCompound.hasKey(Settings.namespace + "items")) {
      val list = container.getTagCompound.getTagList(Settings.namespace + "items")
      for (i <- 0 until (list.tagCount min items.length)) {
        val tag = list.tagAt(i).asInstanceOf[NBTTagCompound]
        if (!tag.hasNoTags) {
          items(i) = Option(ItemStack.loadItemStackFromNBT(tag))
        }
      }
    }
  }

  // Write items back to tag.
  override def onInventoryChanged() {
    val list = new NBTTagList()
    for (i <- 0 until items.length) {
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

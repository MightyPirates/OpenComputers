package li.cil.oc.common.inventory

import li.cil.oc.Settings
import net.minecraft.entity.player.EntityPlayer
import net.minecraft.inventory.IInventory
import net.minecraft.item.ItemStack
import net.minecraft.nbt.{NBTTagList, NBTTagCompound}

abstract class ItemStackInventory extends IInventory {
  // The item stack that provides the inventory.
  def container: ItemStack

  protected val items = Array.fill[Option[ItemStack]](getSizeInventory)(None)

  // Load items from tag.
  {
    if (!container.hasTagCompound) {
      container.setTagCompound(new NBTTagCompound("tag"))
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

  def getStackInSlot(slot: Int) = items(slot).orNull

  def decrStackSize(slot: Int, amount: Int) = items(slot) match {
    case Some(stack) if stack.stackSize - amount < 1 =>
      setInventorySlotContents(slot, null)
      stack
    case Some(stack) =>
      val result = stack.splitStack(amount)
      onInventoryChanged()
      result
    case _ => null
  }

  def getStackInSlotOnClosing(slot: Int) = null

  def setInventorySlotContents(slot: Int, stack: ItemStack) = {
    if (stack == null || stack.stackSize < 1) {
      items(slot) = None
    }
    else {
      items(slot) = Some(stack)
    }
    if (stack != null && stack.stackSize > getInventoryStackLimit) {
      stack.stackSize = getInventoryStackLimit
    }

    onInventoryChanged()
  }

  def isInvNameLocalized = false

  def getInventoryStackLimit = 64

  def onInventoryChanged() {
    // Write items back to tag.
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

  def isUseableByPlayer(player: EntityPlayer) = true

  def openChest() {}

  def closeChest() {}

  def isItemValidForSlot(slot: Int, stack: ItemStack) = true
}

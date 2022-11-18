package li.cil.oc.common.inventory

import li.cil.oc.Localization
import net.minecraft.entity.player.PlayerEntity
import net.minecraft.inventory.IInventory
import net.minecraft.item.ItemStack
import net.minecraft.util.INameable
import net.minecraft.util.text.ITextComponent

trait SimpleInventory extends IInventory with INameable {
  override def hasCustomName = false

  override def getDisplayName: ITextComponent = getName

  override def getMaxStackSize = 64

  // Items required in a slot before it's set to null (for ghost stacks).
  def getInventoryStackRequired = 1

  override def startOpen(player: PlayerEntity): Unit = {}

  override def stopOpen(player: PlayerEntity): Unit = {}

  override def removeItem(slot: Int, amount: Int): ItemStack = {
    if (slot >= 0 && slot < getContainerSize) {
      (getItem(slot) match {
        case stack: ItemStack if stack.getCount - amount < getInventoryStackRequired =>
          setItem(slot, ItemStack.EMPTY)
          stack
        case stack: ItemStack =>
          val result = stack.split(amount)
          setChanged()
          result
        case _ => ItemStack.EMPTY
      }) match {
        case stack: ItemStack if stack.getCount > 0 => stack
        case _ => ItemStack.EMPTY
      }
    }
    else ItemStack.EMPTY
  }

  override def removeItemNoUpdate(slot: Int) = {
    if (slot >= 0 && slot < getContainerSize) {
      val stack = getItem(slot)
      setItem(slot, ItemStack.EMPTY)
      stack
    }
    else ItemStack.EMPTY
  }

  override def clearContent(): Unit = {
    for (slot <- 0 until getContainerSize) {
      setItem(slot, ItemStack.EMPTY)
    }
  }
}

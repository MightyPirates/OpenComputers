package li.cil.oc.common.item.data

import li.cil.oc.api
import li.cil.oc.api.Persistable
import net.minecraft.item.ItemStack
import net.minecraft.nbt.CompoundNBT

abstract class ItemData(val itemName: String) extends Persistable {
  def loadData(stack: ItemStack) {
    if (stack.hasTag) {
      // Because ItemStack's load function doesn't copy the compound tag,
      // but keeps it as is, leading to oh so fun bugs!
      loadData(stack.getTag.copy().asInstanceOf[CompoundNBT])
    }
  }

  def saveData(stack: ItemStack) {
    saveData(stack.getOrCreateTag)
  }

  def createItemStack() = {
    if (itemName == null) ItemStack.EMPTY
    else {
      val stack = api.Items.get(itemName).createItemStack(1)
      saveData(stack)
      stack
    }
  }
}

package li.cil.oc.common.item.data

import li.cil.oc.api.Persistable
import net.minecraft.item.ItemStack
import net.minecraft.nbt.NBTTagCompound

object ItemData {

}

abstract class ItemData extends Persistable {
  def load(stack: ItemStack) {
    if (stack.hasTagCompound) {
      // Because ItemStack's load function doesn't copy the compound tag,
      // but keeps it as is, leading to oh so fun bugs!
      load(stack.getTagCompound.copy().asInstanceOf[NBTTagCompound])
    }
  }

  def save(stack: ItemStack) {
    if (!stack.hasTagCompound) {
      stack.setTagCompound(new NBTTagCompound())
    }
    save(stack.getTagCompound)
  }
}

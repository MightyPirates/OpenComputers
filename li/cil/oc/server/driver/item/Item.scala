package li.cil.oc.server.driver.item

import li.cil.oc.common
import li.cil.oc.{Settings, Items, api}
import net.minecraft.item.ItemStack
import net.minecraft.nbt.NBTTagCompound

trait Item extends api.driver.Item {
  def nbt(stack: ItemStack) = {
    if (!stack.hasTagCompound) {
      stack.setTagCompound(new NBTTagCompound("tag"))
    }
    val nbt = stack.getTagCompound
    if (!nbt.hasKey(Settings.namespace + "data")) {
      nbt.setCompoundTag(Settings.namespace + "data", new NBTTagCompound())
    }
    nbt.getCompoundTag(Settings.namespace + "data")
  }

  protected def isOneOf(stack: ItemStack, items: common.item.Delegate*) =
    stack.getItem == Items.multi && (Items.multi.subItem(stack) match {
      case None => false
      case Some(subItem) => items.contains(subItem)
    })
}

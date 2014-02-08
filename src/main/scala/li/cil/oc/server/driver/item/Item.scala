package li.cil.oc.server.driver.item

import li.cil.oc.api.driver
import li.cil.oc.common
import li.cil.oc.{Settings, Items}
import net.minecraft.item.ItemStack
import net.minecraft.nbt.NBTTagCompound

trait Item extends driver.Item {
  override def tier(stack: ItemStack) = 0

  override def dataTag(stack: ItemStack) = Item.dataTag(stack)

  protected def isOneOf(stack: ItemStack, items: common.item.Delegate*) =
    Items.multi.subItem(stack) match {
      case Some(subItem) => items.contains(subItem)
      case _ => false
    }
}

object Item {
  def dataTag(stack: ItemStack) = {
    if (!stack.hasTagCompound) {
      stack.setTagCompound(new NBTTagCompound())
    }
    val nbt = stack.getTagCompound
    if (!nbt.hasKey(Settings.namespace + "data")) {
      nbt.setTag(Settings.namespace + "data", new NBTTagCompound())
    }
    nbt.getCompoundTag(Settings.namespace + "data")
  }
}
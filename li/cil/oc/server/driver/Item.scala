package li.cil.oc.server.driver

import li.cil.oc.common.item
import li.cil.oc.{Items, api}
import net.minecraft.item.ItemStack
import net.minecraft.nbt.NBTTagCompound

trait Item extends api.driver.Item {
  def nbt(item: ItemStack) = {
    if (!item.hasTagCompound)
      item.setTagCompound(new NBTTagCompound())
    val nbt = item.getTagCompound
    if (!nbt.hasKey("oc.node")) {
      nbt.setCompoundTag("oc.node", new NBTTagCompound())
    }
    nbt.getCompoundTag("oc.node")
  }

  protected def isOneOf(stack: ItemStack, items: item.Delegate*) =
    stack.itemID == Items.multi.itemID && (Items.multi.subItem(stack) match {
      case None => false
      case Some(subItem) => items.contains(subItem)
    })
}

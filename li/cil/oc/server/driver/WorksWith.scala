package li.cil.oc.server.driver

import li.cil.oc.Items
import li.cil.oc.common.item
import net.minecraft.item.ItemStack

case class WorksWith(items: item.Delegate*) {
  def apply(item: ItemStack) =
    item.itemID == Items.multi.itemID && (Items.multi.subItem(item) match {
      case None => false
      case Some(subItem) => items.contains(subItem)
    })
}

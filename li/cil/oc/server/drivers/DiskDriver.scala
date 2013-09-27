package li.cil.oc.server.drivers

import li.cil.oc.Items
import li.cil.oc.api.{IItemDriver, ComponentType}
import net.minecraft.item.ItemStack

object DiskDriver extends IItemDriver {
  override def api = null

  override def worksWith(item: ItemStack) = item.itemID == Items.multi.itemID && (Items.multi.subItem(item) match {
    case None => false
    case Some(subItem) => subItem.itemId == Items.hdd.itemId
  })

  override def componentType(item: ItemStack) = ComponentType.HDD

  override def node(item: ItemStack) = null
}
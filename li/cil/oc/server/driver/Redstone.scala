package li.cil.oc.server.driver

import li.cil.oc.Items
import li.cil.oc.api.driver
import li.cil.oc.api.driver.Slot
import li.cil.oc.common.util.ItemComponentCache
import li.cil.oc.server.component.RedstoneCard
import net.minecraft.item.ItemStack

object Redstone extends driver.Item {
  override def api = Option(getClass.getResourceAsStream("/assets/opencomputers/lua/redstone.lua"))

  override def worksWith(item: ItemStack) = item.itemID == Items.multi.itemID && (Items.multi.subItem(item) match {
    case None => false
    case Some(subItem) => subItem.itemId == Items.rs.itemId
  })

  override def slot(item: ItemStack) = Slot.PCI

  override def node(item: ItemStack) = ItemComponentCache.get[RedstoneCard](item)
}

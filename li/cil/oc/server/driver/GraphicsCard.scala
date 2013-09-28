package li.cil.oc.server.driver

import li.cil.oc.Items
import li.cil.oc.api.driver
import li.cil.oc.api.driver.Slot
import li.cil.oc.common.util.ItemComponentCache
import li.cil.oc.server.component.GraphicsCard
import net.minecraft.item.ItemStack

object GraphicsCard extends driver.Item {
  override def api = Option(getClass.getResourceAsStream("/assets/opencomputers/lua/gpu.lua"))

  override def worksWith(item: ItemStack) = item.itemID == Items.multi.itemID && (Items.multi.subItem(item) match {
    case None => false
    case Some(subItem) => subItem.itemId == Items.gpu.itemId
  })

  override def slot(item: ItemStack) = Slot.PCI

  override def node(item: ItemStack) = ItemComponentCache.get[GraphicsCard](item)
}
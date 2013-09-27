package li.cil.oc.server.drivers

import li.cil.oc.Items
import li.cil.oc.api.{ComponentType, IItemDriver}
import li.cil.oc.common.util.ItemComponentCache
import li.cil.oc.server.components.RedstoneCard
import net.minecraft.item.ItemStack

object RedstoneDriver extends IItemDriver {
  override def api = Option(getClass.getResourceAsStream("/assets/opencomputers/lua/redstone.lua"))

  override def worksWith(item: ItemStack) = item.itemID == Items.multi.itemID && (Items.multi.subItem(item) match {
    case None => false
    case Some(subItem) => subItem.itemId == Items.rs.itemId
  })

  override def componentType(item: ItemStack) = ComponentType.PCI

  override def node(item: ItemStack) = ItemComponentCache.get[RedstoneCard](item)
}

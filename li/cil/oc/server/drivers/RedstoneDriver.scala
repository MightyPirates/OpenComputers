package li.cil.oc.server.drivers

import li.cil.oc.api.{ComponentType, IItemDriver}
import li.cil.oc.common.util.ItemComponentCache
import li.cil.oc.server.components.RedstoneCard
import net.minecraft.item.ItemStack
import li.cil.oc.Items

object RedstoneDriver extends IItemDriver {
  override def api = Option(getClass.getResourceAsStream("/assets/opencomputers/lua/redstone.lua"))

  override def worksWith(item: ItemStack) = item.itemID == Items.rs.itemID

  override def componentType(item: ItemStack) = ComponentType.PCI

  override def node(item: ItemStack) = ItemComponentCache.get[RedstoneCard](item)
}

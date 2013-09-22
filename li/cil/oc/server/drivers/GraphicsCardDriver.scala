package li.cil.oc.server.drivers

import li.cil.oc.Items
import li.cil.oc.api.{IItemDriver, ComponentType}
import li.cil.oc.common.util.ItemComponentCache
import li.cil.oc.server.components.GraphicsCard
import net.minecraft.item.ItemStack

object GraphicsCardDriver extends IItemDriver {
  override def api = Option(getClass.getResourceAsStream("/assets/opencomputers/lua/gpu.lua"))

  override def worksWith(item: ItemStack) = item.itemID == Items.gpu.itemID

  override def componentType(item: ItemStack) = ComponentType.PCI

  override def node(item: ItemStack) = ItemComponentCache.get[GraphicsCard](item)
}
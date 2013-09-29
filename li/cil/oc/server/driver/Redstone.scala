package li.cil.oc.server.driver

import li.cil.oc.api.driver
import li.cil.oc.api.driver.Slot
import li.cil.oc.common.util.ItemComponentCache
import li.cil.oc.server.component.RedstoneCard
import li.cil.oc.{Config, Items}
import net.minecraft.item.ItemStack

object Redstone extends driver.Item {
  override def api = Option(getClass.getResourceAsStream(Config.driverPath + "redstone.lua"))

  override def worksWith(item: ItemStack) = WorksWith(Items.rs)(item)

  override def slot(item: ItemStack) = Slot.PCI

  override def node(item: ItemStack) = ItemComponentCache.get[RedstoneCard](item)
}

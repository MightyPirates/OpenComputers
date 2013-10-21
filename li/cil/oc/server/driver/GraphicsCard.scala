package li.cil.oc.server.driver

import li.cil.oc.api.driver
import li.cil.oc.api.driver.Slot
import li.cil.oc.common
import li.cil.oc.server.component
import li.cil.oc.{Config, Items}
import net.minecraft.item.ItemStack

object GraphicsCard extends driver.Item {
  override def api = Option(getClass.getResourceAsStream(Config.driverPath + "gpu.lua"))

  override def worksWith(item: ItemStack) = WorksWith(Items.gpu1, Items.gpu2, Items.gpu3)(item)

  override def slot(item: ItemStack) = Slot.Card

  override def node(item: ItemStack) =
    Items.multi.subItem(item) match {
      case Some(gpu: common.item.GraphicsCard) =>
        Some(new component.GraphicsCard(gpu.resolutions))
      case _ => throw new IllegalArgumentException()
    }
}
package li.cil.oc.server.driver

import li.cil.oc.Items
import li.cil.oc.api.driver.Slot
import li.cil.oc.common
import li.cil.oc.server.component
import net.minecraft.item.ItemStack

object GraphicsCard extends Item {
  override def worksWith(item: ItemStack) = isOneOf(item, Items.gpu1, Items.gpu2, Items.gpu3)

  override def createEnvironment(item: ItemStack) =
    Items.multi.subItem(item) match {
      case Some(gpu: common.item.GraphicsCard) =>
        new component.GraphicsCard(gpu.maxResolution)
      case _ => null
    }

  override def slot(item: ItemStack) = Slot.Card
}
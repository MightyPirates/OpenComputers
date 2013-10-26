package li.cil.oc.server.driver

import li.cil.oc.api.driver.Slot
import li.cil.oc.server.component
import li.cil.oc.{Config, Items}
import net.minecraft.item.ItemStack

object RedstoneCard extends Item {
  override def worksWith(item: ItemStack) = WorksWith(Items.rs)(item)

  override def createEnvironment(item: ItemStack) = new component.RedstoneCard()

  override def slot(item: ItemStack) = Slot.Card
}

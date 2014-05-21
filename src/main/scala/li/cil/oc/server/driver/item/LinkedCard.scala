package li.cil.oc.server.driver.item

import li.cil.oc.api
import li.cil.oc.api.driver.Slot
import li.cil.oc.server.component
import net.minecraft.item.ItemStack
import li.cil.oc.common.InventorySlots.Tier

object LinkedCard extends Item {
  override def worksWith(stack: ItemStack) = isOneOf(stack, api.Items.get("linkedCard"))

  override def createEnvironment(stack: ItemStack, container: component.Container) = new component.LinkedCard()

  override def slot(stack: ItemStack) = Slot.Card

  override def tier(stack: ItemStack) = Tier.Three
}

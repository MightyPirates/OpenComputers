package li.cil.oc.server.driver.item

import li.cil.oc.api
import li.cil.oc.api.driver.EnvironmentAware
import li.cil.oc.api.driver.EnvironmentHost
import li.cil.oc.common.Slot
import li.cil.oc.common.Tier
import li.cil.oc.server.component
import net.minecraft.item.ItemStack

object InternetCard extends Item with EnvironmentAware {
  override def worksWith(stack: ItemStack) =
    isOneOf(stack, api.Items.get("internetCard"))

  override def createEnvironment(stack: ItemStack, host: EnvironmentHost) = new component.InternetCard()

  override def slot(stack: ItemStack) = Slot.Card

  override def tier(stack: ItemStack) = Tier.Two

  override def providedEnvironment(stack: ItemStack) = classOf[component.InternetCard]
}

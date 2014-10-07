package li.cil.oc.server.driver.item

import li.cil.oc.api
import li.cil.oc.api.driver.EnvironmentAware
import li.cil.oc.api.driver.EnvironmentHost
import li.cil.oc.api.driver.item.HostAware
import li.cil.oc.common.Slot
import li.cil.oc.server.component
import net.minecraft.item.ItemStack

object NetworkCard extends Item with HostAware with EnvironmentAware {
  override def worksWith(stack: ItemStack) =
    isOneOf(stack, api.Items.get("lanCard"))

  override def worksWith(stack: ItemStack, host: Class[_ <: EnvironmentHost]) =
    worksWith(stack) && !isTablet(host)

  override def createEnvironment(stack: ItemStack, host: EnvironmentHost) = new component.NetworkCard()

  override def slot(stack: ItemStack) = Slot.Card

  override def providedEnvironment(stack: ItemStack) = classOf[component.NetworkCard]
}

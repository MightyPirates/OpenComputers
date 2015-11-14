package li.cil.oc.integration.opencomputers

import li.cil.oc.Constants
import li.cil.oc.api
import li.cil.oc.api.driver.EnvironmentAware
import li.cil.oc.api.network.EnvironmentHost
import li.cil.oc.common.Slot
import li.cil.oc.common.Tier
import li.cil.oc.server.component
import net.minecraft.item.ItemStack

object DriverLinkedCard extends Item with EnvironmentAware {
  override def worksWith(stack: ItemStack) = isOneOf(stack,
    api.Items.get(Constants.ItemName.LinkedCard))

  override def createEnvironment(stack: ItemStack, host: EnvironmentHost) =
    if (host.world.isRemote) null
    else new component.LinkedCard()

  override def slot(stack: ItemStack) = Slot.Card

  override def tier(stack: ItemStack) = Tier.Three

  override def providedEnvironment(stack: ItemStack) = classOf[component.LinkedCard]
}

package li.cil.oc.integration.opencomputers

import li.cil.oc.common.Slot
import li.cil.oc.server.component
import li.cil.oc.{Constants, api}
import li.cil.oc.api.driver.{EnvironmentHost, EnvironmentAware}
import net.minecraft.item.ItemStack


object DriverDataCard extends Item with EnvironmentAware{
  override def worksWith(stack: ItemStack) = isOneOf(stack,
    api.Items.get(Constants.ItemName.DataCard))

  override def createEnvironment(stack: ItemStack, host: EnvironmentHost) = new component.DataCard

  override def slot(stack: ItemStack) = Slot.Card

  override def providedEnvironment(stack: ItemStack) = classOf[component.DataCard]
}

package li.cil.oc.server.driver.item

import li.cil.oc.api
import li.cil.oc.api.driver.EnvironmentAware
import li.cil.oc.api.driver.EnvironmentHost
import li.cil.oc.common.Slot
import li.cil.oc.server.component
import net.minecraft.item.ItemStack

object Geolyzer extends Item with EnvironmentAware {
  override def worksWith(stack: ItemStack) =
    isOneOf(stack, api.Items.get("geolyzer"))

  override def createEnvironment(stack: ItemStack, host: EnvironmentHost) = new component.Geolyzer(host)

  override def slot(stack: ItemStack) = Slot.Upgrade

  override def providedEnvironment(stack: ItemStack) = classOf[component.Geolyzer]
}

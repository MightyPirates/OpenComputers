package li.cil.oc.integration.opencomputers

import li.cil.oc.Constants
import li.cil.oc.api
import li.cil.oc.api.driver.EnvironmentAware
import li.cil.oc.api.driver.EnvironmentHost
import li.cil.oc.api.driver.item.HostAware
import li.cil.oc.api.internal.Rotatable
import li.cil.oc.common.Slot
import li.cil.oc.server.component
import net.minecraft.item.ItemStack

object DriverUpgradePiston extends Item with HostAware with EnvironmentAware {
  override def worksWith(stack: ItemStack) = isOneOf(stack,
    api.Items.get(Constants.ItemName.PistonUpgrade))

  override def createEnvironment(stack: ItemStack, host: EnvironmentHost) = host match {
    case rotatable: Rotatable with EnvironmentHost => new component.UpgradePiston(rotatable)
    case _ => null
  }

  override def slot(stack: ItemStack) = Slot.Upgrade

  override def providedEnvironment(stack: ItemStack) = classOf[component.UpgradePiston]
}

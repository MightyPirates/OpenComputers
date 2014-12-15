package li.cil.oc.integration.opencomputers

import li.cil.oc.api
import li.cil.oc.api.driver.EnvironmentHost
import li.cil.oc.api.driver.item.HostAware
import li.cil.oc.common.Slot
import li.cil.oc.server.component
import net.minecraft.item.ItemStack

object DriverUpgradeTank extends Item with HostAware {
  override def worksWith(stack: ItemStack) =
    isOneOf(stack, api.Items.get("tankUpgrade"))

  override def createEnvironment(stack: ItemStack, host: EnvironmentHost) = new component.UpgradeTank(host, 16000)

  override def slot(stack: ItemStack) = Slot.Upgrade
}

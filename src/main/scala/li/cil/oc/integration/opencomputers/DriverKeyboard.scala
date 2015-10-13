package li.cil.oc.integration.opencomputers

import li.cil.oc.Constants
import li.cil.oc.api
import li.cil.oc.api.network.EnvironmentHost
import li.cil.oc.api.driver.item.HostAware
import li.cil.oc.common.Slot
import li.cil.oc.server.component
import net.minecraft.item.ItemStack

object DriverKeyboard extends Item with HostAware {
  override def worksWith(stack: ItemStack) = isOneOf(stack,
    api.Items.get(Constants.BlockName.Keyboard))

  override def createEnvironment(stack: ItemStack, host: EnvironmentHost) = new component.Keyboard(host)

  override def slot(stack: ItemStack) = Slot.Upgrade
}

package li.cil.oc.integration.opencomputers

import li.cil.oc.Constants
import li.cil.oc.api
import li.cil.oc.api.driver.EnvironmentAware
import li.cil.oc.api.driver.item.HostAware
import li.cil.oc.api.internal
import li.cil.oc.api.network.EnvironmentHost
import li.cil.oc.common.Slot
import li.cil.oc.server.component
import net.minecraft.item.ItemStack

object DriverUpgradePiston extends Item with HostAware with EnvironmentAware {
  override def worksWith(stack: ItemStack) = isOneOf(stack,
    api.Items.get(Constants.ItemName.PistonUpgrade))

  override def createEnvironment(stack: ItemStack, host: EnvironmentHost) =
    if (host.world.isRemote) null
    else host match {
      case host: internal.Drone => new component.UpgradePiston.Drone(host)
      case host: internal.Tablet => new component.UpgradePiston.Tablet(host)
      case host: internal.Rotatable with EnvironmentHost => new component.UpgradePiston.Rotatable(host)
      case _ => null
    }

  override def slot(stack: ItemStack) = Slot.Upgrade

  override def providedEnvironment(stack: ItemStack) = classOf[component.UpgradePiston]
}

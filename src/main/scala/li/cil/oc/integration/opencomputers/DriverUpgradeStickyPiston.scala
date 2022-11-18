package li.cil.oc.integration.opencomputers

import li.cil.oc.Constants
import li.cil.oc.api
import li.cil.oc.api.driver.EnvironmentProvider
import li.cil.oc.api.driver.item.HostAware
import li.cil.oc.api.internal
import li.cil.oc.api.network.{EnvironmentHost, ManagedEnvironment}
import li.cil.oc.common.Slot
import li.cil.oc.server.component
import net.minecraft.item.ItemStack

object DriverUpgradeStickyPiston extends Item with HostAware {
  override def worksWith(stack: ItemStack): Boolean = isOneOf(stack,
    api.Items.get(Constants.ItemName.StickyPistonUpgrade))

  override def createEnvironment(stack: ItemStack, host: EnvironmentHost): ManagedEnvironment =
    if (host.world != null && host.world.isClientSide) null
    else host match {
      case host: internal.Drone => new component.UpgradeStickyPiston.Drone(host)
      case host: internal.Tablet => new component.UpgradeStickyPiston.Tablet(host)
      case host: internal.Rotatable with EnvironmentHost => new component.UpgradeStickyPiston.Rotatable(host)
      case _ => null
    }

  override def slot(stack: ItemStack): String = Slot.Upgrade

  object Provider extends EnvironmentProvider {
    override def getEnvironment(stack: ItemStack): Class[_] =
      if (worksWith(stack))
        classOf[component.UpgradeStickyPiston]
      else null
  }

}

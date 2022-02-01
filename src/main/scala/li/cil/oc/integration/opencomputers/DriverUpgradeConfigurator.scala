package li.cil.oc.integration.opencomputers

import li.cil.oc.api.driver.EnvironmentProvider
import li.cil.oc.{Constants, api}
import li.cil.oc.api.driver.item.HostAware
import li.cil.oc.api.internal.Adapter
import li.cil.oc.api.network.{EnvironmentHost, ManagedEnvironment}
import li.cil.oc.common.entity.Drone
import li.cil.oc.common.tileentity.Robot
import li.cil.oc.common.tileentity.Microcontroller
import li.cil.oc.common.{Slot, Tier}
import li.cil.oc.server.component
import net.minecraft.item.ItemStack

object DriverUpgradeConfigurator extends Item with HostAware  {
  override def worksWith(stack: ItemStack): Boolean = isOneOf(stack,
    api.Items.get(Constants.ItemName.ConfiguratorUpgrade))

  override def createEnvironment(stack: ItemStack, host: EnvironmentHost) : ManagedEnvironment =
    if (host.world != null && host.world.isRemote) null
    else host match {
      case host: EnvironmentHost with Adapter => new component.UpgradeConfigurator.Adapter(host)
      case host: EnvironmentHost with Drone => new component.UpgradeConfigurator.Drone(host)
      case host: EnvironmentHost with Robot => new component.UpgradeConfigurator.Robot(host)
      case host: EnvironmentHost with Microcontroller => new component.UpgradeConfigurator.Microcontroller(host)
      case _ => null
    }

  override def slot(stack: ItemStack): String = Slot.Upgrade

  override def tier(stack: ItemStack): Int = Tier.Two

  object Provider extends EnvironmentProvider {
    override def getEnvironment(stack: ItemStack): Class[_] =
      if (worksWith(stack))
        classOf[component.UpgradeConfigurator.Robot]
      else null
  }
}

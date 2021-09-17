package li.cil.oc.integration.forestry

import li.cil.oc.Constants
import li.cil.oc.api
import li.cil.oc.api.driver.EnvironmentProvider
import li.cil.oc.api.driver.item.HostAware
import li.cil.oc.api.internal
import li.cil.oc.api.network.{EnvironmentHost, ManagedEnvironment}
import li.cil.oc.common.{Slot, Tier}
import li.cil.oc.integration.opencomputers.Item
import net.minecraft.item.ItemStack

object DriverUpgradeBeekeeper extends Item with HostAware {
  override def worksWith(stack: ItemStack): Boolean = isOneOf(stack,
    api.Items.get(Constants.ItemName.BeekeeperUpgrade))

  override def createEnvironment(stack: ItemStack, host: EnvironmentHost): ManagedEnvironment =
    if (host.world != null && host.world.isRemote) null
    else host match {
      case host: internal.Agent => new UpgradeBeekeeper(host)
      case _ => null
    }

  override def slot(stack: ItemStack) : String = Slot.Upgrade

  override def tier(stack: ItemStack) : Int = Tier.Two

  object Provider extends EnvironmentProvider {
    override def getEnvironment(stack: ItemStack): Class[_] =
      if (worksWith(stack))
        classOf[UpgradeBeekeeper]
      else null
  }
}

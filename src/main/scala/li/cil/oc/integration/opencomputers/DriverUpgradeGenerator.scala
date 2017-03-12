package li.cil.oc.integration.opencomputers

import li.cil.oc.Constants
import li.cil.oc.api
import li.cil.oc.api.driver.EnvironmentProvider
import li.cil.oc.api.driver.item.HostAware
import li.cil.oc.api.internal
import li.cil.oc.api.network.EnvironmentHost
import li.cil.oc.api.util.Location
import li.cil.oc.common.Slot
import li.cil.oc.common.Tier
import li.cil.oc.server.component
import net.minecraft.item.ItemStack

object DriverUpgradeGenerator extends Item with HostAware {
  override def worksWith(stack: ItemStack) = isOneOf(stack,
    api.Items.get(Constants.ItemName.GeneratorUpgrade))

  override def createEnvironment(stack: ItemStack, host: Location) =
    if (host.getWorld != null && host.getWorld.isRemote) null
    else host match {
      case host: internal.Agent => new component.UpgradeGenerator(host)
      case _ => null
    }

  override def slot(stack: ItemStack) = Slot.Upgrade

  override def tier(stack: ItemStack) = Tier.Two

  object Provider extends EnvironmentProvider {
    override def getEnvironment(stack: ItemStack): Class[_] =
      if (worksWith(stack))
        classOf[component.UpgradeGenerator]
      else null
  }

}

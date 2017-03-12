package li.cil.oc.integration.opencomputers

import li.cil.oc.Constants
import li.cil.oc.api
import li.cil.oc.api.driver.EnvironmentProvider
import li.cil.oc.api.driver.item.HostAware
import li.cil.oc.api.internal.Adapter
import li.cil.oc.api.network.{Environment, EnvironmentHost}
import li.cil.oc.api.tileentity.Rotatable
import li.cil.oc.api.util.Location
import li.cil.oc.common.Slot
import li.cil.oc.server.component
import li.cil.oc.server.component.UpgradeSignInAdapter
import li.cil.oc.server.component.UpgradeSignInRotatable
import net.minecraft.item.ItemStack

object DriverUpgradeSign extends Item with HostAware {
  override def worksWith(stack: ItemStack) = isOneOf(stack,
    api.Items.get(Constants.ItemName.SignUpgrade))

  override def createEnvironment(stack: ItemStack, host: Location) =
    if (host.getWorld != null && host.getWorld.isRemote) null
    else host match {
      case rotatable: Location with Rotatable => new UpgradeSignInRotatable(rotatable)
      case adapter: Location with Adapter => new UpgradeSignInAdapter(adapter)
      case _ => null
    }

  override def slot(stack: ItemStack) = Slot.Upgrade

  object Provider extends EnvironmentProvider {
    override def getEnvironment(stack: ItemStack): Class[_] =
      if (worksWith(stack))
        classOf[component.UpgradeSign]
      else null
  }

}

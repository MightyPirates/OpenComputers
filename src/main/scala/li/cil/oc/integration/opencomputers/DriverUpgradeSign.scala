package li.cil.oc.integration.opencomputers

import li.cil.oc.api
import li.cil.oc.api.driver.{EnvironmentAware, EnvironmentHost}
import li.cil.oc.api.driver.item.HostAware
import li.cil.oc.api.internal.{Adapter, Rotatable}
import li.cil.oc.common.Slot
import li.cil.oc.server.component
import li.cil.oc.server.component.{UpgradeSignInAdapter, UpgradeSignInRotatable}
import net.minecraft.item.ItemStack

object DriverUpgradeSign extends Item with HostAware with EnvironmentAware {
  override def worksWith(stack: ItemStack) =
    isOneOf(stack, api.Items.get("signUpgrade"))

  override def worksWith(stack: ItemStack, host: Class[_ <: EnvironmentHost]) =
    worksWith(stack) && (isRotatable(host) || isAdapter(host))

  override def createEnvironment(stack: ItemStack, host: EnvironmentHost) =
    host match {
      case rotatable: EnvironmentHost with Rotatable => new UpgradeSignInRotatable(rotatable)
      case adapter: EnvironmentHost with Adapter => new UpgradeSignInAdapter(adapter)
      case _ => null
    }

  override def slot(stack: ItemStack) = Slot.Upgrade

  override def providedEnvironment(stack: ItemStack) = classOf[component.UpgradeSign]
}

package li.cil.oc.integration.opencomputers

import li.cil.oc.Constants
import li.cil.oc.api.driver.EnvironmentProvider
import li.cil.oc.api
import li.cil.oc.api.driver.item.{HostAware, Slot}
import li.cil.oc.api.network.{EnvironmentHost, ManagedEnvironment}
import li.cil.oc.server.component
import li.cil.oc.server.component.UpgradeBarcodeReader
import net.minecraft.item.ItemStack

object DriverUpgradeBarcodeReader extends Item with HostAware {
  override def worksWith(stack: ItemStack) = isOneOf(stack,
    api.Items.get(Constants.ItemName.Analyzer))

  override def createEnvironment(stack: ItemStack, host: EnvironmentHost): ManagedEnvironment =
    new UpgradeBarcodeReader(host)

  override def slot(stack: ItemStack) = Slot.Upgrade

  object Provider extends EnvironmentProvider {
    override def getEnvironment(stack: ItemStack): Class[_] =
      if (worksWith(stack))
        classOf[component.UpgradeBarcodeReader]
      else null
  }
}

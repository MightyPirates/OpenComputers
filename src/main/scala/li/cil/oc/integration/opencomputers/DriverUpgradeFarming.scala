package li.cil.oc.integration.opencomputers

import li.cil.oc.api.driver.item.HostAware
import li.cil.oc.api.driver.{EnvironmentAware, EnvironmentHost}
import li.cil.oc.api.internal.Robot
import li.cil.oc.common.{Slot, Tier}
import li.cil.oc.server.component
import li.cil.oc.{Constants, api}
import net.minecraft.item.ItemStack

object DriverUpgradeFarming extends Item with HostAware with EnvironmentAware {
  override def worksWith(stack: ItemStack) = isOneOf(stack,
    api.Items.get(Constants.ItemName.FarmingUpgrade))

  override def createEnvironment(stack: ItemStack, host: EnvironmentHost) =
    host match {
      case robot: EnvironmentHost with Robot => new component.UpgradeFarming(robot)
      case _ => null
    }

  override def slot(stack: ItemStack) = Slot.Upgrade

  override def tier(stack: ItemStack) = Tier.One

  override def providedEnvironment(stack: ItemStack) = classOf[component.UpgradeFarming]
}

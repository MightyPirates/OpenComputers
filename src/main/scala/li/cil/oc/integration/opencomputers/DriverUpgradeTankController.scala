package li.cil.oc.integration.opencomputers

import li.cil.oc.api
import li.cil.oc.api.driver.EnvironmentAware
import li.cil.oc.api.driver.EnvironmentHost
import li.cil.oc.api.driver.item.HostAware
import li.cil.oc.api.internal.Adapter
import li.cil.oc.api.internal.Robot
import li.cil.oc.common.Slot
import li.cil.oc.common.Tier
import li.cil.oc.server.component
import net.minecraft.item.ItemStack

object DriverUpgradeTankController extends Item with HostAware with EnvironmentAware {
  override def worksWith(stack: ItemStack) =
    isOneOf(stack, api.Items.get("tankControllerUpgrade"))

  override def createEnvironment(stack: ItemStack, host: EnvironmentHost) = host match {
    case robot: EnvironmentHost with Robot => new component.UpgradeTankControllerInRobot(robot)
    case adapter: EnvironmentHost with Adapter => new component.UpgradeTankControllerInAdapter(adapter)
    case _ => null
  }

  override def slot(stack: ItemStack) = Slot.Upgrade

  override def tier(stack: ItemStack) = Tier.Two

  override def providedEnvironment(stack: ItemStack) = classOf[component.UpgradeTankControllerInRobot]
}

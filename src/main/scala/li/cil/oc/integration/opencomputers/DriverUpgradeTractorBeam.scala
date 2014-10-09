package li.cil.oc.integration.opencomputers

import li.cil.oc.api
import li.cil.oc.api.driver.EnvironmentAware
import li.cil.oc.api.driver.EnvironmentHost
import li.cil.oc.api.driver.item.HostAware
import li.cil.oc.api.internal.Robot
import li.cil.oc.common.Slot
import li.cil.oc.common.Tier
import li.cil.oc.common.item.TabletWrapper
import li.cil.oc.server.component
import net.minecraft.item.ItemStack

object DriverUpgradeTractorBeam extends Item with HostAware with EnvironmentAware {
  override def worksWith(stack: ItemStack) =
    isOneOf(stack, api.Items.get("tractorBeamUpgrade"))

  override def worksWith(stack: ItemStack, host: Class[_ <: EnvironmentHost]) =
    worksWith(stack) && (isRobot(host) || isTablet(host))

  override def createEnvironment(stack: ItemStack, host: EnvironmentHost) = host match {
    case robot: Robot => new component.UpgradeTractorBeam(host, robot.player)
    case tablet: TabletWrapper => new component.UpgradeTractorBeam(host, () => tablet.player)
    case _ => null
  }

  override def slot(stack: ItemStack) = Slot.Upgrade

  override def tier(stack: ItemStack) = Tier.Three

  override def providedEnvironment(stack: ItemStack) = classOf[component.UpgradeTractorBeam]
}

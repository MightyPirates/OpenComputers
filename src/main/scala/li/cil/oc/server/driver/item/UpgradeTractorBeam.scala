package li.cil.oc.server.driver.item

import li.cil.oc.api
import li.cil.oc.api.driver.EnvironmentHost
import li.cil.oc.api.driver.item.HostAware
import li.cil.oc.api.tileentity.Robot
import li.cil.oc.common.Slot
import li.cil.oc.common.Tier
import li.cil.oc.common.item.TabletWrapper
import li.cil.oc.server.component
import net.minecraft.item.ItemStack

object UpgradeTractorBeam extends Item with HostAware {
  override def worksWith(stack: ItemStack) =
    isOneOf(stack, api.Items.get("tractorBeamUpgrade"))

  override def worksWith(stack: ItemStack, host: Class[_ <: EnvironmentHost]) =
    worksWith(stack) && (isRobot(host) || isTablet(host))

  override def createEnvironment(stack: ItemStack, host: EnvironmentHost) = host match {
    case robot: Robot => new component.UpgradeTractorBeam(host, robot.player)
    case tablet: TabletWrapper => new component.UpgradeTractorBeam(host, () => tablet.holder)
    case _ => null
  }

  override def slot(stack: ItemStack) = Slot.Upgrade

  override def tier(stack: ItemStack) = Tier.Three
}

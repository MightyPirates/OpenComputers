package li.cil.oc.integration.opencomputers

import li.cil.oc.Constants
import li.cil.oc.api
import li.cil.oc.api.driver.EnvironmentAware
import li.cil.oc.api.driver.EnvironmentHost
import li.cil.oc.api.driver.item.HostAware
import li.cil.oc.api.internal.Robot
import li.cil.oc.common.Slot
import li.cil.oc.common.Tier
import li.cil.oc.common.entity.Drone
import li.cil.oc.common.item.TabletWrapper
import li.cil.oc.server.component
import li.cil.oc.server.component.UpgradeTractorBeam
import net.minecraft.item.ItemStack

object DriverUpgradeTractorBeam extends Item with HostAware with EnvironmentAware {
  override def worksWith(stack: ItemStack) = isOneOf(stack,
    api.Items.get(Constants.ItemName.TractorBeamUpgrade))

  override def createEnvironment(stack: ItemStack, host: EnvironmentHost) = host match {
    case drone: Drone => new UpgradeTractorBeam.Drone(drone)
    case robot: Robot => new component.UpgradeTractorBeam.Player(host, robot.player)
    case tablet: TabletWrapper => new component.UpgradeTractorBeam.Player(host, () => tablet.player)
    case _ => null
  }

  override def slot(stack: ItemStack) = Slot.Upgrade

  override def tier(stack: ItemStack) = Tier.Three

  override def providedEnvironment(stack: ItemStack) = classOf[component.UpgradeTractorBeam]
}

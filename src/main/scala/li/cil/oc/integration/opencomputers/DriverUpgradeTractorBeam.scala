package li.cil.oc.integration.opencomputers

import li.cil.oc.Constants
import li.cil.oc.api
import li.cil.oc.api.driver.EnvironmentProvider
import li.cil.oc.api.driver.item.HostAware
import li.cil.oc.api.internal.Robot
import li.cil.oc.api.network.EnvironmentHost
import li.cil.oc.common.Slot
import li.cil.oc.common.Tier
import li.cil.oc.common.entity.Drone
import li.cil.oc.common.item.TabletWrapper
import li.cil.oc.server.component
import li.cil.oc.server.component.UpgradeTractorBeam
import net.minecraft.item.ItemStack

object DriverUpgradeTractorBeam extends Item with HostAware {
  override def worksWith(stack: ItemStack) = isOneOf(stack,
    api.Items.get(Constants.ItemName.TractorBeamUpgrade))

  override def createEnvironment(stack: ItemStack, host: EnvironmentHost) =
    if (host.world.isRemote) null
    else host match {
      case drone: Drone => new UpgradeTractorBeam.Drone(drone)
      case robot: Robot => new component.UpgradeTractorBeam.Player(host, robot.player)
      case tablet: TabletWrapper => new component.UpgradeTractorBeam.Player(host, () => tablet.player)
      case _ => null
    }

  override def slot(stack: ItemStack) = Slot.Upgrade

  override def tier(stack: ItemStack) = Tier.Three

  object Provider extends EnvironmentProvider {
    override def getEnvironment(stack: ItemStack): Class[_] =
      if (worksWith(stack))
        classOf[component.UpgradeTractorBeam]
      else null
  }

}

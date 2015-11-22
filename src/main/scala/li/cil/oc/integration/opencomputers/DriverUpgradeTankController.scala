package li.cil.oc.integration.opencomputers

import li.cil.oc.Constants
import li.cil.oc.api
import li.cil.oc.api.driver.EnvironmentProvider
import li.cil.oc.api.driver.item.HostAware
import li.cil.oc.api.internal.Adapter
import li.cil.oc.api.network.EnvironmentHost
import li.cil.oc.common.Slot
import li.cil.oc.common.Tier
import li.cil.oc.common.entity.Drone
import li.cil.oc.common.tileentity.Robot
import li.cil.oc.server.component
import net.minecraft.item.ItemStack

object DriverUpgradeTankController extends Item with HostAware {
  override def worksWith(stack: ItemStack) = isOneOf(stack,
    api.Items.get(Constants.ItemName.TankControllerUpgrade))

  override def createEnvironment(stack: ItemStack, host: EnvironmentHost) =
    if (host.world.isRemote) null
    else host match {
      case host: EnvironmentHost with Adapter => new component.UpgradeTankController.Adapter(host)
      case host: EnvironmentHost with Drone => new component.UpgradeTankController.Drone(host)
      case host: EnvironmentHost with Robot => new component.UpgradeTankController.Robot(host)
      case _ => null
    }

  override def slot(stack: ItemStack) = Slot.Upgrade

  override def tier(stack: ItemStack) = Tier.Two

  object Provider extends EnvironmentProvider {
    override def getEnvironment(stack: ItemStack): Class[_] =
      if (worksWith(stack))
        classOf[component.UpgradeTankController.Robot]
      else null
  }

}

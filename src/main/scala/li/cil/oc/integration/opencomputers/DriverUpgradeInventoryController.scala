package li.cil.oc.integration.opencomputers

import li.cil.oc.Constants
import li.cil.oc.api
import li.cil.oc.api.driver.EnvironmentAware
import li.cil.oc.api.driver.item.HostAware
import li.cil.oc.api.internal.Adapter
import li.cil.oc.api.network.EnvironmentHost
import li.cil.oc.common.Slot
import li.cil.oc.common.Tier
import li.cil.oc.common.entity.Drone
import li.cil.oc.common.tileentity.Robot
import li.cil.oc.server.component
import net.minecraft.item.ItemStack

object DriverUpgradeInventoryController extends Item with HostAware with EnvironmentAware {
  override def worksWith(stack: ItemStack) = isOneOf(stack,
    api.Items.get(Constants.ItemName.InventoryControllerUpgrade))

  override def createEnvironment(stack: ItemStack, host: EnvironmentHost) =
    if (host.world.isRemote) null
    else host match {
      case host: EnvironmentHost with Adapter => new component.UpgradeInventoryController.Adapter(host)
      case host: EnvironmentHost with Drone => new component.UpgradeInventoryController.Drone(host)
      case host: EnvironmentHost with Robot => new component.UpgradeInventoryController.Robot(host)
      case _ => null
    }

  override def slot(stack: ItemStack) = Slot.Upgrade

  override def tier(stack: ItemStack) = Tier.Two

  override def providedEnvironment(stack: ItemStack) = classOf[component.UpgradeInventoryController.Robot]
}

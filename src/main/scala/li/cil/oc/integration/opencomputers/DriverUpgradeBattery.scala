package li.cil.oc.integration.opencomputers

import li.cil.oc.Constants
import li.cil.oc.api
import li.cil.oc.api.driver.item.HostAware
import li.cil.oc.api.network.Environment
import li.cil.oc.api.util.Location
import li.cil.oc.common.Slot
import li.cil.oc.common.Tier
import li.cil.oc.common.item
import li.cil.oc.common.item.Delegator
import li.cil.oc.server.component
import net.minecraft.item.ItemStack

object DriverUpgradeBattery extends Item with HostAware {
  override def worksWith(stack: ItemStack) = isOneOf(stack,
    api.Items.get(Constants.ItemName.BatteryUpgradeTier1),
    api.Items.get(Constants.ItemName.BatteryUpgradeTier2),
    api.Items.get(Constants.ItemName.BatteryUpgradeTier3))

  override def createEnvironment(stack: ItemStack, host: Location) =
    if (host.getWorld != null && host.getWorld.isRemote) null
    else new component.UpgradeBattery(tier(stack))

  override def slot(stack: ItemStack) = Slot.Upgrade

  override def tier(stack: ItemStack) =
    Delegator.subItem(stack) match {
      case Some(battery: item.UpgradeBattery) => battery.tier
      case _ => Tier.One
    }
}

package li.cil.oc.integration.opencomputers

import li.cil.oc.api
import li.cil.oc.api.driver.EnvironmentHost
import li.cil.oc.api.driver.item.HostAware
import li.cil.oc.common.init.Items
import li.cil.oc.common.{Slot, Tier, item}
import li.cil.oc.server.component
import net.minecraft.item.ItemStack

object DriverUpgradeBattery extends Item with HostAware {
  override def worksWith(stack: ItemStack) =
    isOneOf(stack, api.Items.get("batteryUpgrade1"), api.Items.get("batteryUpgrade2"), api.Items.get("batteryUpgrade3"))

  override def worksWith(stack: ItemStack, host: Class[_ <: EnvironmentHost]) =
    worksWith(stack) && !isAdapter(host)

  override def createEnvironment(stack: ItemStack, host: EnvironmentHost) = new component.UpgradeBattery(tier(stack))

  override def slot(stack: ItemStack) = Slot.Upgrade

  override def tier(stack: ItemStack) =
    Items.multi.subItem(stack) match {
      case Some(battery: item.UpgradeBattery) => battery.tier
      case _ => Tier.One
    }
}

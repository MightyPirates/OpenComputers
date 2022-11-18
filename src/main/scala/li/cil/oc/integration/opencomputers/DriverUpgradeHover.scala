package li.cil.oc.integration.opencomputers

import li.cil.oc.Constants
import li.cil.oc.api
import li.cil.oc.api.network.EnvironmentHost
import li.cil.oc.api.driver.item.HostAware
import li.cil.oc.common.Slot
import li.cil.oc.common.Tier
import li.cil.oc.common.item
import net.minecraft.item.ItemStack

object DriverUpgradeHover extends Item with HostAware {
  override def worksWith(stack: ItemStack) = isOneOf(stack,
    api.Items.get(Constants.ItemName.HoverUpgradeTier1),
    api.Items.get(Constants.ItemName.HoverUpgradeTier2))

  override def createEnvironment(stack: ItemStack, host: EnvironmentHost) = null

  override def slot(stack: ItemStack) = Slot.Upgrade

  override def tier(stack: ItemStack) =
    stack.getItem match {
      case upgrade: item.UpgradeHover => upgrade.tier
      case _ => Tier.One
    }
}

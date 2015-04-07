package li.cil.oc.integration.opencomputers

import li.cil.oc.Constants
import li.cil.oc.api
import li.cil.oc.api.driver.EnvironmentHost
import li.cil.oc.api.driver.item.Container
import li.cil.oc.common.Slot
import li.cil.oc.common.Tier
import li.cil.oc.common.item
import li.cil.oc.common.item.Delegator
import net.minecraft.item.ItemStack

object DriverContainerUpgrade extends Item with Container {
  override def worksWith(stack: ItemStack) = isOneOf(stack,
    api.Items.get(Constants.ItemName.UpgradeContainerTier1),
    api.Items.get(Constants.ItemName.UpgradeContainerTier2),
    api.Items.get(Constants.ItemName.UpgradeContainerTier3))

  override def createEnvironment(stack: ItemStack, host: EnvironmentHost) = null

  override def slot(stack: ItemStack) = Slot.Container

  override def providedSlot(stack: ItemStack) = Slot.Upgrade

  override def providedTier(stack: ItemStack) = tier(stack)

  override def tier(stack: ItemStack) =
    Delegator.subItem(stack) match {
      case Some(container: item.UpgradeContainerUpgrade) => container.tier
      case _ => Tier.One
    }
}

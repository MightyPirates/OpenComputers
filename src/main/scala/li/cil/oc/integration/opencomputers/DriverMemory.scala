package li.cil.oc.integration.opencomputers

import li.cil.oc.Constants
import li.cil.oc.Settings
import li.cil.oc.api
import li.cil.oc.common.Slot
import li.cil.oc.common.Tier
import li.cil.oc.common.item
import li.cil.oc.common.item.Delegator
import net.minecraft.item.ItemStack

object DriverMemory extends Item with api.driver.item.Memory with api.driver.item.CallBudget {
  override def amount(stack: ItemStack) = Delegator.subItem(stack) match {
    case Some(memory: item.Memory) =>
      val sizes = Settings.get.ramSizes
      Settings.get.ramSizes(memory.tier max 0 min (sizes.length - 1))
    case _ => 0.0
  }

  override def worksWith(stack: ItemStack) = isOneOf(stack,
    api.Items.get(Constants.ItemName.RAMTier1),
    api.Items.get(Constants.ItemName.RAMTier2),
    api.Items.get(Constants.ItemName.RAMTier3),
    api.Items.get(Constants.ItemName.RAMTier4),
    api.Items.get(Constants.ItemName.RAMTier5),
    api.Items.get(Constants.ItemName.RAMTier6))

  override def createEnvironment(stack: ItemStack, host: api.network.EnvironmentHost) = null

  override def slot(stack: ItemStack) = Slot.Memory

  override def tier(stack: ItemStack) =
    Delegator.subItem(stack) match {
      case Some(memory: item.Memory) => memory.tier / 2
      case _ => Tier.One
    }

  override def getCallBudget(stack: ItemStack): Double = Settings.get.callBudgets(tier(stack) max Tier.One min Tier.Three)
}

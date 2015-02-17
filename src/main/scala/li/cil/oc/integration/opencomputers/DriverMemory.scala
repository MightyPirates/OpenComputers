package li.cil.oc.integration.opencomputers

import li.cil.oc.Constants
import li.cil.oc.api
import li.cil.oc.api.driver
import li.cil.oc.common.Slot
import li.cil.oc.common.Tier
import li.cil.oc.common.item
import li.cil.oc.common.item.Delegator
import net.minecraft.item.ItemStack

object DriverMemory extends Item with driver.item.Memory {
  override def amount(stack: ItemStack) = Delegator.subItem(stack) match {
    case Some(memory: item.Memory) => memory.tier + 1
    case _ => 0.0
  }

  override def worksWith(stack: ItemStack) = isOneOf(stack,
    api.Items.get(Constants.ItemName.RAMTier1),
    api.Items.get(Constants.ItemName.RAMTier2),
    api.Items.get(Constants.ItemName.RAMTier3),
    api.Items.get(Constants.ItemName.RAMTier4),
    api.Items.get(Constants.ItemName.RAMTier5),
    api.Items.get(Constants.ItemName.RAMTier6))

  override def createEnvironment(stack: ItemStack, host: driver.EnvironmentHost) = null

  override def slot(stack: ItemStack) = Slot.Memory

  override def tier(stack: ItemStack) = Delegator.subItem(stack) match {
    case Some(memory: item.Memory) => memory.tier / 2
    case _ => Tier.One
  }
}

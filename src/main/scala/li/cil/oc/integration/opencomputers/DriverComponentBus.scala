package li.cil.oc.integration.opencomputers

import li.cil.oc.Constants
import li.cil.oc.Settings
import li.cil.oc.api
import li.cil.oc.api.network.EnvironmentHost
import li.cil.oc.api.driver.item.Processor
import li.cil.oc.api.util.Location
import li.cil.oc.common.Slot
import li.cil.oc.common.Tier
import li.cil.oc.common.item
import li.cil.oc.common.item.Delegator
import net.minecraft.item.ItemStack

object DriverComponentBus extends Item with Processor {
  override def worksWith(stack: ItemStack) = isOneOf(stack,
    api.Items.get(Constants.ItemName.ComponentBusTier1),
    api.Items.get(Constants.ItemName.ComponentBusTier2),
    api.Items.get(Constants.ItemName.ComponentBusTier3))

  override def createEnvironment(stack: ItemStack, host: Location) = null

  override def slot(stack: ItemStack) = Slot.ComponentBus

  override def tier(stack: ItemStack) =
    Delegator.subItem(stack) match {
      case Some(bus: item.ComponentBus) => bus.tier
      case _ => Tier.One
    }

  override def supportedComponents(stack: ItemStack) =
    Delegator.subItem(stack) match {
      case Some(bus: item.ComponentBus) => Settings.Computer.cpuComponentCount(bus.tier)
      case _ => Tier.One
    }

  override def architecture(stack: ItemStack) = null
}

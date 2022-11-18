package li.cil.oc.integration.opencomputers

import li.cil.oc.Constants
import li.cil.oc.Settings
import li.cil.oc.api
import li.cil.oc.api.network.EnvironmentHost
import li.cil.oc.api.driver.item.Processor
import li.cil.oc.common.Slot
import li.cil.oc.common.Tier
import li.cil.oc.common.item
import net.minecraft.item.ItemStack

object DriverComponentBus extends Item with Processor {
  override def worksWith(stack: ItemStack) = isOneOf(stack,
    api.Items.get(Constants.ItemName.ComponentBusTier1),
    api.Items.get(Constants.ItemName.ComponentBusTier2),
    api.Items.get(Constants.ItemName.ComponentBusTier3),
    api.Items.get(Constants.ItemName.ComponentBusCreative))

  override def createEnvironment(stack: ItemStack, host: EnvironmentHost) = null

  override def slot(stack: ItemStack) = Slot.ComponentBus

  // Clamp item tier because the creative bus needs to fit into tier 3 slots.
  override def tier(stack: ItemStack) =
    stack.getItem match {
      case bus: item.ComponentBus => bus.tier min Tier.Three
      case _ => Tier.One
    }

  override def supportedComponents(stack: ItemStack) =
    stack.getItem match {
      case bus: item.ComponentBus => Settings.get.cpuComponentSupport(bus.tier)
      case _ => Tier.One
    }

  override def architecture(stack: ItemStack) = null
}

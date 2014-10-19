package li.cil.oc.integration.opencomputers

import li.cil.oc.Settings
import li.cil.oc.api
import li.cil.oc.api.driver.EnvironmentHost
import li.cil.oc.api.driver.item.Processor
import li.cil.oc.common.Slot
import li.cil.oc.common.init.Items
import li.cil.oc.common.item
import net.minecraft.item.ItemStack

object DriverComponentBus extends Item with Processor {
  override def worksWith(stack: ItemStack) = isOneOf(stack, api.Items.get("componentBus1"), api.Items.get("componentBus2"), api.Items.get("componentBus3"))

  override def createEnvironment(stack: ItemStack, host: EnvironmentHost) = null

  override def slot(stack: ItemStack) = Slot.ComponentBus

  override def tier(stack: ItemStack) =
    Items.multi.subItem(stack) match {
      case Some(bus: item.ComponentBus) => bus.tier
      case _ => 0
    }

  override def supportedComponents(stack: ItemStack) =
    Items.multi.subItem(stack) match {
      case Some(bus: item.ComponentBus) => Settings.get.cpuComponentSupport(bus.tier)
      case _ => 0
    }

  override def architecture(stack: ItemStack) = null
}

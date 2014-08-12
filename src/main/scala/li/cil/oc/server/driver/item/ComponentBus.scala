package li.cil.oc.server.driver.item

import li.cil.oc.api.driver
import li.cil.oc.api.driver.{Container, Slot}
import li.cil.oc.common.item
import li.cil.oc.{Items, Settings, api}
import net.minecraft.item.ItemStack

object ComponentBus extends Item with driver.Processor {
  override def worksWith(stack: ItemStack) = isOneOf(stack, api.Items.get("componentBus1"), api.Items.get("componentBus2"), api.Items.get("componentBus3"))

  override def createEnvironment(stack: ItemStack, container: Container) = null

  override def slot(stack: ItemStack) = Slot.None

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
}

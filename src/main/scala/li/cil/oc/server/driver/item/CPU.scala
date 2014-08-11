package li.cil.oc.server.driver.item

import li.cil.oc.api.driver
import li.cil.oc.api.driver.{Container, Slot}
import li.cil.oc.common.item
import li.cil.oc.{Items, Settings, api}
import net.minecraft.item.ItemStack

object CPU extends Item with driver.Processor {
  override def worksWith(stack: ItemStack) = isOneOf(stack, api.Items.get("cpu1"), api.Items.get("cpu2"), api.Items.get("cpu3"))

  override def createEnvironment(stack: ItemStack, container: Container) = null

  override def slot(stack: ItemStack) = Slot.Processor

  override def tier(stack: ItemStack) =
    Items.multi.subItem(stack) match {
      case Some(cpu: item.CPU) => cpu.tier
      case _ => 0
    }

  override def supportedComponents(stack: ItemStack) =
    Items.multi.subItem(stack) match {
      case Some(cpu: item.CPU) => Settings.get.cpuComponentSupport(cpu.tier)
      case _ => 0
    }
}

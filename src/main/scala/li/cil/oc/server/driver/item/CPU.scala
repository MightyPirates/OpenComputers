package li.cil.oc.server.driver.item

import li.cil.oc.api.driver.EnvironmentHost
import li.cil.oc.api.Machine
import li.cil.oc.api.driver
import li.cil.oc.common.Slot
import li.cil.oc.common.item
import li.cil.oc.Items
import li.cil.oc.Settings
import li.cil.oc.api
import net.minecraft.item.ItemStack

object CPU extends Item with driver.Processor {
  override def worksWith(stack: ItemStack) =
    isOneOf(stack, api.Items.get("cpu1"), api.Items.get("cpu2"), api.Items.get("cpu3"))

  override def createEnvironment(stack: ItemStack, host: EnvironmentHost) = null

  override def slot(stack: ItemStack) = Slot.CPU

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

  override def architecture(stack: ItemStack) = Machine.LuaArchitecture
}

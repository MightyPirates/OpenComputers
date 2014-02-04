package li.cil.oc.server.driver.item

import li.cil.oc.api.driver
import li.cil.oc.api.driver.Slot
import li.cil.oc.common.item
import li.cil.oc.{Settings, Items}
import net.minecraft.item.ItemStack
import net.minecraft.tileentity.TileEntity

object Processor extends Item with driver.Processor {
  def worksWith(stack: ItemStack) = isOneOf(stack, Items.cpu0, Items.cpu1, Items.cpu2)

  def createEnvironment(stack: ItemStack, container: TileEntity) = null

  def slot(stack: ItemStack) = Slot.Processor

  override def tier(stack: ItemStack) =
    Items.multi.subItem(stack) match {
      case Some(cpu: item.CPU) => cpu.tier
      case _ => 0
    }

  def supportedComponents(stack: ItemStack) =
    Items.multi.subItem(stack) match {
      case Some(cpu: item.CPU) => Settings.get.cpuComponentSupport(cpu.tier)
      case _ => 0
    }
}

package li.cil.oc.server.driver.item

import li.cil.oc.Items
import li.cil.oc.api.driver
import li.cil.oc.api.driver.Slot
import net.minecraft.item.ItemStack

object Memory extends Item with driver.Memory {
  def amount(stack: ItemStack) = if (stack.getItem == Items.multi) Items.multi.subItem(stack) match {
    case Some(memory: li.cil.oc.common.item.Memory) => memory.kiloBytes * 1024
    case _ => 0
  } else 0

  def worksWith(stack: ItemStack) = isOneOf(stack, Items.ram3, Items.ram1, Items.ram2)

  def createEnvironment(stack: ItemStack, container: AnyRef) = null

  def slot(stack: ItemStack) = Slot.Memory
}

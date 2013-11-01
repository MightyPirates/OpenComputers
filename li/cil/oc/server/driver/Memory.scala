package li.cil.oc.server.driver

import li.cil.oc.Items
import li.cil.oc.api.driver
import li.cil.oc.api.driver.Slot
import net.minecraft.item.ItemStack

object Memory extends Item with driver.Memory {
  def amount(item: ItemStack) = if (item.itemID == Items.multi.itemID) Items.multi.subItem(item) match {
    case Some(memory: li.cil.oc.common.item.Memory) => memory.kiloBytes * 1024
    case _ => 0
  } else 0

  def worksWith(item: ItemStack) = isOneOf(item, Items.ram3, Items.ram1, Items.ram2)

  def createEnvironment(item: ItemStack) = null

  def slot(item: ItemStack) = Slot.Memory
}

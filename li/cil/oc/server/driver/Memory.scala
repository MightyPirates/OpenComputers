package li.cil.oc.server.driver

import li.cil.oc.Items
import li.cil.oc.api.driver
import li.cil.oc.api.driver.Slot
import net.minecraft.item.ItemStack

object Memory extends driver.Memory {
  override def amount(item: ItemStack) = if (item.itemID == Items.multi.itemID) Items.multi.subItem(item) match {
    case Some(memory: li.cil.oc.common.item.Memory) => memory.kiloBytes * 1024
    case _ => 0
  } else 0

  override def worksWith(item: ItemStack) = WorksWith(Items.ram3, Items.ram1, Items.ram2)(item)

  override def slot(item: ItemStack) = Slot.Memory
}

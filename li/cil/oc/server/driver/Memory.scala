package li.cil.oc.server.driver

import li.cil.oc.Items
import li.cil.oc.api.driver
import li.cil.oc.api.driver.Slot
import net.minecraft.item.ItemStack

object Memory extends driver.Item {
  override def worksWith(item: ItemStack) = WorksWith(Items.ram128k, Items.ram32k, Items.ram64k)(item)

  override def slot(item: ItemStack) = Slot.RAM
}

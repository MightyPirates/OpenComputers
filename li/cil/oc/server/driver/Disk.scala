package li.cil.oc.server.driver

import li.cil.oc.Items
import li.cil.oc.api.driver
import li.cil.oc.api.driver.Slot
import net.minecraft.item.ItemStack

object Disk extends driver.Item {
  override def api = null

  override def worksWith(item: ItemStack) = WorksWith(Items.hdd)(item)

  override def slot(item: ItemStack) = Slot.HDD

  override def node(item: ItemStack) = null
}
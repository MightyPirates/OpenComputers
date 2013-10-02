package li.cil.oc.server.driver

import li.cil.oc.{Config, Items}
import li.cil.oc.api.driver
import li.cil.oc.api.driver.Slot
import net.minecraft.item.ItemStack

object FileSystem extends driver.Item {
  override def api = Option(getClass.getResourceAsStream(Config.driverPath + "fs.lua"))

  override def worksWith(item: ItemStack) = WorksWith(Items.hdd)(item)

  override def slot(item: ItemStack) = Slot.HDD

  override def node(item: ItemStack) = None
}
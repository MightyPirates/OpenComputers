package li.cil.oc.server.driver

import li.cil.oc.api.driver.Slot
import li.cil.oc.api.{Persistable, driver}
import li.cil.oc.server.component
import li.cil.oc.{Config, Items}
import net.minecraft.item.ItemStack

object GraphicsCard extends driver.Item {
  override def api = Option(getClass.getResourceAsStream(Config.driverPath + "gpu.lua"))

  override def worksWith(item: ItemStack) = WorksWith(Items.gpu)(item)

  override def slot(item: ItemStack) = Slot.PCI

  override def node(item: ItemStack) = Persistable.get[component.GraphicsCard](item)
}
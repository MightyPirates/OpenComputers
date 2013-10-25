package li.cil.oc.server.driver

import li.cil.oc.api.driver.Slot
import li.cil.oc.server.component
import li.cil.oc.{Config, Items}
import net.minecraft.item.ItemStack

object NetworkCard extends Item {
  override def api = getClass.getResourceAsStream(Config.driverPath + "network.lua")

  def worksWith(item: ItemStack) = WorksWith(Items.lan)(item)

  def slot(item: ItemStack) = Slot.Card

  override def node(item: ItemStack) = new component.NetworkCard()
}

package li.cil.oc.server.driver

import li.cil.oc.api.driver.Slot
import li.cil.oc.server.component
import li.cil.oc.{Config, Items}
import net.minecraft.item.ItemStack

object RedstoneCard extends Item {
  override def api = getClass.getResourceAsStream(Config.driverPath + "redstone.lua")

  override def worksWith(item: ItemStack) = WorksWith(Items.rs)(item)

  override def slot(item: ItemStack) = Slot.Card

  override def node(item: ItemStack) = new component.RedstoneCard()
}

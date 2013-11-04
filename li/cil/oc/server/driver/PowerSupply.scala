package li.cil.oc.server.driver

import li.cil.oc.Items
import li.cil.oc.api.driver.Slot
import li.cil.oc.server.component
import net.minecraft.item.ItemStack

object PowerSupply extends Item {
  override def worksWith(item: ItemStack) = isOneOf(item, Items.psu)

  override def createEnvironment(item: ItemStack) = new component.PowerSupply()

  override def slot(item: ItemStack) = Slot.Power
}

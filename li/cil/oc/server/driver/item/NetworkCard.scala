package li.cil.oc.server.driver.item

import li.cil.oc.Items
import li.cil.oc.api.driver.Slot
import li.cil.oc.server.component
import net.minecraft.item.ItemStack

object NetworkCard extends Item {
  def worksWith(item: ItemStack) = isOneOf(item, Items.lan)

  override def createEnvironment(item: ItemStack, container: AnyRef) = new component.NetworkCard()

  def slot(item: ItemStack) = Slot.Card
}

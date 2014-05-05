package li.cil.oc.server.driver.item

import li.cil.oc.api
import li.cil.oc.api.driver.Slot
import li.cil.oc.common.component
import li.cil.oc.server.component.Container
import net.minecraft.item.ItemStack

object Screen extends Item {
  override def worksWith(stack: ItemStack) = isOneOf(stack, api.Items.get("screen1"), api.Items.get("screen2"), api.Items.get("screen3"))

  override def createEnvironment(stack: ItemStack, container: Container) = new component.TextBuffer(container)

  // Only allow programmatic 'installation' of the screen as an item.
  override def slot(stack: ItemStack) = Slot.None
}

package li.cil.oc.server.driver.item

import li.cil.oc.api
import li.cil.oc.api.driver.EnvironmentHost
import li.cil.oc.common.Slot
import li.cil.oc.server.component
import net.minecraft.item.ItemStack

object DebugCard extends Item {
  override def worksWith(stack: ItemStack) = isOneOf(stack, api.Items.get("debugCard"))

  override def createEnvironment(stack: ItemStack, host: EnvironmentHost) = new component.DebugCard(host)

  override def slot(stack: ItemStack) = Slot.Card
}

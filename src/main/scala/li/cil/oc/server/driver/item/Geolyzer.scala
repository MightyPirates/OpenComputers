package li.cil.oc.server.driver.item

import li.cil.oc.api
import li.cil.oc.api.driver.Container
import li.cil.oc.common.Slot
import li.cil.oc.server.component
import net.minecraft.item.ItemStack

object Geolyzer extends Item {
  override def worksWith(stack: ItemStack) = isOneOf(stack, api.Items.get("geolyzer"))

  override def createEnvironment(stack: ItemStack, container: Container) = new component.Geolyzer(container)

  override def slot(stack: ItemStack) = Slot.Upgrade
}

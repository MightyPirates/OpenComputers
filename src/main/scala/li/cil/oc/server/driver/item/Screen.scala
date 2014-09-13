package li.cil.oc.server.driver.item

import li.cil.oc.api
import li.cil.oc.api.driver.{Container, Slot}
import li.cil.oc.common.{component, tileentity}
import net.minecraft.item.ItemStack

object Screen extends Item {
  override def worksWith(stack: ItemStack) = isOneOf(stack, api.Items.get("screen1"))

  override def createEnvironment(stack: ItemStack, container: Container) = container match {
    case screen: tileentity.Screen if screen.tier > 0 => new component.Screen(screen)
    case _ => new component.TextBuffer(container)
  }

  override def slot(stack: ItemStack) = Slot.Upgrade
}

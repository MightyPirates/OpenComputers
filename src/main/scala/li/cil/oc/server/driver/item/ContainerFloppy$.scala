package li.cil.oc.server.driver.item

import li.cil.oc.api
import li.cil.oc.api.driver.{Container, EnvironmentHost}
import li.cil.oc.common.{Slot, Tier}
import net.minecraft.item.ItemStack

object ContainerFloppy$ extends Item with Container {
  override def worksWith(stack: ItemStack) = isOneOf(stack, api.Items.get("diskDrive"))

  override def createEnvironment(stack: ItemStack, host: EnvironmentHost) = null

  override def slot(stack: ItemStack) = Slot.Container

  override def providedSlot(stack: ItemStack) = Slot.Floppy

  override def providedTier(stack: ItemStack) = Tier.Any
}

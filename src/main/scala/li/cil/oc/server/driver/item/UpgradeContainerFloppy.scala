package li.cil.oc.server.driver.item

import li.cil.oc.api
import li.cil.oc.api.driver.{Container, Slot, UpgradeContainer}
import net.minecraft.item.ItemStack

object UpgradeContainerFloppy extends Item with UpgradeContainer {
  override def worksWith(stack: ItemStack) = isOneOf(stack, api.Items.get("diskDrive"))

  override def createEnvironment(stack: ItemStack, container: Container) = null

  override def slot(stack: ItemStack) = Slot.UpgradeContainer

  override def providedSlot(stack: ItemStack) = Slot.Disk

  override def providedTier(stack: ItemStack) = Int.MaxValue // Any
}

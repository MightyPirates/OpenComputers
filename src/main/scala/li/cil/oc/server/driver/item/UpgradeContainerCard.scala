package li.cil.oc.server.driver.item

import li.cil.oc.api.driver.{Container, Slot, UpgradeContainer}
import li.cil.oc.common.item
import li.cil.oc.{Items, api}
import net.minecraft.item.ItemStack

object UpgradeContainerCard extends Item with UpgradeContainer {
  override def worksWith(stack: ItemStack) = isOneOf(stack, api.Items.get("cardContainer1"), api.Items.get("cardContainer2"), api.Items.get("cardContainer3"))

  override def createEnvironment(stack: ItemStack, container: Container) = null

  override def slot(stack: ItemStack) = Slot.UpgradeContainer

  override def providedSlot(stack: ItemStack) = Slot.Card

  override def providedTier(stack: ItemStack) = tier(stack)

  override def tier(stack: ItemStack) =
    Items.multi.subItem(stack) match {
      case Some(container: item.UpgradeContainerCard) => container.tier
      case _ => 0
    }
}

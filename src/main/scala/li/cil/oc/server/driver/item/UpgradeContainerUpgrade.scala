package li.cil.oc.server.driver.item

import li.cil.oc.api.driver.{Container, Slot, UpgradeContainer}
import li.cil.oc.common.item
import li.cil.oc.{Items, api}
import net.minecraft.item.ItemStack

object UpgradeContainerUpgrade extends Item with UpgradeContainer {
  override def worksWith(stack: ItemStack) = isOneOf(stack, api.Items.get("upgradeContainer1"), api.Items.get("upgradeContainer2"), api.Items.get("upgradeContainer3"))

  override def createEnvironment(stack: ItemStack, container: Container) = null

  override def slot(stack: ItemStack) = Slot.UpgradeContainer

  override def providedSlot(stack: ItemStack) = Slot.Upgrade

  override def providedTier(stack: ItemStack) = tier(stack)

  override def tier(stack: ItemStack) =
    Items.multi.subItem(stack) match {
      case Some(container: item.UpgradeContainerUpgrade) => container.tier
      case _ => 0
    }
}

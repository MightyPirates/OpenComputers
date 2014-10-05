package li.cil.oc.server.driver.item

import li.cil.oc.api.driver.Container
import li.cil.oc.api.driver.EnvironmentHost
import li.cil.oc.common.Slot
import li.cil.oc.common.item
import li.cil.oc.Items
import li.cil.oc.api
import net.minecraft.item.ItemStack

object UpgradeContainer extends Item with Container {
  override def worksWith(stack: ItemStack) =
    isOneOf(stack, api.Items.get("upgradeContainer1"), api.Items.get("upgradeContainer2"), api.Items.get("upgradeContainer3"))

  override def createEnvironment(stack: ItemStack, host: EnvironmentHost) = null

  override def slot(stack: ItemStack) = Slot.Container

  override def providedSlot(stack: ItemStack) = Slot.Upgrade

  override def providedTier(stack: ItemStack) = tier(stack)

  override def tier(stack: ItemStack) =
    Items.multi.subItem(stack) match {
      case Some(container: item.UpgradeContainerUpgrade) => container.tier
      case _ => 0
    }
}

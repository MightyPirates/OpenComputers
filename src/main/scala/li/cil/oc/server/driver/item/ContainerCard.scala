package li.cil.oc.server.driver.item

import li.cil.oc.api
import li.cil.oc.api.driver.EnvironmentHost
import li.cil.oc.api.driver.item.Container
import li.cil.oc.common.Slot
import li.cil.oc.common.item
import li.cil.oc.init.Items
import net.minecraft.item.ItemStack

object ContainerCard extends Item with Container {
  override def worksWith(stack: ItemStack) =
    isOneOf(stack, api.Items.get("cardContainer1"), api.Items.get("cardContainer2"), api.Items.get("cardContainer3"))

  override def createEnvironment(stack: ItemStack, host: EnvironmentHost) = null

  override def slot(stack: ItemStack) = Slot.Container

  override def providedSlot(stack: ItemStack) = Slot.Card

  override def providedTier(stack: ItemStack) = tier(stack)

  override def tier(stack: ItemStack) =
    Items.multi.subItem(stack) match {
      case Some(container: item.UpgradeContainerCard) => container.tier
      case _ => 0
    }
}

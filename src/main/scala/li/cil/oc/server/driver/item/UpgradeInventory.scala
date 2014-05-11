package li.cil.oc.server.driver.item

import li.cil.oc.api
import li.cil.oc.api.driver.{Inventory, Slot}
import li.cil.oc.server.component
import net.minecraft.item.ItemStack

object UpgradeInventory extends Item with Inventory {
  override def worksWith(stack: ItemStack) = isOneOf(stack, api.Items.get("inventoryUpgrade"))

  override def createEnvironment(stack: ItemStack, container: component.Container) = null

  override def slot(stack: ItemStack) = Slot.Upgrade

  override def inventoryCapacity(stack: ItemStack) = 16
}

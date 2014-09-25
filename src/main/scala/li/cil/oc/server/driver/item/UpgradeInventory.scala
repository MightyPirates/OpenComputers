package li.cil.oc.server.driver.item

import li.cil.oc.api
import li.cil.oc.api.driver.{EnvironmentHost, Inventory}
import li.cil.oc.api.tileentity.Robot
import li.cil.oc.common.Slot
import net.minecraft.item.ItemStack

object UpgradeInventory extends Item with Inventory {
  override def worksWith(stack: ItemStack, host: EnvironmentHost) =
    isOneOf(stack, api.Items.get("inventoryUpgrade")) &&
      host.isInstanceOf[Robot]

  override def createEnvironment(stack: ItemStack, host: EnvironmentHost) = null

  override def slot(stack: ItemStack) = Slot.Upgrade

  override def inventoryCapacity(stack: ItemStack) = 16
}

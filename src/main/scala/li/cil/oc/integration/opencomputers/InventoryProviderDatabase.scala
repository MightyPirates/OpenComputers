package li.cil.oc.integration.opencomputers

import li.cil.oc.api.driver.InventoryProvider
import li.cil.oc.common.inventory.DatabaseInventory
import net.minecraft.entity.player.PlayerEntity
import net.minecraft.inventory.IInventory
import net.minecraft.item.ItemStack

object InventoryProviderDatabase extends InventoryProvider {
  override def worksWith(stack: ItemStack, player: PlayerEntity): Boolean = DriverUpgradeDatabase.worksWith(stack)

  override def getInventory(stack: ItemStack, player: PlayerEntity): IInventory = new DatabaseInventory {
    override def container: ItemStack = stack

    override def stillValid(player: PlayerEntity): Boolean = player == player
  }
}

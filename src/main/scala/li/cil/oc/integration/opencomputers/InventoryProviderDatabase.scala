package li.cil.oc.integration.opencomputers

import li.cil.oc.api.driver.InventoryProvider
import li.cil.oc.common.inventory.DatabaseInventory
import net.minecraft.entity.player.EntityPlayer
import net.minecraft.inventory.IInventory
import net.minecraft.item.ItemStack

object InventoryProviderDatabase extends InventoryProvider {
  override def worksWith(stack: ItemStack, player: EntityPlayer): Boolean = DriverUpgradeDatabase.worksWith(stack)

  override def getInventory(stack: ItemStack, player: EntityPlayer): IInventory = new DatabaseInventory {
    override def container: ItemStack = stack

    override def isUseableByPlayer(player: EntityPlayer): Boolean = player == player
  }
}

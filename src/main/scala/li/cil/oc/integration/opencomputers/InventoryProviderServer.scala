package li.cil.oc.integration.opencomputers

import li.cil.oc.api.driver.InventoryProvider
import li.cil.oc.common.inventory.ServerInventory
import net.minecraft.entity.player.PlayerEntity
import net.minecraft.inventory.IInventory
import net.minecraft.item.ItemStack

object InventoryProviderServer extends InventoryProvider {
  override def worksWith(stack: ItemStack, player: PlayerEntity): Boolean = DriverServer.worksWith(stack)

  override def getInventory(stack: ItemStack, player: PlayerEntity): IInventory = new ServerInventory {
    override def container: ItemStack = stack
  }
}

package li.cil.oc.integration.opencomputers

import li.cil.oc.api.driver.InventoryProvider
import li.cil.oc.common.inventory.ServerInventory
import net.minecraft.entity.player.EntityPlayer
import net.minecraft.inventory.IInventory
import net.minecraft.item.ItemStack

object InventoryProviderServer extends InventoryProvider {
  override def worksWith(stack: ItemStack, player: EntityPlayer): Boolean = DriverServer.worksWith(stack)

  override def getInventory(stack: ItemStack, player: EntityPlayer): IInventory = new ServerInventory {
    override def container: ItemStack = stack
  }
}

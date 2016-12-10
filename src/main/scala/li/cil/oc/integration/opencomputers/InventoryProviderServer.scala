package li.cil.oc.integration.opencomputers

import li.cil.oc.api.driver.InventoryProvider
import li.cil.oc.common.inventory.ServerInventory
import li.cil.oc.util.InventoryUtils
import net.minecraft.entity.player.EntityPlayer
import net.minecraft.item.ItemStack
import net.minecraftforge.items.IItemHandler

object InventoryProviderServer extends InventoryProvider {
  override def worksWith(stack: ItemStack, player: EntityPlayer): Boolean = DriverServer.worksWith(stack)

  override def getItemHandler(stack: ItemStack, player: EntityPlayer): IItemHandler = InventoryUtils.asItemHandler(new ServerInventory {
    override def container: ItemStack = stack
  })
}

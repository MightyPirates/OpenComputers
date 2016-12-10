package li.cil.oc.integration.opencomputers

import li.cil.oc.api.driver.InventoryProvider
import li.cil.oc.common.inventory.DatabaseInventory
import li.cil.oc.util.InventoryUtils
import net.minecraft.entity.player.EntityPlayer
import net.minecraft.item.ItemStack
import net.minecraftforge.items.IItemHandler

object InventoryProviderDatabase extends InventoryProvider {
  override def worksWith(stack: ItemStack, player: EntityPlayer): Boolean = DriverUpgradeDatabase.worksWith(stack)

  override def getItemHandler(stack: ItemStack, player: EntityPlayer): IItemHandler = InventoryUtils.asItemHandler(new DatabaseInventory {
    override def container: ItemStack = stack

    override def isUseableByPlayer(player: EntityPlayer): Boolean = player == player
  })
}

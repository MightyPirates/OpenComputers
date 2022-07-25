package li.cil.oc.common.inventory

import li.cil.oc.Settings
import li.cil.oc.integration.opencomputers.DriverUpgradeDatabase
import net.minecraft.item.ItemStack

trait DatabaseInventory extends ItemStackInventory {
  def tier: Int = DriverUpgradeDatabase.tier(container)

  override def getContainerSize = Settings.get.databaseEntriesPerTier(tier)

  override protected def inventoryName = "database"

  override def getMaxStackSize = 1

  override def getInventoryStackRequired = 1

  override def canPlaceItem(slot: Int, stack: ItemStack) = stack != container
}

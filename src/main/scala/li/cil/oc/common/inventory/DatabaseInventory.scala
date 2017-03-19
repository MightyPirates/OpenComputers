package li.cil.oc.common.inventory

import li.cil.oc.{Constants, Settings}
import li.cil.oc.integration.opencomputers.DriverUpgradeDatabase
import net.minecraft.item.ItemStack

trait DatabaseInventory extends ItemStackInventory {
  def tier: Int = DriverUpgradeDatabase.tier(container)

  override def getSizeInventory = Constants.databaseEntriesPerTier(tier)

  override protected def inventoryName = "Database"

  override def getInventoryStackLimit = 0

  override def getInventoryStackRequired = 0

  override def isItemValidForSlot(slot: Int, stack: ItemStack) = stack != container
}

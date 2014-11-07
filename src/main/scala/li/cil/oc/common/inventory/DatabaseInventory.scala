package li.cil.oc.common.inventory

import li.cil.oc.Settings
import net.minecraft.item.ItemStack

trait DatabaseInventory extends ItemStackInventory {
  def tier: Int

  override def getSizeInventory = Settings.get.databaseEntriesPerTier(tier)

  override protected def inventoryName = "Database"

  override def getInventoryStackLimit = 0

  override def getInventoryStackRequired = 0

  override def isItemValidForSlot(slot: Int, stack: ItemStack) = stack != container
}

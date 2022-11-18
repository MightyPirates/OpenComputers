package li.cil.oc.common.inventory

import li.cil.oc.Settings
import li.cil.oc.common.container.ContainerTypes
import li.cil.oc.common.container.{Database => DatabaseContainer}
import li.cil.oc.integration.opencomputers.DriverUpgradeDatabase
import net.minecraft.entity.player.PlayerEntity
import net.minecraft.entity.player.PlayerInventory
import net.minecraft.inventory.container.INamedContainerProvider
import net.minecraft.item.ItemStack
import net.minecraft.util.text.StringTextComponent

trait DatabaseInventory extends ItemStackInventory with INamedContainerProvider {
  def tier: Int = DriverUpgradeDatabase.tier(container)

  override def getContainerSize = Settings.get.databaseEntriesPerTier(tier)

  override protected def inventoryName = "database"

  override def getMaxStackSize = 1

  override def getInventoryStackRequired = 1

  override def canPlaceItem(slot: Int, stack: ItemStack) = stack != container

  override def getDisplayName = StringTextComponent.EMPTY

  override def createMenu(id: Int, playerInventory: PlayerInventory, player: PlayerEntity) =
    new DatabaseContainer(ContainerTypes.DATABASE, id, playerInventory, container, this, tier)
}

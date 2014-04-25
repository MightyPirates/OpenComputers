package li.cil.oc.common.inventory

import li.cil.oc.Settings
import li.cil.oc.server.driver.Registry
import net.minecraft.entity.player.EntityPlayer
import net.minecraft.item.ItemStack
import li.cil.oc.common.InventorySlots

trait ServerInventory extends ItemStackInventory {
  def tier: Int

  override def getSizeInventory = tier match {
    case 1 => 12
    case 2 => 16
    case _ => 8
  }

  override def getInventoryName = Settings.namespace + "container.Server"

  override def getInventoryStackLimit = 1

  override def isUseableByPlayer(player: EntityPlayer) = false

  override def isItemValidForSlot(slot: Int, stack: ItemStack) =
    Registry.itemDriverFor(stack) match {
      case Some(driver) =>
        val provided = InventorySlots.server(tier)(slot)
        driver.slot(stack) == provided.slot && driver.tier(stack) <= provided.tier
      case _ => false // Invalid item.
    }
}

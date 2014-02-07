package li.cil.oc.common.inventory

import li.cil.oc.Settings
import li.cil.oc.api.driver.Slot
import li.cil.oc.server.driver.Registry
import net.minecraft.item.ItemStack
import net.minecraft.entity.player.EntityPlayer

trait ServerInventory extends ItemStackInventory {
  override def getSizeInventory = 14

  override def getInvName = Settings.namespace + "container.Server"

  override def getInventoryStackLimit = 1

  override def isUseableByPlayer(player: EntityPlayer) = false

  override def isItemValidForSlot(slot: Int, stack: ItemStack) =
    (slot, Registry.itemDriverFor(stack)) match {
      case (_, None) => false // Invalid item.
      case (0, Some(driver)) => driver.slot(stack) == Slot.Card && driver.tier(stack) <= 2
      case (1, Some(driver)) => driver.slot(stack) == Slot.Card && driver.tier(stack) <= 1
      case (2 | 3 | 4 | 5, Some(driver)) => driver.slot(stack) == Slot.Processor && driver.tier(stack) <= 2
      case (6 | 7 | 8 | 9, Some(driver)) => driver.slot(stack) == Slot.Memory && driver.tier(stack) <= 2
      case (10 | 11 | 12 | 13, Some(driver)) => driver.slot(stack) == Slot.HardDiskDrive && driver.tier(stack) <= 2
      case _ => false // Invalid slot.
    }
}

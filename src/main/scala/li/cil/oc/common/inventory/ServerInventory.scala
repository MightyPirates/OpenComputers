package li.cil.oc.common.inventory

import li.cil.oc.Settings
import li.cil.oc.api.driver.Slot
import li.cil.oc.server.driver.Registry
import net.minecraft.entity.player.EntityPlayer
import net.minecraft.item.ItemStack

trait ServerInventory extends ItemStackInventory {
  def tier: Int

  override def getSizeInventory = tier match {
    case 1 => 11
    case 2 => 14
    case _ => 8
  }

  override def getInventoryName = Settings.namespace + "container.Server"

  override def getInventoryStackLimit = 1

  override def isUseableByPlayer(player: EntityPlayer) = false

  override def isItemValidForSlot(slot: Int, stack: ItemStack) = tier match {
    case 1 => (slot, Registry.itemDriverFor(stack)) match {
      case (_, None) => false // Invalid item.
      case (0, Some(driver)) => driver.slot(stack) == Slot.Card && driver.tier(stack) <= 2
      case (1, Some(driver)) => driver.slot(stack) == Slot.Card && driver.tier(stack) <= 1
      case (2 | 3 | 4, Some(driver)) => driver.slot(stack) == Slot.Processor && driver.tier(stack) <= 2
      case (5 | 6 | 7, Some(driver)) => driver.slot(stack) == Slot.Memory && driver.tier(stack) <= 2
      case (8 | 9 | 10, Some(driver)) => driver.slot(stack) == Slot.HardDiskDrive && driver.tier(stack) <= 2
      case _ => false // Invalid slot.
    }
    case 2 => (slot, Registry.itemDriverFor(stack)) match {
      case (_, None) => false // Invalid item.
      case (0, Some(driver)) => driver.slot(stack) == Slot.Card && driver.tier(stack) <= 2
      case (1, Some(driver)) => driver.slot(stack) == Slot.Card && driver.tier(stack) <= 1
      case (2 | 3 | 4 | 5, Some(driver)) => driver.slot(stack) == Slot.Processor && driver.tier(stack) <= 2
      case (6 | 7 | 8 | 9, Some(driver)) => driver.slot(stack) == Slot.Memory && driver.tier(stack) <= 2
      case (10 | 11 | 12 | 13, Some(driver)) => driver.slot(stack) == Slot.HardDiskDrive && driver.tier(stack) <= 2
      case _ => false // Invalid slot.
    }
    case _ => (slot, Registry.itemDriverFor(stack)) match {
      case (_, None) => false // Invalid item.
      case (0, Some(driver)) => driver.slot(stack) == Slot.Card && driver.tier(stack) <= 2
      case (1, Some(driver)) => driver.slot(stack) == Slot.Card && driver.tier(stack) <= 1
      case (2 | 3, Some(driver)) => driver.slot(stack) == Slot.Processor && driver.tier(stack) <= 2
      case (4 | 5, Some(driver)) => driver.slot(stack) == Slot.Memory && driver.tier(stack) <= 2
      case (6 | 7, Some(driver)) => driver.slot(stack) == Slot.HardDiskDrive && driver.tier(stack) <= 2
      case _ => false // Invalid slot.
    }
  }
}

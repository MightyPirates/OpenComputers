package li.cil.oc.common.inventory

import li.cil.oc.api.Driver
import li.cil.oc.common.tileentity
import li.cil.oc.common.Slot
import net.minecraft.item.ItemStack

trait DiskDriveMountableInventory extends ItemStackInventory {
  def tier: Int = 1

  override def getSizeInventory = 1

  override protected def inventoryName = "diskdrive"

  override def getInventoryStackLimit = 1

  override def isItemValidForSlot(slot: Int, stack: ItemStack): Boolean = (slot, Option(Driver.driverFor(stack, classOf[tileentity.DiskDrive]))) match {
    case (0, Some(driver)) => driver.slot(stack) == Slot.Floppy
    case _ => false
  }
}

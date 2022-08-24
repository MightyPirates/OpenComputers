package li.cil.oc.common.inventory

import li.cil.oc.api.Driver
import li.cil.oc.common.Slot
import li.cil.oc.common.container.ContainerTypes
import li.cil.oc.common.container.{DiskDrive => DiskDriveContainer}
import li.cil.oc.common.tileentity
import net.minecraft.entity.player.PlayerEntity
import net.minecraft.entity.player.PlayerInventory
import net.minecraft.inventory.container.INamedContainerProvider
import net.minecraft.item.ItemStack
import net.minecraft.util.text.StringTextComponent

trait DiskDriveMountableInventory extends ItemStackInventory with INamedContainerProvider {
  def tier: Int = 1

  override def getContainerSize = 1

  override protected def inventoryName = "diskdrive"

  override def getMaxStackSize = 1

  override def canPlaceItem(slot: Int, stack: ItemStack): Boolean = (slot, Option(Driver.driverFor(stack, classOf[tileentity.DiskDrive]))) match {
    case (0, Some(driver)) => driver.slot(stack) == Slot.Floppy
    case _ => false
  }

  override def getDisplayName = StringTextComponent.EMPTY

  override def createMenu(id: Int, playerInventory: PlayerInventory, player: PlayerEntity) =
    new DiskDriveContainer(ContainerTypes.DISK_DRIVE, id, playerInventory, this)
}

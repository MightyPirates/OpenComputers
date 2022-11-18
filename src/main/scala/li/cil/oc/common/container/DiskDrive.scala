package li.cil.oc.common.container

import li.cil.oc.common.Slot
import li.cil.oc.common.tileentity
import net.minecraft.entity.player.PlayerInventory
import net.minecraft.inventory.IInventory
import net.minecraft.inventory.container.ContainerType

class DiskDrive(selfType: ContainerType[_ <: DiskDrive], id: Int, playerInventory: PlayerInventory, drive: IInventory)
  extends Player(selfType, id, playerInventory, drive) {

  override protected def getHostClass = classOf[tileentity.DiskDrive]

  addSlotToContainer(80, 35, Slot.Floppy)
  addPlayerInventorySlots(8, 84)
}

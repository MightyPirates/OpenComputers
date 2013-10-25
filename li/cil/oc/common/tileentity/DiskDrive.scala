package li.cil.oc.common.tileentity

import li.cil.oc.api.driver.Slot
import li.cil.oc.api.network.{Component, Message, Visibility}
import li.cil.oc.server.driver.Registry
import net.minecraft.entity.player.EntityPlayer
import net.minecraft.item.ItemStack
import net.minecraft.nbt.NBTTagCompound

class DiskDrive extends Rotatable with Component with ComponentInventory {
  val name = "disk_drive"

  val visibility = Visibility.Network

  componentVisibility = visibility

  def world = worldObj

  // ----------------------------------------------------------------------- //

  override def canUpdate = false

  override def receive(message: Message) = Option(super.receive(message)).orElse {
    components(0) match {
      case Some(node) if node.address.isDefined => Option(node.receive(message))
      case _ if message.name.startsWith("fs.") => result(Unit, "no disk")
      case _ => None
    }
  }.orNull

  // ----------------------------------------------------------------------- //

  override def readFromNBT(nbt: NBTTagCompound) {
    super[Rotatable].readFromNBT(nbt)
    super.readFromNBT(nbt)
  }

  override def writeToNBT(nbt: NBTTagCompound) {
    super[Rotatable].writeToNBT(nbt)
    super.writeToNBT(nbt)
  }

  // ----------------------------------------------------------------------- //

  def getInvName = "oc.container.DiskDrive"

  def getSizeInventory = 1

  def isItemValidForSlot(slot: Int, item: ItemStack) = (slot, Registry.driverFor(item)) match {
    case (0, Some(driver)) => driver.slot(item) == Slot.Disk
    case _ => false
  }

  def isUseableByPlayer(player: EntityPlayer) =
    worldObj.getBlockTileEntity(xCoord, yCoord, zCoord) == this &&
      player.getDistanceSq(xCoord + 0.5, yCoord + 0.5, zCoord + 0.5) < 64
}

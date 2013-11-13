package li.cil.oc.common.tileentity

import li.cil.oc.api.driver.Slot
import li.cil.oc.api.network.{Component, Visibility}
import li.cil.oc.client.{PacketSender => ClientPacketSender}
import li.cil.oc.server.driver.Registry
import li.cil.oc.server.{PacketSender => ServerPacketSender}
import li.cil.oc.{Config, api}
import net.minecraft.entity.player.EntityPlayer
import net.minecraft.item.ItemStack
import net.minecraft.nbt.NBTTagCompound

class DiskDrive extends Environment with ComponentInventory with Rotatable {
  val node = api.Network.newNode(this, Visibility.None).create()

  // ----------------------------------------------------------------------- //

  override def validate() = {
    super.validate()
    if (worldObj.isRemote) {
      ClientPacketSender.sendRotatableStateRequest(this)
    }
  }

  // ----------------------------------------------------------------------- //

  override def readFromNBT(nbt: NBTTagCompound) {
    super.readFromNBT(nbt)
    super.load(nbt)
  }

  override def writeToNBT(nbt: NBTTagCompound) {
    super.writeToNBT(nbt)
    super.save(nbt)
  }

  // ----------------------------------------------------------------------- //

  override protected def onRotationChanged() {
    if (!world.isRemote) {
      ServerPacketSender.sendRotatableState(this)
    }
  }

  def getInvName = Config.namespace + "container.DiskDrive"

  def getSizeInventory = 1

  def isItemValidForSlot(slot: Int, item: ItemStack) = (slot, Registry.driverFor(item)) match {
    case (0, Some(driver)) => driver.slot(item) == Slot.Disk
    case _ => false
  }

  def isUseableByPlayer(player: EntityPlayer) =
    worldObj.getBlockTileEntity(xCoord, yCoord, zCoord) == this &&
      player.getDistanceSq(xCoord + 0.5, yCoord + 0.5, zCoord + 0.5) < 64

  override protected def onItemAdded(slot: Int, item: ItemStack) {
    super.onItemAdded(slot, item)
    components(slot) match {
      case Some(environment) => environment.node match {
        case component: Component => component.setVisibility(Visibility.Network)
      }
      case _ =>
    }
  }
}

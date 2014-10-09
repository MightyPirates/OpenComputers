package li.cil.oc.common.tileentity

import cpw.mods.fml.relauncher.Side
import cpw.mods.fml.relauncher.SideOnly
import li.cil.oc.api
import li.cil.oc.api.Driver
import li.cil.oc.api.network.Analyzable
import li.cil.oc.api.network.Component
import li.cil.oc.api.network.Visibility
import li.cil.oc.common.Slot
import li.cil.oc.common.Sound
import li.cil.oc.server.{PacketSender => ServerPacketSender}
import li.cil.oc.util.ExtendedNBT._
import net.minecraft.entity.player.EntityPlayer
import net.minecraft.item.ItemStack
import net.minecraft.nbt.NBTTagCompound

class DiskDrive extends traits.Environment with traits.ComponentInventory with traits.Rotatable with Analyzable {
  val node = api.Network.newNode(this, Visibility.None).create()

  // Used on client side to check whether to render disk activity indicators.
  var lastAccess = 0L

  // ----------------------------------------------------------------------- //

  override def onAnalyze(player: EntityPlayer, side: Int, hitX: Float, hitY: Float, hitZ: Float) =
    components(0) match {
      case Some(environment) => Array(environment.node)
      case _ => null
    }

  override def canUpdate = false

  // ----------------------------------------------------------------------- //

  override def getSizeInventory = 1

  override def isItemValidForSlot(slot: Int, stack: ItemStack) = (slot, Option(Driver.driverFor(stack, getClass))) match {
    case (0, Some(driver)) => driver.slot(stack) == Slot.Floppy
    case _ => false
  }

  override protected def onItemAdded(slot: Int, stack: ItemStack) {
    super.onItemAdded(slot, stack)
    components(slot) match {
      case Some(environment) => environment.node match {
        case component: Component => component.setVisibility(Visibility.Network)
      }
      case _ =>
    }
    Sound.playDiskInsert(this)
    if (isServer) {
      ServerPacketSender.sendFloppyChange(this, stack)
    }
  }

  override protected def onItemRemoved(slot: Int, stack: ItemStack) {
    super.onItemRemoved(slot, stack)
    Sound.playDiskEject(this)
    if (isServer) {
      ServerPacketSender.sendFloppyChange(this)
    }
  }

  // ----------------------------------------------------------------------- //

  @SideOnly(Side.CLIENT) override
  def readFromNBTForClient(nbt: NBTTagCompound) {
    super.readFromNBTForClient(nbt)
    items(0) match {
      case Some(stack) => nbt.setNewCompoundTag("disk", stack.writeToNBT)
      case _ =>
    }
  }

  override def writeToNBTForClient(nbt: NBTTagCompound) {
    super.writeToNBTForClient(nbt)
    if (nbt.hasKey("disk")) {
      setInventorySlotContents(0, ItemStack.loadItemStackFromNBT(nbt.getCompoundTag("disk")))
    }
  }
}

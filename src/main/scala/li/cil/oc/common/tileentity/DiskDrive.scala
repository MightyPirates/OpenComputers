package li.cil.oc.common.tileentity

import li.cil.oc.Settings
import li.cil.oc.api
import li.cil.oc.api.Driver
import li.cil.oc.api.machine.Arguments
import li.cil.oc.api.machine.Callback
import li.cil.oc.api.machine.Context
import li.cil.oc.api.network.Analyzable
import li.cil.oc.api.network.Component
import li.cil.oc.api.network.Node
import li.cil.oc.api.network.Visibility
import li.cil.oc.common.Slot
import li.cil.oc.common.Sound
import li.cil.oc.server.{PacketSender => ServerPacketSender}
import li.cil.oc.util.ExtendedNBT._
import li.cil.oc.util.InventoryUtils
import net.minecraft.entity.player.EntityPlayer
import net.minecraft.item.ItemStack
import net.minecraft.nbt.NBTTagCompound
import net.minecraft.util.EnumFacing
import net.minecraftforge.fml.relauncher.Side
import net.minecraftforge.fml.relauncher.SideOnly

class DiskDrive extends traits.Environment with traits.ComponentInventory with traits.Rotatable with Analyzable {
  // Used on client side to check whether to render disk activity indicators.
  var lastAccess = 0L

  def filesystemNode = components(0) match {
    case Some(environment) => Option(environment.node)
    case _ => None
  }

  // ----------------------------------------------------------------------- //
  // Environment

  val node = api.Network.newNode(this, Visibility.Network).
    withComponent("disk_drive").
    create()

  @Callback(doc = """function():boolean -- Checks whether some medium is currently in the drive.""")
  def isEmpty(context: Context, args: Arguments): Array[AnyRef] = {
    result(filesystemNode.isEmpty)
  }

  @Callback(doc = """function([velocity:number]):boolean -- Eject the currently present medium from the drive.""")
  def eject(context: Context, args: Arguments): Array[AnyRef] = {
    val velocity = args.optDouble(0, 0) max 0 min 1
    val ejected = decrStackSize(0, 1)
    if (ejected != null && ejected.stackSize > 0) {
      val entity = InventoryUtils.spawnStackInWorld(position, ejected, Option(facing))
      if (entity != null) {
        val vx = facing.getFrontOffsetX * velocity
        val vy = facing.getFrontOffsetY * velocity
        val vz = facing.getFrontOffsetZ * velocity
        entity.addVelocity(vx, vy, vz)
      }
      result(true)
    }
    else result(false)
  }

  // ----------------------------------------------------------------------- //
  // Analyzable

  override def onAnalyze(player: EntityPlayer, side: EnumFacing, hitX: Float, hitY: Float, hitZ: Float) = filesystemNode.fold(null: Array[Node])(Array(_))

  // ----------------------------------------------------------------------- //
  // IInventory

  override def getSizeInventory = 1

  override def isItemValidForSlot(slot: Int, stack: ItemStack) = (slot, Option(Driver.driverFor(stack, getClass))) match {
    case (0, Some(driver)) => driver.slot(stack) == Slot.Floppy
    case _ => false
  }

  // ----------------------------------------------------------------------- //
  // ComponentInventory

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
  // TileEntity

  override def canUpdate = false

  private final val DiskTag = Settings.namespace + "disk"

  @SideOnly(Side.CLIENT) override
  def readFromNBTForClient(nbt: NBTTagCompound) {
    super.readFromNBTForClient(nbt)
    if (nbt.hasKey(DiskTag)) {
      setInventorySlotContents(0, ItemStack.loadItemStackFromNBT(nbt.getCompoundTag(DiskTag)))
    }
  }

  override def writeToNBTForClient(nbt: NBTTagCompound) {
    super.writeToNBTForClient(nbt)
    items(0).foreach(stack => nbt.setNewCompoundTag(DiskTag, stack.writeToNBT))
  }
}

package li.cil.oc.common.tileentity

import java.util

import li.cil.oc.Constants
import li.cil.oc.api.driver.DeviceInfo.DeviceAttribute
import li.cil.oc.api.driver.DeviceInfo.DeviceClass
import li.cil.oc.Settings
import li.cil.oc.api
import li.cil.oc.api.Driver
import li.cil.oc.api.driver.DeviceInfo
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

import scala.collection.convert.WrapAsJava._

class DiskDrive extends traits.Environment with traits.ComponentInventory with traits.Rotatable with Analyzable with DeviceInfo {
  // Used on client side to check whether to render disk activity indicators.
  var lastAccess = 0L

  def filesystemNode: Option[Node] = components(0) match {
    case Some(environment) => Option(environment.node)
    case _ => None
  }

  private final lazy val deviceInfo = Map(
    DeviceAttribute.Class -> DeviceClass.Disk,
    DeviceAttribute.Description -> "Floppy disk drive",
    DeviceAttribute.Vendor -> Constants.DeviceInfo.DefaultVendor,
    DeviceAttribute.Product -> "Spinner 520p1"
  )

  override def getDeviceInfo: util.Map[String, String] = deviceInfo

  // ----------------------------------------------------------------------- //
  // Environment

  val node: Component = api.Network.newNode(this, Visibility.Network).
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
    if (!ejected.isEmpty) {
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

  override def onAnalyze(player: EntityPlayer, side: EnumFacing, hitX: Float, hitY: Float, hitZ: Float): Array[Node] = filesystemNode.fold(null: Array[Node])(Array(_))

  // ----------------------------------------------------------------------- //
  // IInventory

  override def getSizeInventory = 1

  override def isItemValidForSlot(slot: Int, stack: ItemStack): Boolean = (slot, Option(Driver.driverFor(stack, getClass))) match {
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
    if (isServer) {
      ServerPacketSender.sendFloppyChange(this, stack)
      Sound.playDiskInsert(this)
    }
  }

  override protected def onItemRemoved(slot: Int, stack: ItemStack) {
    super.onItemRemoved(slot, stack)
    if (isServer) {
      ServerPacketSender.sendFloppyChange(this)
      Sound.playDiskEject(this)
    }
  }

  // ----------------------------------------------------------------------- //
  // TileEntity

  private final val DiskTag = Settings.namespace + "disk"

  @SideOnly(Side.CLIENT) override
  def readFromNBTForClient(nbt: NBTTagCompound) {
    super.readFromNBTForClient(nbt)
    if (nbt.hasKey(DiskTag)) {
      setInventorySlotContents(0, new ItemStack(nbt.getCompoundTag(DiskTag)))
    }
  }

  override def writeToNBTForClient(nbt: NBTTagCompound) {
    super.writeToNBTForClient(nbt)
    if (!items(0).isEmpty) nbt.setNewCompoundTag(DiskTag, items(0).writeToNBT)
  }
}

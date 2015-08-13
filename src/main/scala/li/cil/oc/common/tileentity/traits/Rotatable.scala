package li.cil.oc.common.tileentity.traits

import cpw.mods.fml.relauncher.Side
import cpw.mods.fml.relauncher.SideOnly
import li.cil.oc.Settings
import li.cil.oc.api.internal
import li.cil.oc.server.{PacketSender => ServerPacketSender}
import li.cil.oc.util.ExtendedWorld._
import li.cil.oc.util.RotationHelper
import net.minecraft.entity.Entity
import net.minecraft.nbt.NBTTagCompound
import net.minecraftforge.common.util.ForgeDirection

/** TileEntity base class for rotatable blocks. */
trait Rotatable extends RotationAware with internal.Rotatable {
  // ----------------------------------------------------------------------- //
  // Lookup tables
  // ----------------------------------------------------------------------- //

  private val pitch2Direction = Array(ForgeDirection.UP, ForgeDirection.NORTH, ForgeDirection.DOWN)

  private val yaw2Direction = Array(ForgeDirection.SOUTH, ForgeDirection.WEST, ForgeDirection.NORTH, ForgeDirection.EAST)

  // ----------------------------------------------------------------------- //
  // State
  // ----------------------------------------------------------------------- //

  /** One of Up, Down and North (where north means forward/no pitch). */
  private var _pitch = ForgeDirection.NORTH

  /** One of the four cardinal directions. */
  private var _yaw = ForgeDirection.SOUTH

  // ----------------------------------------------------------------------- //
  // Accessors
  // ----------------------------------------------------------------------- //

  def pitch = _pitch

  def pitch_=(value: ForgeDirection): Unit =
    trySetPitchYaw(value match {
      case ForgeDirection.DOWN | ForgeDirection.UP => value
      case _ => ForgeDirection.NORTH
    }, _yaw)

  def yaw = _yaw

  def yaw_=(value: ForgeDirection): Unit =
    trySetPitchYaw(pitch, value match {
      case ForgeDirection.DOWN | ForgeDirection.UP => _yaw
      case _ => value
    })

  def setFromEntityPitchAndYaw(entity: Entity) =
    trySetPitchYaw(
      pitch2Direction((entity.rotationPitch / 90).round + 1),
      yaw2Direction((entity.rotationYaw / 360 * 4).round & 3))

  def setFromFacing(value: ForgeDirection) =
    value match {
      case ForgeDirection.DOWN | ForgeDirection.UP =>
        trySetPitchYaw(value, yaw)
      case yaw =>
        trySetPitchYaw(ForgeDirection.NORTH, yaw)
    }

  def invertRotation() =
    trySetPitchYaw(_pitch match {
      case ForgeDirection.DOWN | ForgeDirection.UP => _pitch.getOpposite
      case _ => ForgeDirection.NORTH
    }, _yaw.getOpposite)

  override def facing = _pitch match {
    case ForgeDirection.DOWN | ForgeDirection.UP => _pitch
    case _ => _yaw
  }

  def rotate(axis: ForgeDirection) = {
    val block = world.getBlock(position)
    if (block != null) {
      val valid = block.getValidRotations(world, x, y, z)
      if (valid != null && valid.contains(axis)) {
        val (newPitch, newYaw) = facing.getRotation(axis) match {
          case value@(ForgeDirection.UP | ForgeDirection.DOWN) =>
            if (value == pitch) (value, yaw.getRotation(axis))
            else (value, yaw)
          case value => (ForgeDirection.NORTH, value)
        }
        trySetPitchYaw(newPitch, newYaw)
      }
      else false
    }
    else false
  }

  override def toLocal(value: ForgeDirection) = RotationHelper.toLocal(_pitch, _yaw, value)

  override def toGlobal(value: ForgeDirection) = RotationHelper.toGlobal(_pitch, _yaw, value)

  def validFacings = Array(ForgeDirection.NORTH, ForgeDirection.SOUTH, ForgeDirection.WEST, ForgeDirection.EAST)

  // ----------------------------------------------------------------------- //

  protected def onRotationChanged() {
    if (isServer) {
      ServerPacketSender.sendRotatableState(this)
    }
    else {
      world.markBlockForUpdate(x, y, z)
    }
    world.notifyBlocksOfNeighborChange(x, y, z, block)
  }

  // ----------------------------------------------------------------------- //

  override def readFromNBTForServer(nbt: NBTTagCompound) = {
    super.readFromNBTForServer(nbt)
    if (nbt.hasKey(Settings.namespace + "pitch")) {
      pitch = ForgeDirection.getOrientation(nbt.getInteger(Settings.namespace + "pitch"))
    }
    if (nbt.hasKey(Settings.namespace + "yaw")) {
      yaw = ForgeDirection.getOrientation(nbt.getInteger(Settings.namespace + "yaw"))
    }
    validatePitchAndYaw()
    updateTranslation()
  }

  override def writeToNBTForServer(nbt: NBTTagCompound) = {
    super.writeToNBTForServer(nbt)
    nbt.setInteger(Settings.namespace + "pitch", pitch.ordinal)
    nbt.setInteger(Settings.namespace + "yaw", yaw.ordinal)
  }

  @SideOnly(Side.CLIENT)
  override def readFromNBTForClient(nbt: NBTTagCompound) {
    super.readFromNBTForClient(nbt)
    pitch = ForgeDirection.getOrientation(nbt.getInteger("pitch"))
    yaw = ForgeDirection.getOrientation(nbt.getInteger("yaw"))
    validatePitchAndYaw()
    updateTranslation()
  }

  override def writeToNBTForClient(nbt: NBTTagCompound) {
    super.writeToNBTForClient(nbt)
    nbt.setInteger("pitch", pitch.ordinal)
    nbt.setInteger("yaw", yaw.ordinal)
  }

  private def validatePitchAndYaw() {
    if (!Set(ForgeDirection.UP, ForgeDirection.DOWN, ForgeDirection.NORTH).contains(_pitch)) {
      _pitch = ForgeDirection.NORTH
    }
    if (!Set(ForgeDirection.NORTH, ForgeDirection.SOUTH, ForgeDirection.EAST, ForgeDirection.WEST).contains(_yaw)) {
      _yaw = ForgeDirection.SOUTH
    }
  }

  // ----------------------------------------------------------------------- //

  /** Updates cached translation array and sends notification to clients. */
  private def updateTranslation() = {
    if (world != null) {
      onRotationChanged()
    }
  }

  /** Validates new values against the allowed rotations as set in our block. */
  private def trySetPitchYaw(pitch: ForgeDirection, yaw: ForgeDirection) = {
    var changed = false
    if (pitch != _pitch) {
      changed = true
      _pitch = pitch
    }
    if (yaw != _yaw) {
      changed = true
      _yaw = yaw
    }
    if (changed) {
      updateTranslation()
    }
    changed
  }
}

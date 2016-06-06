package li.cil.oc.common.tileentity.traits

import li.cil.oc.api.internal
import li.cil.oc.common.block.property.PropertyRotatable
import li.cil.oc.server.{PacketSender => ServerPacketSender}
import li.cil.oc.util.ExtendedEnumFacing._
import li.cil.oc.util.ExtendedWorld._
import li.cil.oc.util.RotationHelper
import net.minecraft.block.state.IBlockState
import net.minecraft.entity.Entity
import net.minecraft.util.EnumFacing

/** TileEntity base class for rotatable blocks. */
trait Rotatable extends RotationAware with internal.Rotatable {
  // ----------------------------------------------------------------------- //
  // Lookup tables
  // ----------------------------------------------------------------------- //

  private val pitch2Direction = Array(EnumFacing.UP, EnumFacing.NORTH, EnumFacing.DOWN)

  private val yaw2Direction = Array(EnumFacing.SOUTH, EnumFacing.WEST, EnumFacing.NORTH, EnumFacing.EAST)

  // ----------------------------------------------------------------------- //
  // Accessors
  // ----------------------------------------------------------------------- //

  def pitch = if (world != null) getBlockType match {
    case rotatable if world.getBlockState(getPos).getProperties.containsKey(PropertyRotatable.Pitch) => world.getBlockState(getPos).getValue(PropertyRotatable.Pitch)
    case _ => EnumFacing.NORTH
  } else EnumFacing.NORTH

  def pitch_=(value: EnumFacing): Unit =
    trySetPitchYaw(value match {
      case EnumFacing.DOWN | EnumFacing.UP => value
      case _ => EnumFacing.NORTH
    }, yaw)

  def yaw = if (world != null) getBlockType match {
    case rotatable if world.getBlockState(getPos).getProperties.containsKey(PropertyRotatable.Yaw) => world.getBlockState(getPos).getValue(PropertyRotatable.Yaw)
    case rotatable if world.getBlockState(getPos).getProperties.containsKey(PropertyRotatable.Facing) => world.getBlockState(getPos).getValue(PropertyRotatable.Facing)
    case _ => EnumFacing.SOUTH
  } else EnumFacing.SOUTH

  def yaw_=(value: EnumFacing): Unit =
    trySetPitchYaw(pitch, value match {
      case EnumFacing.DOWN | EnumFacing.UP => yaw
      case _ => value
    })

  def setFromEntityPitchAndYaw(entity: Entity) =
    trySetPitchYaw(
      pitch2Direction((entity.rotationPitch / 90).round + 1),
      yaw2Direction((entity.rotationYaw / 360 * 4).round & 3))

  def setFromFacing(value: EnumFacing) =
    value match {
      case EnumFacing.DOWN | EnumFacing.UP =>
        trySetPitchYaw(value, yaw)
      case yaw =>
        trySetPitchYaw(EnumFacing.NORTH, yaw)
    }

  def invertRotation() =
    trySetPitchYaw(pitch match {
      case EnumFacing.DOWN | EnumFacing.UP => pitch.getOpposite
      case _ => EnumFacing.NORTH
    }, yaw.getOpposite)

  override def facing = pitch match {
    case EnumFacing.DOWN | EnumFacing.UP => pitch
    case _ => yaw
  }

  def rotate(axis: EnumFacing) = {
    val block = world.getBlock(position)
    if (block != null) {
      val valid = block.getValidRotations(world, getPos)
      if (valid != null && valid.contains(axis)) {
        val (newPitch, newYaw) = facing.getRotation(axis) match {
          case value@(EnumFacing.UP | EnumFacing.DOWN) =>
            if (value == pitch) (value, yaw.getRotation(axis))
            else (value, yaw)
          case value => (EnumFacing.NORTH, value)
        }
        trySetPitchYaw(newPitch, newYaw)
      }
      else false
    }
    else false
  }

  override def toLocal(value: EnumFacing) = if (value == null) null else RotationHelper.toLocal(pitch, yaw, value)

  override def toGlobal(value: EnumFacing) = if (value == null) null else RotationHelper.toGlobal(pitch, yaw, value)

  def validFacings = Array(EnumFacing.NORTH, EnumFacing.SOUTH, EnumFacing.WEST, EnumFacing.EAST)

  // ----------------------------------------------------------------------- //

  protected def onRotationChanged() {
    if (isServer) {
      ServerPacketSender.sendRotatableState(this)
    }
    else {
      world.markBlockForUpdate(getPos)
    }
    world.notifyNeighborsOfStateChange(getPos, getBlockType)
  }

  // ----------------------------------------------------------------------- //

  /** Updates cached translation array and sends notification to clients. */
  protected def updateTranslation(): Unit = {
    if (world != null) {
      onRotationChanged()
    }
  }

  /** Validates new values against the allowed rotations as set in our block. */
  protected def trySetPitchYaw(pitch: EnumFacing, yaw: EnumFacing) = {
    val oldState = world.getBlockState(getPos)
    def setState(newState: IBlockState): Boolean = {
      if (oldState.hashCode() != newState.hashCode()) {
        world.setBlockState(getPos, newState)
        updateTranslation()
        true
      }
      else false
    }
    getBlockType match {
      case rotatable if oldState.getProperties.containsKey(PropertyRotatable.Pitch) && oldState.getProperties.containsKey(PropertyRotatable.Yaw) =>
        setState(oldState.withProperty(PropertyRotatable.Pitch, pitch).withProperty(PropertyRotatable.Yaw, yaw))
      case rotatable if oldState.getProperties.containsKey(PropertyRotatable.Facing) =>
        setState(oldState.withProperty(PropertyRotatable.Facing, yaw))
      case _ => false
    }
  }
}

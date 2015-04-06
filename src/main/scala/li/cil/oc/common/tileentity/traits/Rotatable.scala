package li.cil.oc.common.tileentity.traits

import li.cil.oc.api.internal
import li.cil.oc.common.block
import li.cil.oc.server.{PacketSender => ServerPacketSender}
import li.cil.oc.util.ExtendedEnumFacing._
import li.cil.oc.util.ExtendedWorld._
import net.minecraft.block.state.IBlockState
import net.minecraft.entity.Entity
import net.minecraft.util.EnumFacing

/** TileEntity base class for rotatable blocks. */
trait Rotatable extends RotationAware with internal.Rotatable {
  // ----------------------------------------------------------------------- //
  // Lookup tables
  // ----------------------------------------------------------------------- //

  /**
   * Translates forge directions based on the block's pitch and yaw. The base
   * forward direction is facing south with no pitch. The outer array is for
   * the three different pitch states, the inner for the four different yaw
   * states.
   */
  private val translations = Array(
    // Pitch = Down
    Array(
      // Yaw = North
      Array(D.south, D.north, D.up, D.down, D.east, D.west),
      // Yaw = South
      Array(D.south, D.north, D.down, D.up, D.west, D.east),
      // Yaw = West
      Array(D.south, D.north, D.west, D.east, D.up, D.down),
      // Yaw = East
      Array(D.south, D.north, D.east, D.west, D.down, D.up)),
    // Pitch = Up
    Array(
      // Yaw = North
      Array(D.north, D.south, D.down, D.up, D.east, D.west),
      // Yaw = South
      Array(D.north, D.south, D.up, D.down, D.west, D.east),
      // Yaw = West
      Array(D.north, D.south, D.west, D.east, D.down, D.up),
      // Yaw = East
      Array(D.north, D.south, D.east, D.west, D.up, D.down)),
    // Pitch = Forward (North|East|South|West)
    Array(
      // Yaw = North
      Array(D.down, D.up, D.south, D.north, D.east, D.west),
      // Yaw = South
      Array(D.down, D.up, D.north, D.south, D.west, D.east),
      // Yaw = West
      Array(D.down, D.up, D.west, D.east, D.south, D.north),
      // Yaw = East
      Array(D.down, D.up, D.east, D.west, D.north, D.south)))

  private val pitch2Direction = Array(D.up, D.north, D.down)

  private val yaw2Direction = Array(D.south, D.west, D.north, D.east)

  /** Shortcuts for forge directions to make the above more readable. */
  private object D {
    val down = EnumFacing.DOWN
    val up = EnumFacing.UP
    val north = EnumFacing.NORTH
    val south = EnumFacing.SOUTH
    val west = EnumFacing.WEST
    val east = EnumFacing.EAST
  }

  // ----------------------------------------------------------------------- //
  // State
  // ----------------------------------------------------------------------- //

  /** Translation for facings based on current pitch and yaw. */
  private var cachedTranslation: Array[EnumFacing] = null

  /** Translation from local to global coordinates. */
  private var cachedInverseTranslation: Array[EnumFacing] = null

  protected var cacheDirty = true

  // ----------------------------------------------------------------------- //
  // Accessors
  // ----------------------------------------------------------------------- //

  def pitch = if (world != null) getBlockType match {
    case rotatable: block.traits.OmniRotatable => rotatable.getPitch(world.getBlockState(getPos))
    case _ => EnumFacing.NORTH
  } else EnumFacing.NORTH

  def pitch_=(value: EnumFacing): Unit =
    trySetPitchYaw(value match {
      case EnumFacing.DOWN | EnumFacing.UP => value
      case _ => EnumFacing.NORTH
    }, yaw)

  def yaw = if (world != null) getBlockType match {
    case rotatable: block.traits.OmniRotatable => rotatable.getYaw(world.getBlockState(getPos))
    case rotatable: block.traits.Rotatable => rotatable.getFacing(world.getBlockState(getPos))
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

  override def toLocal(value: EnumFacing) = if (value != null) {
    updateTranslation()
    cachedTranslation(value.ordinal)
  }
  else null

  override def toGlobal(value: EnumFacing) = if (value != null) {
    updateTranslation()
    cachedInverseTranslation(value.ordinal)
  }
  else null

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
  protected def updateTranslation(): Unit = if (cacheDirty) {
    cacheDirty = false

    val newTranslation = translations(pitch.ordinal)(yaw.ordinal - 2)
    if (cachedTranslation != newTranslation) {
      cachedTranslation = newTranslation
      cachedInverseTranslation = invert(cachedTranslation)
      if (world != null) {
        onRotationChanged()
      }
    }
  }

  /** Validates new values against the allowed rotations as set in our block. */
  protected def trySetPitchYaw(pitch: EnumFacing, yaw: EnumFacing) = {
    val oldState = world.getBlockState(getPos)
    def setState(newState: IBlockState): Boolean = {
      if (oldState.hashCode() != newState.hashCode()) {
        world.setBlockState(getPos, newState)
        cacheDirty = true
        true
      }
      else false
    }
    getBlockType match {
      case rotatable: block.traits.OmniRotatable =>
        setState(rotatable.withPitchAndYaw(oldState, pitch, yaw))
      case rotatable: block.traits.Rotatable =>
        setState(rotatable.withFacing(oldState, yaw))
      case _ => false
    }
  }

  private def invert(t: Array[EnumFacing]) =
    (0 until t.length).map(i => EnumFacing.getFront(t.indexOf(EnumFacing.getFront(i)))).toArray
}

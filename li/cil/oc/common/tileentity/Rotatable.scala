package li.cil.oc.common.tileentity

import li.cil.oc.client.{PacketSender => ClientPacketSender}
import li.cil.oc.server.{PacketSender => ServerPacketSender}
import net.minecraft.block.Block
import net.minecraft.entity.Entity
import net.minecraft.nbt.NBTTagCompound
import net.minecraft.tileentity.TileEntity
import net.minecraftforge.common.ForgeDirection

/** TileEntity base class for rotatable blocks. */
abstract class Rotatable extends TileEntity {
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
      Array(D.south, D.north, D.up, D.down, D.east, D.west, D.unknown),
      // Yaw = South
      Array(D.south, D.north, D.down, D.up, D.west, D.east, D.unknown),
      // Yaw = West
      Array(D.south, D.north, D.west, D.east, D.up, D.down, D.unknown),
      // Yaw = East
      Array(D.south, D.north, D.east, D.west, D.down, D.up, D.unknown)),
    // Pitch = Up
    Array(
      // Yaw = North
      Array(D.north, D.south, D.down, D.up, D.east, D.west, D.unknown),
      // Yaw = South
      Array(D.north, D.south, D.up, D.down, D.west, D.east, D.unknown),
      // Yaw = West
      Array(D.north, D.south, D.west, D.east, D.down, D.up, D.unknown),
      // Yaw = East
      Array(D.north, D.south, D.east, D.west, D.up, D.down, D.unknown)),
    // Pitch = Forward (North|East|South|West)
    Array(
      // Yaw = North
      Array(D.down, D.up, D.south, D.north, D.east, D.west, D.unknown),
      // Yaw = South
      Array(D.down, D.up, D.north, D.south, D.west, D.east, D.unknown),
      // Yaw = West
      Array(D.down, D.up, D.west, D.east, D.south, D.north, D.unknown),
      // Yaw = East
      Array(D.down, D.up, D.east, D.west, D.north, D.south, D.unknown)))

  private val pitch2Direction = Array(D.up, D.north, D.down)

  private val yaw2Direction = Array(D.south, D.west, D.north, D.east)

  /** Shortcuts for forge directions to make the above more readable. */
  private object D {
    val down = ForgeDirection.DOWN
    val up = ForgeDirection.UP
    val north = ForgeDirection.NORTH
    val south = ForgeDirection.SOUTH
    val west = ForgeDirection.WEST
    val east = ForgeDirection.EAST
    val unknown = ForgeDirection.UNKNOWN
  }

  // ----------------------------------------------------------------------- //
  // State
  // ----------------------------------------------------------------------- //

  /** One of Up, Down and North (where north means forward/no pitch). */
  private var _pitch = ForgeDirection.NORTH

  /** One of the four cardinal directions. */
  private var _yaw = ForgeDirection.SOUTH

  /** Translation for facings based on current pitch and yaw. */
  private var cachedTranslation = translations(_pitch.ordinal)(_yaw.ordinal - 2)

  /** Translation from local to global coordinates. */
  private var cachedInverseTranslation = invert(cachedTranslation)

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
        trySetPitchYaw(value, ForgeDirection.SOUTH)
      case yaw =>
        trySetPitchYaw(ForgeDirection.NORTH, yaw)
    }

  def invertRotation() =
    trySetPitchYaw(_pitch match {
      case ForgeDirection.DOWN | ForgeDirection.UP => _pitch.getOpposite
      case _ => ForgeDirection.NORTH
    }, _yaw.getOpposite)

  def facing = _pitch match {
    case ForgeDirection.DOWN | ForgeDirection.UP => _pitch
    case _ => _yaw
  }

  def toLocal(value: ForgeDirection) = cachedTranslation(value.ordinal)

  def toGlobal(value: ForgeDirection) = cachedInverseTranslation(value.ordinal)

  override def readFromNBT(nbt: NBTTagCompound) = {
    super.readFromNBT(nbt)
    _pitch = ForgeDirection.getOrientation(nbt.getInteger("pitch"))
    _yaw = ForgeDirection.getOrientation(nbt.getInteger("yaw"))
    updateTranslation()
  }

  override def writeToNBT(nbt: NBTTagCompound) = {
    super.writeToNBT(nbt)
    nbt.setInteger("pitch", _pitch.ordinal)
    nbt.setInteger("yaw", _yaw.ordinal)
  }

  override def validate() = {
    super.validate()
    if (worldObj.isRemote)
      ClientPacketSender.sendRotatableStateRequest(this)
  }

  /** Updates cached translation array and sends notification to clients. */
  private def updateTranslation() = {
    val newTranslation = translations(_pitch.ordinal)(_yaw.ordinal - 2)
    if (cachedTranslation != newTranslation) {
      cachedTranslation = newTranslation
      cachedInverseTranslation = invert(cachedTranslation)
      if (worldObj != null && !worldObj.isRemote) {
        ServerPacketSender.sendRotatableState(this)
      }
    }
  }

  /** Validates new values against the allowed rotations as set in our block. */
  private def trySetPitchYaw(pitch: ForgeDirection, yaw: ForgeDirection) = {
    val block = Block.blocksList(worldObj.getBlockId(xCoord, yCoord, zCoord))
    if (block != null) {
      val valid = block.getValidRotations(worldObj, xCoord, yCoord, zCoord)
      if (valid.contains(pitch))
        _pitch = pitch
      if (valid.contains(yaw))
        _yaw = yaw
      updateTranslation()
    }
    this
  }

  def invert(t: Array[ForgeDirection]) =
    (0 until t.length).map(i => ForgeDirection.getOrientation(t.indexOf(ForgeDirection.getOrientation(i))))
}
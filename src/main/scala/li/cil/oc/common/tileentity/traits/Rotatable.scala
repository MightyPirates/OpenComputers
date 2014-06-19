package li.cil.oc.common.tileentity.traits

import cpw.mods.fml.relauncher.{Side, SideOnly}
import li.cil.oc.server.{PacketSender => ServerPacketSender}
import li.cil.oc.{Settings, api}
import net.minecraft.entity.Entity
import net.minecraft.nbt.NBTTagCompound
import net.minecraftforge.common.util.ForgeDirection

/** TileEntity base class for rotatable blocks. */
trait Rotatable extends RotationAware with api.Rotatable {
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
    val block = world.getBlock(x, y, z)
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

  override def toLocal(value: ForgeDirection) = cachedTranslation(value.ordinal)

  override def toGlobal(value: ForgeDirection) = cachedInverseTranslation(value.ordinal)

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

  override def readFromNBT(nbt: NBTTagCompound) = {
    super.readFromNBT(nbt)
    if (nbt.hasKey(Settings.namespace + "pitch")) {
      pitch = ForgeDirection.getOrientation(nbt.getInteger(Settings.namespace + "pitch"))
    }
    if (nbt.hasKey(Settings.namespace + "yaw")) {
      yaw = ForgeDirection.getOrientation(nbt.getInteger(Settings.namespace + "yaw"))
    }
    validatePitchAndYaw()
    updateTranslation()
  }

  override def writeToNBT(nbt: NBTTagCompound) = {
    super.writeToNBT(nbt)
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
    val newTranslation = translations(_pitch.ordinal)(_yaw.ordinal - 2)
    if (cachedTranslation != newTranslation) {
      cachedTranslation = newTranslation
      cachedInverseTranslation = invert(cachedTranslation)
      if (world != null) {
        onRotationChanged()
      }
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

  private def invert(t: Array[ForgeDirection]) =
    (0 until t.length).map(i => ForgeDirection.getOrientation(t.indexOf(ForgeDirection.getOrientation(i))))
}

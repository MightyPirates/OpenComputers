package li.cil.oc.common.tileentity.traits

import li.cil.oc.Settings
import li.cil.oc.util.RotationHelper
import net.minecraft.nbt.CompoundNBT
import net.minecraft.util.Direction
import net.minecraftforge.api.distmarker.Dist
import net.minecraftforge.api.distmarker.OnlyIn

/**
 * Like Rotatable, but stores the rotation information in the TE's NBT instead
 * of the block's metadata.
 */
trait RotatableTile extends Rotatable {
  // ----------------------------------------------------------------------- //
  // State
  // ----------------------------------------------------------------------- //

  /** One of Up, Down and North (where north means forward/no pitch). */
  private var _pitch = Direction.NORTH

  /** One of the four cardinal directions. */
  private var _yaw = Direction.SOUTH

  // ----------------------------------------------------------------------- //
  // Accessors
  // ----------------------------------------------------------------------- //

  override def pitch = _pitch

  override def yaw = _yaw

  // ----------------------------------------------------------------------- //

  private final val PitchTag = Settings.namespace + "pitch"
  private final val YawTag = Settings.namespace + "yaw"

  override def loadForServer(nbt: CompoundNBT) = {
    super.loadForServer(nbt)
    if (nbt.contains(PitchTag)) {
      pitch = Direction.from3DDataValue(nbt.getInt(PitchTag))
    }
    if (nbt.contains(YawTag)) {
      yaw = Direction.from3DDataValue(nbt.getInt(YawTag))
    }
    validatePitchAndYaw()
  }

  override def saveForServer(nbt: CompoundNBT) = {
    super.saveForServer(nbt)
    nbt.putInt(PitchTag, pitch.ordinal)
    nbt.putInt(YawTag, yaw.ordinal)
  }

  @OnlyIn(Dist.CLIENT)
  override def loadForClient(nbt: CompoundNBT) {
    super.loadForClient(nbt)
    pitch = Direction.from3DDataValue(nbt.getInt(PitchTag))
    yaw = Direction.from3DDataValue(nbt.getInt(YawTag))
    validatePitchAndYaw()
  }

  override def saveForClient(nbt: CompoundNBT) {
    super.saveForClient(nbt)
    nbt.putInt(PitchTag, pitch.ordinal)
    nbt.putInt(YawTag, yaw.ordinal)
  }

  private def validatePitchAndYaw() {
    if (!_pitch.getAxis.isVertical) {
      _pitch = Direction.NORTH
    }
    if (!_yaw.getAxis.isHorizontal) {
      _yaw = Direction.SOUTH
    }
    updateTranslation()
  }

  // ----------------------------------------------------------------------- //

  /** Validates new values against the allowed rotations as set in our block. */
  override protected def trySetPitchYaw(pitch: Direction, yaw: Direction) = {
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

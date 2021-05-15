package li.cil.oc.common.tileentity.traits

import li.cil.oc.Settings
import net.minecraft.nbt.NBTTagCompound
import net.minecraft.util.EnumFacing
import net.minecraftforge.fml.relauncher.Side
import net.minecraftforge.fml.relauncher.SideOnly

/**
 * Like Rotatable, but stores the rotation information in the TE's NBT instead
 * of the block's metadata.
 */
trait RotatableTile extends Rotatable {
  // ----------------------------------------------------------------------- //
  // State
  // ----------------------------------------------------------------------- //

  /** One of Up, Down and North (where north means forward/no pitch). */
  private var _pitch = EnumFacing.NORTH

  /** One of the four cardinal directions. */
  private var _yaw = EnumFacing.SOUTH

  // ----------------------------------------------------------------------- //
  // Accessors
  // ----------------------------------------------------------------------- //

  override def pitch = _pitch

  override def yaw = _yaw

  // ----------------------------------------------------------------------- //

  private final val PitchTag = Settings.namespace + "pitch"
  private final val YawTag = Settings.namespace + "yaw"

  override def readFromNBTForServer(nbt: NBTTagCompound) = {
    super.readFromNBTForServer(nbt)
    if (nbt.hasKey(PitchTag)) {
      pitch = EnumFacing.getFront(nbt.getInteger(PitchTag))
    }
    if (nbt.hasKey(YawTag)) {
      yaw = EnumFacing.getFront(nbt.getInteger(YawTag))
    }
    validatePitchAndYaw()
  }

  override def writeToNBTForServer(nbt: NBTTagCompound) = {
    super.writeToNBTForServer(nbt)
    nbt.setInteger(PitchTag, pitch.ordinal)
    nbt.setInteger(YawTag, yaw.ordinal)
  }

  @SideOnly(Side.CLIENT)
  override def readFromNBTForClient(nbt: NBTTagCompound) {
    super.readFromNBTForClient(nbt)
    pitch = EnumFacing.getFront(nbt.getInteger(PitchTag))
    yaw = EnumFacing.getFront(nbt.getInteger(YawTag))
    validatePitchAndYaw()
  }

  override def writeToNBTForClient(nbt: NBTTagCompound) {
    super.writeToNBTForClient(nbt)
    nbt.setInteger(PitchTag, pitch.ordinal)
    nbt.setInteger(YawTag, yaw.ordinal)
  }

  private def validatePitchAndYaw() {
    if (!_pitch.getAxis.isVertical) {
      _pitch = EnumFacing.NORTH
    }
    if (!_yaw.getAxis.isHorizontal) {
      _yaw = EnumFacing.SOUTH
    }
    updateTranslation()
  }

  // ----------------------------------------------------------------------- //

  /** Validates new values against the allowed rotations as set in our block. */
  override protected def trySetPitchYaw(pitch: EnumFacing, yaw: EnumFacing) = {
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

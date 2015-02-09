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

  override def readFromNBTForServer(nbt: NBTTagCompound) = {
    super.readFromNBTForServer(nbt)
    if (nbt.hasKey(Settings.namespace + "pitch")) {
      pitch = EnumFacing.getFront(nbt.getInteger(Settings.namespace + "pitch"))
    }
    if (nbt.hasKey(Settings.namespace + "yaw")) {
      yaw = EnumFacing.getFront(nbt.getInteger(Settings.namespace + "yaw"))
    }
    validatePitchAndYaw()
  }

  override def writeToNBTForServer(nbt: NBTTagCompound) = {
    super.writeToNBTForServer(nbt)
    nbt.setInteger(Settings.namespace + "pitch", pitch.ordinal)
    nbt.setInteger(Settings.namespace + "yaw", yaw.ordinal)
  }

  @SideOnly(Side.CLIENT)
  override def readFromNBTForClient(nbt: NBTTagCompound) {
    super.readFromNBTForClient(nbt)
    pitch = EnumFacing.getFront(nbt.getInteger("pitch"))
    yaw = EnumFacing.getFront(nbt.getInteger("yaw"))
    validatePitchAndYaw()
  }

  override def writeToNBTForClient(nbt: NBTTagCompound) {
    super.writeToNBTForClient(nbt)
    nbt.setInteger("pitch", pitch.ordinal)
    nbt.setInteger("yaw", yaw.ordinal)
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
      cacheDirty = true
      updateTranslation()
    }
    changed
  }
}

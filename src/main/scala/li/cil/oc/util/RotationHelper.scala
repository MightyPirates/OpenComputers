package li.cil.oc.util

import net.minecraft.util.EnumFacing

object RotationHelper {
  def fromYaw(yaw: Float) = {
    (yaw / 360 * 4).round & 3 match {
      case 0 => EnumFacing.SOUTH
      case 1 => EnumFacing.WEST
      case 2 => EnumFacing.NORTH
      case 3 => EnumFacing.EAST
    }
  }
}

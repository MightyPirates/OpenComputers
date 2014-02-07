package li.cil.oc.util

import net.minecraftforge.common.util.ForgeDirection

object RotationHelper {
  def fromYaw(yaw: Float) = {
    (yaw / 360 * 4).round & 3 match {
      case 0 => ForgeDirection.SOUTH
      case 1 => ForgeDirection.WEST
      case 2 => ForgeDirection.NORTH
      case 3 => ForgeDirection.EAST
    }
  }
}

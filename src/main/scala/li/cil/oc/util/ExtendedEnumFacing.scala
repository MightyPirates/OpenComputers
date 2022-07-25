package li.cil.oc.util

import net.minecraft.util.Direction

import scala.language.implicitConversions

object ExtendedEnumFacing {
  implicit def ExtendedEnumFacing(facing: Direction): ExtendedEnumFacing = new ExtendedEnumFacing(facing)

  class ExtendedEnumFacing(val facing: Direction) {
    // Copy-pasta from old Forge's ForgeDirection, because MC's equivalent in Direction is client side only \o/
    private val ROTATION_MATRIX = Array(
      Array(0, 1, 4, 5, 3, 2, 6),
      Array(0, 1, 5, 4, 2, 3, 6),
      Array(5, 4, 2, 3, 0, 1, 6),
      Array(4, 5, 2, 3, 1, 0, 6),
      Array(2, 3, 1, 0, 4, 5, 6),
      Array(3, 2, 0, 1, 4, 5, 6),
      Array(0, 1, 2, 3, 4, 5, 6))

    def getRotation(axis: Direction) = {
      Direction.from3DDataValue(ROTATION_MATRIX(axis.ordinal)(facing.ordinal))
    }
  }

}

package li.cil.oc.util

import net.minecraft.util.AxisAlignedBB

import scala.language.implicitConversions

object ExtendedAABB {
  implicit def extendedAABB(bounds: AxisAlignedBB): ExtendedAABB = new ExtendedAABB(bounds)

  def unitBounds = AxisAlignedBB.getBoundingBox(0, 0, 0, 1, 1, 1)

  class ExtendedAABB(val bounds: AxisAlignedBB) {
    def volume: Int = {
      val sx = ((bounds.maxX - bounds.minX) * 16).round.toInt
      val sy = ((bounds.maxY - bounds.minY) * 16).round.toInt
      val sz = ((bounds.maxZ - bounds.minZ) * 16).round.toInt
      sx * sy * sz
    }

    def surface: Int = {
      val sx = ((bounds.maxX - bounds.minX) * 16).round.toInt
      val sy = ((bounds.maxY - bounds.minY) * 16).round.toInt
      val sz = ((bounds.maxZ - bounds.minZ) * 16).round.toInt
      sx * sy * 2 + sx * sz * 2 + sy * sz * 2
    }
  }

}

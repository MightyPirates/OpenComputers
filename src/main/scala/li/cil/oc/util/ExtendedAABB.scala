package li.cil.oc.util

import net.minecraft.util.Direction
import net.minecraft.util.math.AxisAlignedBB
import net.minecraft.util.math.BlockPos
import net.minecraft.util.math.vector.Vector3d

import scala.language.implicitConversions

object ExtendedAABB {
  implicit def extendedAABB(bounds: AxisAlignedBB): ExtendedAABB = new ExtendedAABB(bounds)

  def unitBounds = new AxisAlignedBB(0, 0, 0, 1, 1, 1)

  class ExtendedAABB(val bounds: AxisAlignedBB) {
    def offset(pos: BlockPos) = {
      new AxisAlignedBB(
        bounds.minX + pos.getX,
        bounds.minY + pos.getY,
        bounds.minZ + pos.getZ,
        bounds.maxX + pos.getX,
        bounds.maxY + pos.getY,
        bounds.maxZ + pos.getZ)
    }

    def minVec = new Vector3d(bounds.minX, bounds.minY, bounds.minZ)

    def maxVec = new Vector3d(bounds.maxX, bounds.maxY, bounds.maxZ)

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

    def rotateTowards(facing: Direction) = rotateY(facing match {
      case Direction.WEST => 3
      case Direction.NORTH => 2
      case Direction.EAST => 1
      case _ => 0
    })

    def rotateY(count: Int): AxisAlignedBB = {
      var min = new Vector3d(bounds.minX - 0.5, bounds.minY - 0.5, bounds.minZ - 0.5)
      var max = new Vector3d(bounds.maxX - 0.5, bounds.maxY - 0.5, bounds.maxZ - 0.5)
      min = min.yRot(count * Math.PI.toFloat * 0.5f)
      max = max.yRot(count * Math.PI.toFloat * 0.5f)
      new AxisAlignedBB(
        (math.min(min.x + 0.5, max.x + 0.5) * 32).round / 32f,
        (math.min(min.y + 0.5, max.y + 0.5) * 32).round / 32f,
        (math.min(min.z + 0.5, max.z + 0.5) * 32).round / 32f,
        (math.max(min.x + 0.5, max.x + 0.5) * 32).round / 32f,
        (math.max(min.y + 0.5, max.y + 0.5) * 32).round / 32f,
        (math.max(min.z + 0.5, max.z + 0.5) * 32).round / 32f)
    }
  }

}

package li.cil.oc.util

import net.minecraft.util.AxisAlignedBB
import net.minecraft.util.BlockPos
import net.minecraft.util.EnumFacing
import net.minecraft.util.Vec3

import scala.language.implicitConversions

object ExtendedAABB {
  implicit def extendedAABB(bounds: AxisAlignedBB): ExtendedAABB = new ExtendedAABB(bounds)

  def unitBounds = AxisAlignedBB.fromBounds(0, 0, 0, 1, 1, 1)

  class ExtendedAABB(val bounds: AxisAlignedBB) {
    def offset(pos: BlockPos) = {
      AxisAlignedBB.fromBounds(
        bounds.minX + pos.getX,
        bounds.minY + pos.getY,
        bounds.minZ + pos.getZ,
        bounds.maxX + pos.getX,
        bounds.maxY + pos.getY,
        bounds.maxZ + pos.getZ)
    }

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

    def rotateTowards(facing: EnumFacing) = rotateY(facing match {
      case EnumFacing.WEST => 3
      case EnumFacing.NORTH => 2
      case EnumFacing.EAST => 1
      case _ => 0
    })

    def rotateY(count: Int): AxisAlignedBB = {
      val min = new Vec3(bounds.minX - 0.5, bounds.minY - 0.5, bounds.minZ - 0.5)
      val max = new Vec3(bounds.maxX - 0.5, bounds.maxY - 0.5, bounds.maxZ - 0.5)
      min.rotateYaw(count * Math.PI.toFloat * 0.5f)
      max.rotateYaw(count * Math.PI.toFloat * 0.5f)
      AxisAlignedBB.fromBounds(
        (math.min(min.xCoord + 0.5, max.xCoord + 0.5) * 32).round / 32f,
        (math.min(min.yCoord + 0.5, max.yCoord + 0.5) * 32).round / 32f,
        (math.min(min.zCoord + 0.5, max.zCoord + 0.5) * 32).round / 32f,
        (math.max(min.xCoord + 0.5, max.xCoord + 0.5) * 32).round / 32f,
        (math.max(min.yCoord + 0.5, max.yCoord + 0.5) * 32).round / 32f,
        (math.max(min.zCoord + 0.5, max.zCoord + 0.5) * 32).round / 32f)
    }
  }

}

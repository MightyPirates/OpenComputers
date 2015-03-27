package li.cil.oc.util

import net.minecraft.util.AxisAlignedBB
import net.minecraft.util.Vec3
import net.minecraftforge.common.util.ForgeDirection

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

    def rotateTowards(facing: ForgeDirection) = rotateY(facing match {
      case ForgeDirection.WEST => 3
      case ForgeDirection.NORTH => 2
      case ForgeDirection.EAST => 1
      case _ => 0
    })

    def rotateY(count: Int): AxisAlignedBB = {
      val min = Vec3.createVectorHelper(bounds.minX - 0.5, bounds.minY - 0.5, bounds.minZ - 0.5)
      val max = Vec3.createVectorHelper(bounds.maxX - 0.5, bounds.maxY - 0.5, bounds.maxZ - 0.5)
      min.rotateAroundY(count * Math.PI.toFloat * 0.5f)
      max.rotateAroundY(count * Math.PI.toFloat * 0.5f)
      AxisAlignedBB.getBoundingBox(
        (math.min(min.xCoord + 0.5, max.xCoord + 0.5) * 32).round / 32f,
        (math.min(min.yCoord + 0.5, max.yCoord + 0.5) * 32).round / 32f,
        (math.min(min.zCoord + 0.5, max.zCoord + 0.5) * 32).round / 32f,
        (math.max(min.xCoord + 0.5, max.xCoord + 0.5) * 32).round / 32f,
        (math.max(min.yCoord + 0.5, max.yCoord + 0.5) * 32).round / 32f,
        (math.max(min.zCoord + 0.5, max.zCoord + 0.5) * 32).round / 32f)
    }
  }

}

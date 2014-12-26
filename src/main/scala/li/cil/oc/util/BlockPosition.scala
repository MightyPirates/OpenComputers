package li.cil.oc.util

import li.cil.oc.api.driver.EnvironmentHost
import net.minecraft.entity.Entity
import net.minecraft.util._
import net.minecraft.world.World

class BlockPosition(val x: Int, val y: Int, val z: Int, val world: Option[World]) {
  def this(x: Double, y: Double, z: Double, world: Option[World] = None) = this(
    math.floor(x).toInt,
    math.floor(y).toInt,
    math.floor(z).toInt,
    world
  )

  def offset(direction: EnumFacing) = new BlockPosition(
    x + direction.getFrontOffsetX,
    y + direction.getFrontOffsetY,
    z + direction.getFrontOffsetZ,
    world
  )

  def offset(x: Double, y: Double, z: Double) = new Vec3(this.x + x, this.y + y, this.z + z)

  def bounds = AxisAlignedBB.fromBounds(x, y, z, x + 1, y + 1, z + 1)

  def toBlockPos = new BlockPos(x, y, z)

  def toVec3 = new Vec3(x + 0.5, y + 0.5, z + 0.5)

  override def equals(obj: scala.Any) = obj match {
    case position: BlockPosition => position.x == x && position.y == y && position.z == z && position.world == world
    case _ => super.equals(obj)
  }
}

object BlockPosition {
  def apply(x: Int, y: Int, z: Int, world: World) = new BlockPosition(x, y, z, Option(world))

  def apply(x: Int, y: Int, z: Int) = new BlockPosition(x, y, z, None)

  def apply(x: Double, y: Double, z: Double, world: World) = new BlockPosition(x, y, z, Option(world))

  def apply(x: Double, y: Double, z: Double) = new BlockPosition(x, y, z, None)

  def apply(host: EnvironmentHost): BlockPosition = BlockPosition(host.xPosition, host.yPosition, host.zPosition, host.world)

  def apply(entity: Entity): BlockPosition = BlockPosition(entity.posX, entity.posY, entity.posZ, entity.worldObj)

  def apply(pos: BlockPos, world: World): BlockPosition = BlockPosition(pos.getX, pos.getY, pos.getZ, world)

  def apply(pos: BlockPos): BlockPosition = BlockPosition(pos.getX, pos.getY, pos.getZ)
}

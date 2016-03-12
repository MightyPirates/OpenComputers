package li.cil.oc.util

import com.google.common.hash.Hashing
import li.cil.oc.api.network.EnvironmentHost
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

  def offset(direction: EnumFacing, n: Int) = new BlockPosition(
    x + direction.getFrontOffsetX * n,
    y + direction.getFrontOffsetY * n,
    z + direction.getFrontOffsetZ * n,
    world
  )

  def offset(direction: EnumFacing): BlockPosition = offset(direction, 1)

  def offset(x: Double, y: Double, z: Double) = new Vec3(this.x + x, this.y + y, this.z + z)

  def bounds = AxisAlignedBB.fromBounds(x, y, z, x + 1, y + 1, z + 1)

  def toBlockPos = new BlockPos(x, y, z)

  def toVec3 = new Vec3(x + 0.5, y + 0.5, z + 0.5)

  override def equals(obj: scala.Any) = obj match {
    case position: BlockPosition => position.x == x && position.y == y && position.z == z && position.world == world
    case _ => super.equals(obj)
  }

  override def hashCode(): Int = {
    Hashing.
      goodFastHash(32).
      newHasher(16).
      putInt(x).
      putInt(y).
      putInt(z).
      putInt(world.hashCode()).
      hash().
      asInt()
  }
}

object BlockPosition {
  def apply(x: Int, y: Int, z: Int, world: World) = new BlockPosition(x, y, z, Option(world))

  def apply(x: Int, y: Int, z: Int) = new BlockPosition(x, y, z, None)

  def apply(x: Double, y: Double, z: Double, world: World) = new BlockPosition(x, y, z, Option(world))

  def apply(x: Double, y: Double, z: Double) = new BlockPosition(x, y, z, None)

  def apply(v: Vec3) = new BlockPosition(v.xCoord, v.yCoord, v.zCoord, None)

  def apply(v: Vec3, world: World) = new BlockPosition(v.xCoord, v.yCoord, v.zCoord, Option(world))

  def apply(host: EnvironmentHost): BlockPosition = BlockPosition(host.xPosition, host.yPosition, host.zPosition, host.world)

  def apply(entity: Entity): BlockPosition = BlockPosition(entity.posX, entity.posY, entity.posZ, entity.worldObj)

  def apply(pos: BlockPos, world: World): BlockPosition = BlockPosition(pos.getX, pos.getY, pos.getZ, world)

  def apply(pos: BlockPos): BlockPosition = BlockPosition(pos.getX, pos.getY, pos.getZ)
}

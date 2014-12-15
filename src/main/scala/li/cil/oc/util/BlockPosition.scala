package li.cil.oc.util

import appeng.api.util.DimensionalCoord
import cpw.mods.fml.common.Optional
import li.cil.oc.api.driver.EnvironmentHost
import li.cil.oc.integration.Mods
import net.minecraft.util.AxisAlignedBB
import net.minecraft.util.ChunkCoordinates
import net.minecraft.util.Vec3
import net.minecraft.world.World
import net.minecraftforge.common.util.ForgeDirection

case class BlockPosition(x: Int, y: Int, z: Int, world: Option[World]) {
  def this(x: Double, y: Double, z: Double, world: Option[World] = None) = this(
    math.floor(x).toInt,
    math.floor(y).toInt,
    math.floor(z).toInt,
    world
  )

  def this(host: EnvironmentHost) = this(
    host.xPosition,
    host.yPosition,
    host.zPosition,
    Option(host.world))

  def offset(direction: ForgeDirection) = BlockPosition(
    x + direction.offsetX,
    y + direction.offsetY,
    z + direction.offsetZ,
    world
  )

  def offset(x: Double, y: Double, z: Double) = Vec3.createVectorHelper(this.x + x, this.y + y, this.z + z)

  def bounds = AxisAlignedBB.getBoundingBox(x, y, z, x + 1, y + 1, z + 1)

  def toChunkCoordinates = new ChunkCoordinates(x, y, z)
}

object BlockPosition {
  def apply(x: Int, y: Int, z: Int, world: World) = new BlockPosition(x, y, z, Option(world))

  def apply(x: Int, y: Int, z: Int) = new BlockPosition(x, y, z, None)

  def apply(x: Double, y: Double, z: Double, world: World) = new BlockPosition(x, y, z, Option(world))

  def apply(x: Double, y: Double, z: Double) = new BlockPosition(x, y, z, None)

  def apply(host: EnvironmentHost) = new BlockPosition(host)

  @Optional.Method(modid = Mods.IDs.AppliedEnergistics2)
  def apply(coord: DimensionalCoord) = new BlockPosition(coord.x, coord.y, coord.z, Option(coord.getWorld))
}

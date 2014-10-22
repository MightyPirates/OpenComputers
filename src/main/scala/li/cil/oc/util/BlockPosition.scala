package li.cil.oc.util

import li.cil.oc.api.driver.EnvironmentHost
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
}

object BlockPosition {
  def apply(x: Int, y: Int, z: Int, world: World) = new BlockPosition(x, y, z, Option(world))

  def apply(x: Int, y: Int, z: Int) = new BlockPosition(x, y, z, None)

  def apply(x: Double, y: Double, z: Double, world: World) = new BlockPosition(x, y, z, Option(world))

  def apply(x: Double, y: Double, z: Double) = new BlockPosition(x, y, z, None)

  def apply(host: EnvironmentHost) = new BlockPosition(host)
}

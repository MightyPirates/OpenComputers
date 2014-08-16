package li.cil.oc.common.block

import li.cil.oc.common.tileentity
import net.minecraft.world.World
import net.minecraftforge.common.ForgeDirection

class Adapter(val parent: SimpleDelegator) extends SimpleDelegate {
  override protected def customTextures = Array(
    None,
    Some("AdapterTop"),
    Some("AdapterSide"),
    Some("AdapterSide"),
    Some("AdapterSide"),
    Some("AdapterSide")
  )

  // ----------------------------------------------------------------------- //

  override def hasTileEntity = true

  override def createTileEntity(world: World) = Some(new tileentity.Adapter())

  // ----------------------------------------------------------------------- //

  override def neighborBlockChanged(world: World, x: Int, y: Int, z: Int, blockId: Int) =
    world.getBlockTileEntity(x, y, z) match {
      case adapter: tileentity.Adapter => adapter.neighborChanged()
      case _ => // Ignore.
    }

  override def neighborTileChanged(world: World, x: Int, y: Int, z: Int, tileX: Int, tileY: Int, tileZ: Int) =
    world.getBlockTileEntity(x, y, z) match {
      case adapter: tileentity.Adapter =>
        val (dx, dy, dz) = (tileX - x, tileY - y, tileZ - z)
        val index = 3 + dx + dy + dy + dz + dz + dz
        if (index >= 0 && index < sides.length) {
          adapter.neighborChanged(sides(index))
        }
      case _ => // Ignore.
    }

  private val sides = Array(
    ForgeDirection.NORTH,
    ForgeDirection.DOWN,
    ForgeDirection.WEST,
    ForgeDirection.UNKNOWN,
    ForgeDirection.EAST,
    ForgeDirection.UP,
    ForgeDirection.SOUTH)
}

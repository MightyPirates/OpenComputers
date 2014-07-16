package li.cil.oc.common.block

import li.cil.oc.common.tileentity
import net.minecraft.world.World

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

  override def createTileEntity(world: World) = Some(new tileentity.Adapter)

  // ----------------------------------------------------------------------- //

  override def neighborBlockChanged(world: World, x: Int, y: Int, z: Int, blockId: Int) =
    world.getBlockTileEntity(x, y, z) match {
      case adapter: tileentity.Adapter => adapter.neighborChanged()
      case _ => // Ignore.
    }
}

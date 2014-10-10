package li.cil.oc.common.block

import li.cil.oc.common.tileentity
import net.minecraft.block.Block
import net.minecraft.world.World

class Capacitor extends SimpleBlock {
  setLightLevel(0.34f)

  override protected def customTextures = Array(
    None,
    Some("CapacitorTop"),
    Some("CapacitorSide"),
    Some("CapacitorSide"),
    Some("CapacitorSide"),
    Some("CapacitorSide")
  )

  // ----------------------------------------------------------------------- //

  override def hasTileEntity(metadata: Int) = true

  override def createTileEntity(world: World, metadata: Int) = new tileentity.Capacitor()

  // ----------------------------------------------------------------------- //

  override def onNeighborBlockChange(world: World, x: Int, y: Int, z: Int, block: Block) =
    world.getTileEntity(x, y, z) match {
      case capacitor: tileentity.Capacitor => capacitor.recomputeCapacity()
      case _ =>
    }
}

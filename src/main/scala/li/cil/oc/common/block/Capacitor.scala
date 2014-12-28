package li.cil.oc.common.block

import li.cil.oc.common.tileentity
import net.minecraft.block.Block
import net.minecraft.block.state.IBlockState
import net.minecraft.util.BlockPos
import net.minecraft.world.World

class Capacitor extends SimpleBlock {
  setLightLevel(0.34f)

  // ----------------------------------------------------------------------- //

  override def hasTileEntity(state: IBlockState) = true

  override def createNewTileEntity(world: World, metadata: Int) = new tileentity.Capacitor()

  // ----------------------------------------------------------------------- //

  override def onNeighborBlockChange(world: World, pos: BlockPos, state: IBlockState, neighborBlock: Block) =
    world.getTileEntity(pos) match {
      case capacitor: tileentity.Capacitor => capacitor.recomputeCapacity()
      case _ =>
    }
}

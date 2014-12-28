package li.cil.oc.common.block

import li.cil.oc.common.tileentity
import net.minecraft.block.state.IBlockState
import net.minecraft.world.World

class PowerDistributor extends SimpleBlock {
  setLightLevel(0.34f)

  // ----------------------------------------------------------------------- //

  override def hasTileEntity(state: IBlockState) = true

  override def createNewTileEntity(world: World, metadata: Int) = new tileentity.PowerDistributor()
}


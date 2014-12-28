package li.cil.oc.common.block

import li.cil.oc.common.tileentity
import net.minecraft.block.state.IBlockState
import net.minecraft.world.World

class MotionSensor extends SimpleBlock {
  override def hasTileEntity(state: IBlockState) = true

  override def createNewTileEntity(world: World, metadata: Int) = new tileentity.MotionSensor()
}

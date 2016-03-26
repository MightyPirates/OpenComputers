package li.cil.oc.common.block

import li.cil.oc.common.tileentity
import net.minecraft.util.BlockPos
import net.minecraft.util.EnumFacing
import net.minecraft.world.IBlockAccess
import net.minecraft.world.World

class Transposer extends SimpleBlock {
  override def isSideSolid(world: IBlockAccess, pos: BlockPos, side: EnumFacing): Boolean = false

  // ----------------------------------------------------------------------- //

  override def createNewTileEntity(world: World, meta: Int) = new tileentity.Transposer()
}

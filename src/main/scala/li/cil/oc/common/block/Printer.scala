package li.cil.oc.common.block

import li.cil.oc.common.GuiType
import li.cil.oc.common.tileentity
import net.minecraft.block.state.IBlockState
import net.minecraft.util.BlockPos
import net.minecraft.util.EnumFacing
import net.minecraft.world.IBlockAccess
import net.minecraft.world.World

class Printer extends SimpleBlock with traits.StateAware with traits.GUI {
  override def isOpaqueCube = false

  override def isFullCube = false

  override def isBlockSolid(world: IBlockAccess, pos: BlockPos, side: EnumFacing) = side == EnumFacing.DOWN

  override def isSideSolid(world: IBlockAccess, pos: BlockPos, side: EnumFacing) = side == EnumFacing.DOWN

  // ----------------------------------------------------------------------- //

  override def guiType = GuiType.Printer

  override def createNewTileEntity(world: World, metadata: Int) = new tileentity.Printer()
}

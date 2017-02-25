package li.cil.oc.common.block

import li.cil.oc.common.tileentity
import net.minecraft.block.Block
import net.minecraft.block.state.IBlockState
import net.minecraft.util.EnumFacing
import net.minecraft.util.math.BlockPos
import net.minecraft.world.IBlockAccess
import net.minecraft.world.World

abstract class RedstoneAware extends SimpleBlock {
  override def canProvidePower(state: IBlockState): Boolean = true

  override def canConnectRedstone(state: IBlockState, world: IBlockAccess, pos: BlockPos, side: EnumFacing): Boolean =
    world.getTileEntity(pos) match {
      case redstone: tileentity.traits.RedstoneAware => redstone.isOutputEnabled
      case _ => false
    }

  override def getStrongPower(state: IBlockState, world: IBlockAccess, pos: BlockPos, side: EnumFacing) =
    getWeakPower(state, world, pos, side)

  override def getWeakPower(state: IBlockState, world: IBlockAccess, pos: BlockPos, side: EnumFacing) =
    world.getTileEntity(pos) match {
      case redstone: tileentity.traits.RedstoneAware if side != null => math.min(math.max(redstone.output(side.getOpposite), 0), 15)
      case _ => super.getWeakPower(state, world, pos, side)
    }

  // ----------------------------------------------------------------------- //

  override def neighborChanged(state: IBlockState, world: World, pos: BlockPos, block: Block, fromPos: BlockPos): Unit = {
    world.getTileEntity(pos) match {
      case redstone: tileentity.traits.RedstoneAware => redstone.checkRedstoneInputChanged()
      case _ => // Ignore.
    }
  }
}

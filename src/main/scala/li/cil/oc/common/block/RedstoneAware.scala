package li.cil.oc.common.block

import li.cil.oc.common.tileentity
import net.minecraft.block.AbstractBlock.Properties
import net.minecraft.block.Block
import net.minecraft.block.BlockState
import net.minecraft.util.Direction
import net.minecraft.util.math.BlockPos
import net.minecraft.world.IBlockReader
import net.minecraft.world.World

abstract class RedstoneAware(props: Properties) extends SimpleBlock(props) {
  override def isSignalSource(state: BlockState): Boolean = true

  override def canConnectRedstone(state: BlockState, world: IBlockReader, pos: BlockPos, side: Direction): Boolean =
    world.getBlockEntity(pos) match {
      case redstone: tileentity.traits.RedstoneAware => redstone.isOutputEnabled
      case _ => false
    }

  override def getDirectSignal(state: BlockState, world: IBlockReader, pos: BlockPos, side: Direction) =
    getSignal(state, world, pos, side)

  @Deprecated
  override def getSignal(state: BlockState, world: IBlockReader, pos: BlockPos, side: Direction) =
    world.getBlockEntity(pos) match {
      case redstone: tileentity.traits.RedstoneAware if side != null => redstone.getOutput(side.getOpposite) max 0
      case _ => super.getSignal(state, world, pos, side)
    }

  // ----------------------------------------------------------------------- //

  @Deprecated
  override def neighborChanged(state: BlockState, world: World, pos: BlockPos, block: Block, fromPos: BlockPos, b: Boolean): Unit = {
    world.getBlockEntity(pos) match {
      case redstone: tileentity.traits.RedstoneAware => redstone.checkRedstoneInputChanged()
      case _ => // Ignore.
    }
  }
}

package li.cil.oc.common.block

import java.util.Random

import li.cil.oc.common.tileentity
import net.minecraft.block.Block
import net.minecraft.block.BlockState
import net.minecraft.util.math.BlockPos
import net.minecraft.world.IBlockReader
import net.minecraft.world.World
import net.minecraft.world.server.ServerWorld

class Capacitor extends SimpleBlock {
  @Deprecated
  override def isRandomlyTicking(state: BlockState) = true

  // ----------------------------------------------------------------------- //

  override def newBlockEntity(world: IBlockReader) = new tileentity.Capacitor()

  // ----------------------------------------------------------------------- //

  override def hasAnalogOutputSignal(state: BlockState): Boolean = true

  override def getAnalogOutputSignal(state: BlockState, world: World, pos: BlockPos): Int =
    world.getBlockEntity(pos) match {
      case capacitor: tileentity.Capacitor if !world.isClientSide =>
        math.round(15 * capacitor.node.localBuffer / capacitor.node.localBufferSize).toInt
      case _ => 0
    }

  override def tick(state: BlockState, world: ServerWorld, pos: BlockPos, rand: Random): Unit = {
    world.updateNeighborsAt(pos, this)
  }

  @Deprecated
  override def neighborChanged(state: BlockState, world: World, pos: BlockPos, block: Block, fromPos: BlockPos, b: Boolean): Unit =
    world.getBlockEntity(pos) match {
      case capacitor: tileentity.Capacitor => capacitor.recomputeCapacity()
      case _ =>
    }
}

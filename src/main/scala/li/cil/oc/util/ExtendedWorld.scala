package li.cil.oc.util

import li.cil.oc.api.network.EnvironmentHost
import net.minecraft.block.Block
import net.minecraft.block.Blocks
import net.minecraft.block.BlockState
import net.minecraft.block.material.Material
import net.minecraft.entity.player.PlayerEntity
import net.minecraft.tileentity.TileEntity
import net.minecraft.util.Direction
import net.minecraft.util.math.BlockPos
import net.minecraft.world.IBlockReader
import net.minecraft.world.World

import scala.language.implicitConversions

object ExtendedWorld {

  implicit def extendedBlockAccess(world: IBlockReader): ExtendedBlockAccess = new ExtendedBlockAccess(world)

  implicit def extendedWorld(world: World): ExtendedWorld = new ExtendedWorld(world)

  class ExtendedBlockAccess(val world: IBlockReader) {
    def getBlock(position: BlockPosition) = world.getBlockState(position.toBlockPos).getBlock

    def getBlockMapColor(position: BlockPosition) = getBlockMetadata(position).getMapColor(world, position.toBlockPos)

    def getBlockMetadata(position: BlockPosition) = world.getBlockState(position.toBlockPos)

    def getBlockEntity(position: BlockPosition): TileEntity = world.getBlockEntity(position.toBlockPos)

    def getBlockEntity(host: EnvironmentHost): TileEntity = getBlockEntity(BlockPosition(host))

    def isAirBlock(position: BlockPosition) = {
      val state = world.getBlockState(position.toBlockPos)
      state.getBlock.isAir(state, world, position.toBlockPos)
    }
  }

  class ExtendedWorld(override val world: World) extends ExtendedBlockAccess(world) {
    def blockExists(position: BlockPosition) = world.isLoaded(position.toBlockPos)

    def breakBlock(position: BlockPosition, drops: Boolean = true) = world.destroyBlock(position.toBlockPos, drops)

    def destroyBlockInWorldPartially(entityId: Int, position: BlockPosition, progress: Int) = world.destroyBlockProgress(entityId, position.toBlockPos, progress)

    def extinguishFire(player: PlayerEntity, position: BlockPosition, side: Direction) = {
      val pos = position.toBlockPos
      val state = world.getBlockState(pos)
      if (state.getMaterial == Material.FIRE) {
        world.setBlock(pos, Blocks.AIR.defaultBlockState, 3)
        true
      }
      else false
    }

    def getBlockHardness(position: BlockPosition) = world.getBlockState(position.toBlockPos).getDestroySpeed(world, position.toBlockPos)

    def getBlockHarvestLevel(position: BlockPosition) = getBlock(position).getHarvestLevel(getBlockMetadata(position))

    def getBlockHarvestTool(position: BlockPosition) = getBlock(position).getHarvestTool(getBlockMetadata(position))

    def computeRedstoneSignal(position: BlockPosition, side: Direction) = math.max(world.isBlockProvidingPowerTo(position.offset(side), side), world.getIndirectPowerLevelTo(position.offset(side), side))

    def isBlockProvidingPowerTo(position: BlockPosition, side: Direction) = world.getDirectSignal(position.toBlockPos, side)

    def getIndirectPowerLevelTo(position: BlockPosition, side: Direction) = world.getSignal(position.toBlockPos, side)

    def notifyBlockUpdate(pos: BlockPos): Unit = world.sendBlockUpdated(pos, world.getBlockState(pos), world.getBlockState(pos), 3)

    def notifyBlockUpdate(position: BlockPosition): Unit = world.sendBlockUpdated(position.toBlockPos, world.getBlockState(position.toBlockPos), world.getBlockState(position.toBlockPos), 3)

    def notifyBlockUpdate(position: BlockPosition, oldState: BlockState, newState: BlockState, flags: Int = 3): Unit = world.sendBlockUpdated(position.toBlockPos, oldState, newState, flags)

    def notifyBlockOfNeighborChange(position: BlockPosition, block: Block) = world.neighborChanged(position.toBlockPos, block, position.toBlockPos)

    @Deprecated
    def notifyBlocksOfNeighborChange(position: BlockPosition, block: Block, updateObservers: Boolean) = world.updateNeighborsAt(position.toBlockPos, block)

    def notifyBlocksOfNeighborChange(position: BlockPosition, block: Block, side: Direction) = world.updateNeighborsAtExceptFromFacing(position.toBlockPos, block, side)

    def playAuxSFX(id: Int, position: BlockPosition, data: Int) = world.levelEvent(id, position.toBlockPos, data)

    def setBlock(position: BlockPosition, block: Block) = world.setBlockAndUpdate(position.toBlockPos, block.defaultBlockState)

    @Deprecated
    def setBlock(position: BlockPosition, block: Block, metadata: Int, flag: Int) = {
      val states = block.getStateDefinition.getPossibleStates
      val state = if (metadata >= 0 && metadata < states.size) states.get(metadata) else block.defaultBlockState
      world.setBlock(position.toBlockPos, state, flag)
    }

    def setBlockToAir(position: BlockPosition) = world.setBlockAndUpdate(position.toBlockPos, Blocks.AIR.defaultBlockState)

    def isLoaded(position: BlockPosition) = world.isLoaded(position.toBlockPos)
  }

}

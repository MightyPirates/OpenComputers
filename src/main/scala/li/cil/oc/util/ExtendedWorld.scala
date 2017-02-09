package li.cil.oc.util

import li.cil.oc.api.network.EnvironmentHost
import net.minecraft.block.Block
import net.minecraft.block.state.IBlockState
import net.minecraft.entity.player.EntityPlayer
import net.minecraft.tileentity.TileEntity
import net.minecraft.util.EnumFacing
import net.minecraft.util.math.BlockPos
import net.minecraft.world.IBlockAccess
import net.minecraft.world.World

import scala.language.implicitConversions

object ExtendedWorld {

  implicit def extendedBlockAccess(world: IBlockAccess): ExtendedBlockAccess = new ExtendedBlockAccess(world)

  implicit def extendedWorld(world: World): ExtendedWorld = new ExtendedWorld(world)

  class ExtendedBlockAccess(val world: IBlockAccess) {
    def getBlock(position: BlockPosition) = world.getBlockState(position.toBlockPos).getBlock

    def getBlockMapColor(position: BlockPosition) = getBlock(position).getMapColor(getBlockMetadata(position))

    def getBlockMetadata(position: BlockPosition) = world.getBlockState(position.toBlockPos)

    def getTileEntity(position: BlockPosition): TileEntity = world.getTileEntity(position.toBlockPos)

    def getTileEntity(host: EnvironmentHost): TileEntity = getTileEntity(BlockPosition(host))

    def isAirBlock(position: BlockPosition) = world.isAirBlock(position.toBlockPos)

    def getLightBrightnessForSkyBlocks(position: BlockPosition, minBrightness: Int) = world.getCombinedLight(position.toBlockPos, minBrightness)
  }

  class ExtendedWorld(override val world: World) extends ExtendedBlockAccess(world) {
    def blockExists(position: BlockPosition) = world.isBlockLoaded(position.toBlockPos)

    def breakBlock(position: BlockPosition, drops: Boolean = true) = world.destroyBlock(position.toBlockPos, drops)

    def destroyBlockInWorldPartially(entityId: Int, position: BlockPosition, progress: Int) = world.sendBlockBreakProgress(entityId, position.toBlockPos, progress)

    def extinguishFire(player: EntityPlayer, position: BlockPosition, side: EnumFacing) = world.extinguishFire(player, position.toBlockPos, side)

    def getBlockHardness(position: BlockPosition) = getBlock(position).getBlockHardness(world.getBlockState(position.toBlockPos), world, position.toBlockPos)

    def getBlockHarvestLevel(position: BlockPosition) = getBlock(position).getHarvestLevel(getBlockMetadata(position))

    def getBlockHarvestTool(position: BlockPosition) = getBlock(position).getHarvestTool(getBlockMetadata(position))

    def computeRedstoneSignal(position: BlockPosition, side: EnumFacing) = math.max(world.isBlockProvidingPowerTo(position.offset(side), side), world.getIndirectPowerLevelTo(position.offset(side), side))

    def isBlockProvidingPowerTo(position: BlockPosition, side: EnumFacing) = world.getStrongPower(position.toBlockPos, side)

    def getIndirectPowerLevelTo(position: BlockPosition, side: EnumFacing) = world.getRedstonePower(position.toBlockPos, side)

    def notifyBlockUpdate(pos: BlockPos): Unit = world.notifyBlockUpdate(pos, world.getBlockState(pos), world.getBlockState(pos), 3)

    def notifyBlockUpdate(position: BlockPosition): Unit = notifyBlockUpdate(position, world.getBlockState(position.toBlockPos), world.getBlockState(position.toBlockPos))

    def notifyBlockUpdate(position: BlockPosition, oldState: IBlockState, newState: IBlockState, flags: Int = 3): Unit = world.notifyBlockUpdate(position.toBlockPos, oldState, newState, flags)

    def notifyBlockOfNeighborChange(position: BlockPosition, block: Block) = world.neighborChanged(position.toBlockPos, block, position.toBlockPos)

    def notifyBlocksOfNeighborChange(position: BlockPosition, block: Block, updateObservers: Boolean) = world.notifyNeighborsOfStateChange(position.toBlockPos, block, updateObservers)

    def notifyBlocksOfNeighborChange(position: BlockPosition, block: Block, side: EnumFacing) = world.notifyNeighborsOfStateExcept(position.toBlockPos, block, side)

    def playAuxSFX(id: Int, position: BlockPosition, data: Int) = world.playEvent(id, position.toBlockPos, data)

    def setBlock(position: BlockPosition, block: Block) = world.setBlockState(position.toBlockPos, block.getDefaultState)

    def setBlock(position: BlockPosition, block: Block, metadata: Int, flag: Int) = world.setBlockState(position.toBlockPos, block.getStateFromMeta(metadata), flag)

    def setBlockToAir(position: BlockPosition) = world.setBlockToAir(position.toBlockPos)

    def isSideSolid(position: BlockPosition, side: EnumFacing) = world.isSideSolid(position.toBlockPos, side)

    def isBlockLoaded(position: BlockPosition) = world.isBlockLoaded(position.toBlockPos)
  }

}

package li.cil.oc.util

import li.cil.oc.api.network.EnvironmentHost
import net.minecraft.block.Block
import net.minecraft.entity.player.EntityPlayer
import net.minecraft.tileentity.TileEntity
import net.minecraft.world.IBlockAccess
import net.minecraft.world.World
import net.minecraftforge.common.util.ForgeDirection

import scala.language.implicitConversions

object ExtendedWorld {

  implicit def extendedBlockAccess(world: IBlockAccess): ExtendedBlockAccess = new ExtendedBlockAccess(world)

  implicit def extendedWorld(world: World): ExtendedWorld = new ExtendedWorld(world)

  class ExtendedBlockAccess(val world: IBlockAccess) {
    def getBlock(position: BlockPosition) = world.getBlock(position.x, position.y, position.z)

    def getBlockMapColor(position: BlockPosition) = getBlock(position).getMapColor(getBlockMetadata(position))

    def getBlockMetadata(position: BlockPosition) = world.getBlockMetadata(position.x, position.y, position.z)

    def getTileEntity(position: BlockPosition): TileEntity = world.getTileEntity(position.x, position.y, position.z)

    def getTileEntity(host: EnvironmentHost): TileEntity = getTileEntity(BlockPosition(host))

    def isAirBlock(position: BlockPosition) = world.isAirBlock(position.x, position.y, position.z)

    def getLightBrightnessForSkyBlocks(position: BlockPosition, minBrightness: Int) = world.getLightBrightnessForSkyBlocks(position.x, position.y, position.z, minBrightness)
  }

  class ExtendedWorld(override val world: World) extends ExtendedBlockAccess(world) {
    def blockExists(position: BlockPosition) = world.blockExists(position.x, position.y, position.z)

    def breakBlock(position: BlockPosition, drops: Boolean = true) = world.func_147480_a(position.x, position.y, position.z, drops)

    def destroyBlockInWorldPartially(entityId: Int, position: BlockPosition, progress: Int) = world.destroyBlockInWorldPartially(entityId, position.x, position.y, position.z, progress)

    def extinguishFire(player: EntityPlayer, position: BlockPosition, side: ForgeDirection) = world.extinguishFire(player, position.x, position.y, position.z, side.ordinal)

    def getBlockHardness(position: BlockPosition) = getBlock(position).getBlockHardness(world, position.x, position.y, position.z)

    def getBlockHarvestLevel(position: BlockPosition) = getBlock(position).getHarvestLevel(getBlockMetadata(position))

    def getBlockHarvestTool(position: BlockPosition) = getBlock(position).getHarvestTool(getBlockMetadata(position))

    // Passing `side` instead of `side.getOpposite` is *correct* here, because Minecraft.
    def computeRedstoneSignal(position: BlockPosition, side: ForgeDirection) = math.max(world.isBlockProvidingPowerTo(position.offset(side), side), world.getIndirectPowerLevelTo(position.offset(side), side))

    def isBlockProvidingPowerTo(position: BlockPosition, side: ForgeDirection) = world.isBlockProvidingPowerTo(position.x, position.y, position.z, side.ordinal)

    def getIndirectPowerLevelTo(position: BlockPosition, side: ForgeDirection) = world.getIndirectPowerLevelTo(position.x, position.y, position.z, side.ordinal)

    def markBlockForUpdate(position: BlockPosition) = world.markBlockForUpdate(position.x, position.y, position.z)

    def notifyBlockOfNeighborChange(position: BlockPosition, block: Block) = world.notifyBlockOfNeighborChange(position.x, position.y, position.z, block)

    def notifyBlocksOfNeighborChange(position: BlockPosition, block: Block) = world.notifyBlocksOfNeighborChange(position.x, position.y, position.z, block)

    def notifyBlocksOfNeighborChange(position: BlockPosition, block: Block, side: ForgeDirection) = world.notifyBlocksOfNeighborChange(position.x, position.y, position.z, block, side.ordinal)

    def playAuxSFX(id: Int, position: BlockPosition, data: Int) = world.playAuxSFX(id, position.x, position.y, position.z, data)

    def setBlock(position: BlockPosition, block: Block) = world.setBlock(position.x, position.y, position.z, block)

    def setBlock(position: BlockPosition, block: Block, metadata: Int, flag: Int) = world.setBlock(position.x, position.y, position.z, block, metadata, flag)

    def setBlockToAir(position: BlockPosition) = world.setBlockToAir(position.x, position.y, position.z)

    def isSideSolid(position: BlockPosition, side: ForgeDirection) = world.isSideSolid(position.x, position.y, position.z, side)
  }

}

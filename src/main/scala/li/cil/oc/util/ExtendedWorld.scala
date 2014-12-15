package li.cil.oc.util

import li.cil.oc.api.driver.EnvironmentHost
import net.minecraft.block.Block
import net.minecraft.tileentity.TileEntity
import net.minecraft.world.IBlockAccess
import net.minecraft.world.World

import scala.language.implicitConversions

object ExtendedWorld {

  implicit def extendedBlockAccess(world: IBlockAccess): ExtendedBlockAccess = new ExtendedBlockAccess(world)

  implicit def extendedWorld(world: World): ExtendedWorld = new ExtendedWorld(world)

  class ExtendedBlockAccess(val world: IBlockAccess) {
    def getBlock(position: BlockPosition) = world.getBlock(position.x, position.y, position.z)

    def getBlockMetadata(position: BlockPosition) = world.getBlockMetadata(position.x, position.y, position.z)

    def getBlockMapColor(position: BlockPosition) = getBlock(position).getMapColor(getBlockMetadata(position))

    def getTileEntity(position: BlockPosition): TileEntity = world.getTileEntity(position.x, position.y, position.z)

    def getTileEntity(host: EnvironmentHost): TileEntity = getTileEntity(BlockPosition(host))

    def isAirBlock(position: BlockPosition) = world.isAirBlock(position.x, position.y, position.z)
  }

  class ExtendedWorld(override val world: World) extends ExtendedBlockAccess(world) {
    def blockExists(position: BlockPosition) = world.blockExists(position.x, position.y, position.z)

    def getBlockHardness(position: BlockPosition) = getBlock(position).getBlockHardness(world, position.x, position.y, position.z)

    def getBlockHarvestLevel(position: BlockPosition) = getBlock(position).getHarvestLevel(getBlockMetadata(position))

    def getBlockHarvestTool(position: BlockPosition) = getBlock(position).getHarvestTool(getBlockMetadata(position))

    def setBlockToAir(position: BlockPosition) = world.setBlockToAir(position.x, position.y, position.z)

    def notifyBlockOfNeighborChange(position: BlockPosition, block: Block) = world.notifyBlockOfNeighborChange(position.x, position.y, position.z, block)

    def breakBlock(position: BlockPosition, drops: Boolean = true) = world.func_147480_a(position.x, position.y, position.z, drops)

    def setBlock(position: BlockPosition, block: Block) = world.setBlock(position.x, position.y, position.z, block)
  }

}

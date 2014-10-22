package li.cil.oc.util

import li.cil.oc.api.driver.EnvironmentHost
import net.minecraft.tileentity.TileEntity
import net.minecraft.world.IBlockAccess
import net.minecraft.world.World

object ExtendedWorld {

  implicit def extendedBlockAccess(world: IBlockAccess) = new ExtendedBlockAccess(world)

  implicit def extendedWorld(world: World) = new ExtendedWorld(world)

  class ExtendedBlockAccess(val world: IBlockAccess) {
    def getBlock(position: BlockPosition) = world.getBlock(position.x, position.y, position.z)

    def getBlockMetadata(position: BlockPosition) = world.getBlockMetadata(position.x, position.y, position.z)

    def getBlockMapColor(position: BlockPosition) = getBlock(position).getMapColor(getBlockMetadata(position))

    def getTileEntity(position: BlockPosition): TileEntity = world.getTileEntity(position.x, position.y, position.z)

    def getTileEntity(host: EnvironmentHost): TileEntity = getTileEntity(BlockPosition(host))

    def isAirBlock(position: BlockPosition) = world.isAirBlock(position.x, position.y, position.z)
  }

  class ExtendedWorld(override val world: World) extends ExtendedBlockAccess(world) {
    def getBlockHardness(position: BlockPosition) = getBlock(position).getBlockHardness(world, position.x, position.y, position.z)

    def getBlockHarvestLevel(position: BlockPosition) = getBlock(position).getHarvestLevel(getBlockMetadata(position))

    def getBlockHarvestTool(position: BlockPosition) = getBlock(position).getHarvestTool(getBlockMetadata(position))

    def setBlockToAir(position: BlockPosition) = world.setBlockToAir(position.x, position.y, position.z)
  }

}

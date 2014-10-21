package li.cil.oc.util

import li.cil.oc.api.driver.EnvironmentHost
import net.minecraft.tileentity.TileEntity
import net.minecraft.world.IBlockAccess
import net.minecraft.world.World

object ExtendedWorld {

  implicit def extendedBlockAccess(world: IBlockAccess) = new ExtendedBlockAccess(world)

  implicit def extendedWorld(world: World) = new ExtendedWorld(world)

  class ExtendedBlockAccess(val world: IBlockAccess) {
    def getTileEntity(position: BlockPosition): TileEntity = world.getTileEntity(position.x, position.y, position.z)

    def getTileEntity(host: EnvironmentHost): TileEntity = getTileEntity(BlockPosition(host))

    def isAirBlock(position: BlockPosition) = world.isAirBlock(position.x, position.y, position.z)
  }

  class ExtendedWorld(override val world: World) extends ExtendedBlockAccess(world) {
    def setBlockToAir(position: BlockPosition) = world.setBlockToAir(position.x, position.y, position.z)
  }

}

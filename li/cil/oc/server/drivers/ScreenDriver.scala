package li.cil.oc.server.drivers

import li.cil.oc.Blocks
import li.cil.oc.api.scala.IBlockDriver
import li.cil.oc.common.block.BlockMulti
import li.cil.oc.common.components.Screen
import li.cil.oc.common.tileentity.TileEntityScreen
import net.minecraft.block.Block
import net.minecraft.world.World

object ScreenDriver extends IBlockDriver {
  def componentName = "screen"

  def worksWith(world: World, x: Int, y: Int, z: Int) =
    Block.blocksList(world.getBlockId(x, y, z)) match {
      case multi: BlockMulti => multi.subBlockId(world, x, y, z) == Blocks.screen.blockId
      case _ => false
    }

  def component(world: World, x: Int, y: Int, z: Int) =
    world.getBlockTileEntity(x, y, z) match {
      case tileEntity: TileEntityScreen => Some(tileEntity.screen)
      case _ => None
    }
}
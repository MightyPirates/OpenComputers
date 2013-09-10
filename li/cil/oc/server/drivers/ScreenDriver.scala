package li.cil.oc.server.drivers

import li.cil.oc.Config
import li.cil.oc.api.IBlockDriver
import li.cil.oc.common.tileentity.TileEntityScreen
import net.minecraft.block.Block
import net.minecraft.world.World

object ScreenDriver extends IBlockDriver {
  // ----------------------------------------------------------------------- //
  // IDriver
  // ----------------------------------------------------------------------- //

  def componentName = "screen"

  // ----------------------------------------------------------------------- //
  // IBlockDriver
  // ----------------------------------------------------------------------- //

  def worksWith(world: World, block: Block) =
    block.blockID == Config.blockScreenId

  def component(world: World, x: Int, y: Int, z: Int) =
    world.getBlockTileEntity(x, y, z).asInstanceOf[TileEntityScreen].component
}
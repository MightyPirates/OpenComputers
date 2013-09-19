package li.cil.oc.server.drivers

import li.cil.oc.Blocks
import li.cil.oc.api.IBlockDriver
import li.cil.oc.common.components.Screen
import li.cil.oc.common.tileentity.TileEntityScreen
import net.minecraft.world.World

object ScreenDriver extends IBlockDriver {
  // ----------------------------------------------------------------------- //
  // IDriver
  // ----------------------------------------------------------------------- //

  def componentName = "screen"

  def id(component: Any) = component.asInstanceOf[Screen].id

  def id(component: Any, id: Int) = component.asInstanceOf[Screen].id = id

  // ----------------------------------------------------------------------- //
  // IBlockDriver
  // ----------------------------------------------------------------------- //

  def worksWith(world: World, x: Int, y: Int, z: Int) =
    Blocks.multi.subBlockId(world, x, y, z) == Blocks.screen.blockId

  def component(world: World, x: Int, y: Int, z: Int) =
    world.getBlockTileEntity(x, y, z) match {
      case tileEntity: TileEntityScreen => tileEntity.component
      case _ => null
    }
}
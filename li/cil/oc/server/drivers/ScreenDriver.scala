package li.cil.oc.server.drivers

import li.cil.oc.Config
import li.cil.oc.api.IBlockDriver
import li.cil.oc.common.tileentity.TileEntityScreen
import li.cil.oc.server.components.Screen
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
    world.getBlockId(x, y, z) == Config.blockScreenId

  def component(world: World, x: Int, y: Int, z: Int) =
    world.getBlockTileEntity(x, y, z) match {
      case tileEntity: TileEntityScreen => tileEntity.component
      case _ => null
    }
}
package li.cil.oc.client

import li.cil.oc.common.tileentity
import li.cil.oc.common.{GuiHandler => CommonGuiHandler, GuiType}
import net.minecraft.entity.player.EntityPlayer
import net.minecraft.world.World

object GuiHandler extends CommonGuiHandler {
  override def getClientGuiElement(id: Int, player: EntityPlayer, world: World, x: Int, y: Int, z: Int) =
    world.getBlockTileEntity(x, y, z) match {
      case adapter: tileentity.Adapter if id == GuiType.Adapter.id =>
        new gui.Adapter(player.inventory, adapter)
      case computer: tileentity.Case if id == GuiType.Case.id =>
        new gui.Case(player.inventory, computer)
      case drive: tileentity.DiskDrive if id == GuiType.DiskDrive.id =>
        new gui.DiskDrive(player.inventory, drive)
      case proxy: tileentity.RobotProxy if id == GuiType.Robot.id =>
        new gui.Robot(player.inventory, proxy.robot)
      case screen: tileentity.Screen if id == GuiType.Screen.id =>
        new gui.Screen(screen)
      case _ => null
    }
}

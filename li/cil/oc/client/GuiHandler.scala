package li.cil.oc.client

import li.cil.oc.common.tileentity
import li.cil.oc.common.{GuiHandler => CommonGuiHandler, GuiType}
import net.minecraft.entity.player.EntityPlayer
import net.minecraft.world.World

object GuiHandler extends CommonGuiHandler {
  override def getClientGuiElement(id: Int, player: EntityPlayer, world: World, x: Int, y: Int, z: Int) =
    world.getBlockTileEntity(x, y, z) match {
      case computer: tileentity.Computer if id == GuiType.Computer.id =>
        new gui.Computer(player.inventory, computer)
      case screen: tileentity.Screen if id == GuiType.Screen.id =>
        new gui.Screen(screen)
      case drive: tileentity.DiskDrive if id == GuiType.DiskDrive.id =>
        new gui.DiskDrive(player.inventory, drive)
      case _ => null
    }
}

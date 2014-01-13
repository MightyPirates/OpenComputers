package li.cil.oc.common

import cpw.mods.fml.common.network.IGuiHandler
import li.cil.oc.Items
import li.cil.oc.common.inventory.ServerInventory
import net.minecraft.entity.player.EntityPlayer
import net.minecraft.world.World

abstract class GuiHandler extends IGuiHandler {
  override def getServerGuiElement(id: Int, player: EntityPlayer, world: World, x: Int, y: Int, z: Int) =
    world.getBlockTileEntity(x, y, z) match {
      case adapter: tileentity.Adapter if id == GuiType.Adapter.id =>
        new container.Adapter(player.inventory, adapter)
      case computer: tileentity.Case if id == GuiType.Case.id =>
        new container.Case(player.inventory, computer)
      case drive: tileentity.DiskDrive if id == GuiType.DiskDrive.id =>
        new container.DiskDrive(player.inventory, drive)
      case proxy: tileentity.RobotProxy if id == GuiType.Robot.id =>
        new container.Robot(player.inventory, proxy.robot)
      case rack: tileentity.Rack if id == GuiType.Rack.id =>
        new container.Rack(player.inventory, rack)
      case _ => Items.multi.subItem(player.getCurrentEquippedItem) match {
        case Some(server: item.Server) if id == GuiType.Server.id =>
          new container.Server(player.inventory, new ServerInventory {
            def container = player.getCurrentEquippedItem

            override def isUseableByPlayer(player: EntityPlayer) = player == player
          })
        case Some(terminal: item.Terminal) if id == GuiType.Terminal.id =>
          null // TODO
        case _ => null
      }
    }
}
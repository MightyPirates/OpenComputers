package li.cil.oc.common

import cpw.mods.fml.common.network.IGuiHandler
import li.cil.oc.Items
import li.cil.oc.common.inventory.ServerInventory
import net.minecraft.entity.player.EntityPlayer
import net.minecraft.world.World

abstract class GuiHandler extends IGuiHandler {
  override def getServerGuiElement(id: Int, player: EntityPlayer, world: World, x: Int, y: Int, z: Int) =
    world.getTileEntity(x, y, z) match {
      case computer: tileentity.Case if id == GuiType.Case.id =>
        new container.Case(player.inventory, computer)
      case disassembler: tileentity.Disassembler if id == GuiType.Disassembler.id =>
        new container.Disassembler(player.inventory, disassembler)
      case drive: tileentity.DiskDrive if id == GuiType.DiskDrive.id =>
        new container.DiskDrive(player.inventory, drive)
      case proxy: tileentity.RobotProxy if id == GuiType.Robot.id =>
        new container.Robot(player.inventory, proxy.robot)
      case rack: tileentity.ServerRack if id == GuiType.Rack.id =>
        new container.ServerRack(player.inventory, rack)
      case assembler: tileentity.RobotAssembler if id == GuiType.RobotAssembler.id =>
        new container.RobotAssembler(player.inventory, assembler)
      case switch: tileentity.Switch if id == GuiType.Switch.id =>
        new container.Switch(player.inventory, switch)
      case _ => Items.multi.subItem(player.getCurrentEquippedItem) match {
        case Some(server: item.Server) if id == GuiType.Server.id =>
          new container.Server(player.inventory, new ServerInventory {
            override def tier = server.tier

            override def container = player.getCurrentEquippedItem

            override def isUseableByPlayer(player: EntityPlayer) = player == player
          })
        case _ => null
      }
    }
}
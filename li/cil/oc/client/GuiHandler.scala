package li.cil.oc.client

import li.cil.oc.common.inventory.ServerInventory
import li.cil.oc.common.{GuiHandler => CommonGuiHandler, item, tileentity, GuiType}
import li.cil.oc.{Settings, Items}
import net.minecraft.entity.player.EntityPlayer
import net.minecraft.world.World
import scala.collection.convert.WrapAsScala._

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
      case rack: tileentity.Rack if id == GuiType.Rack.id =>
        new gui.Rack(player.inventory, rack)
      case screen: tileentity.Screen if id == GuiType.Screen.id =>
        new gui.Screen(screen.origin.buffer, screen.tier > 0, () => screen.origin.hasPower)
      case _ => Items.multi.subItem(player.getCurrentEquippedItem) match {
        case Some(server: item.Server) if id == GuiType.Server.id =>
          new gui.Server(player.inventory, new ServerInventory {
            def container = player.getCurrentEquippedItem

            override def isUseableByPlayer(player: EntityPlayer) = player == player
          })
        case Some(terminal: item.Terminal) if id == GuiType.Terminal.id =>
          val stack = player.getCurrentEquippedItem
          if (stack.hasTagCompound) {
            val address = stack.getTagCompound.getString(Settings.namespace + "server")
            if (address != null && !address.isEmpty) {
              // Check if bound to server is loaded. TODO optimize this?
              world.loadedTileEntityList.flatMap {
                case rack: tileentity.Rack => rack.terminals
                case _ => Iterable.empty
              } find (_.rack.isPresent.exists {
                case Some(value) => value == address
                case _ => false
              }) match {
                case Some(term) =>
                  // TODO check reachability
                  new gui.Screen(term.buffer, true, () => true)
                case _ => null
              }
            }
            else null
          }
          else null
        case _ => null
      }
    }
}

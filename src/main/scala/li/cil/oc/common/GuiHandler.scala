package li.cil.oc.common

import li.cil.oc.common.inventory.{DatabaseInventory, DiskDriveMountableInventory, ServerInventory}
import li.cil.oc.common.item.Delegator
import li.cil.oc.util.BlockPosition
import li.cil.oc.util.ExtendedWorld._
import li.cil.oc.server.component.{DiskDriveMountable, Server}
import net.minecraft.entity.player.EntityPlayer
import net.minecraft.item.ItemStack
import net.minecraft.world.World
import net.minecraftforge.fml.common.network.IGuiHandler

abstract class GuiHandler extends IGuiHandler {
  override def getServerGuiElement(id: Int, player: EntityPlayer, world: World, x: Int, y: Int, z: Int): AnyRef = {
    GuiType.Categories.get(id) match {
      case Some(GuiType.Category.Block) =>
        world.getTileEntity(BlockPosition(x, GuiType.extractY(y), z)) match {
          case t: tileentity.Adapter if id == GuiType.Adapter.id =>
            new container.Adapter(player.inventory, t)
          case t: tileentity.Assembler if id == GuiType.Assembler.id =>
            new container.Assembler(player.inventory, t)
          case t: tileentity.Charger if id == GuiType.Charger.id =>
            new container.Charger(player.inventory, t)
          case t: tileentity.Case if id == GuiType.Case.id =>
            new container.Case(player.inventory, t)
          case t: tileentity.Disassembler if id == GuiType.Disassembler.id =>
            new container.Disassembler(player.inventory, t)
          case t: tileentity.DiskDrive if id == GuiType.DiskDrive.id =>
            new container.DiskDrive(player.inventory, t)
          case t: tileentity.Printer if id == GuiType.Printer.id =>
            new container.Printer(player.inventory, t)
          case t: tileentity.Raid if id == GuiType.Raid.id =>
            new container.Raid(player.inventory, t)
          case t: tileentity.Relay if id == GuiType.Relay.id =>
            new container.Relay(player.inventory, t)
          case t: tileentity.RobotProxy if id == GuiType.Robot.id =>
            new container.Robot(player.inventory, t.robot)
          case t: tileentity.Rack if id == GuiType.Rack.id =>
            new container.Rack(player.inventory, t)
          case t: tileentity.Rack if id == GuiType.ServerInRack.id =>
            val slot = GuiType.extractSlot(y)
            val server = t.getMountable(slot).asInstanceOf[Server]
            new container.Server(player.inventory, server, Option(server))
          case t: tileentity.Rack if id == GuiType.DiskDriveMountableInRack.id =>
            val slot = GuiType.extractSlot(y)
            val drive = t.getMountable(slot).asInstanceOf[DiskDriveMountable]
            new container.DiskDrive(player.inventory, drive)
          case t: tileentity.Switch if id == GuiType.Switch.id =>
            new container.Switch(player.inventory, t)
          case _ => null
        }
      case Some(GuiType.Category.Entity) =>
        world.getEntityByID(x) match {
          case drone: entity.Drone if id == GuiType.Drone.id =>
            new container.Drone(player.inventory, drone)
          case _ => null
        }
      case Some(GuiType.Category.Item) => {
        val itemStackInUse = getItemStackInUse(id, player)
        Delegator.subItem(itemStackInUse) match {
          case Some(database: item.UpgradeDatabase) if id == GuiType.Database.id =>
            new container.Database(player.inventory, new DatabaseInventory {
              override def container = itemStackInUse

              override def isUseableByPlayer(player: EntityPlayer) = player == player
            })
          case Some(server: item.Server) if id == GuiType.Server.id =>
            new container.Server(player.inventory, new ServerInventory {
              override def container = itemStackInUse

              override def isUseableByPlayer(player: EntityPlayer) = player == player
            })
          case Some(tablet: item.Tablet) if id == GuiType.TabletInner.id =>
            val stack = itemStackInUse
            if (stack.hasTagCompound)
              new container.Tablet(player.inventory, item.Tablet.get(stack, player))
            else
              null
          case Some(drive: item.DiskDriveMountable) if id == GuiType.DiskDriveMountable.id =>
            new container.DiskDrive(player.inventory, new DiskDriveMountableInventory {
              override def container: ItemStack = itemStackInUse

              override def isUseableByPlayer(player: EntityPlayer) = player == player
            })
          case _ => null
        }
      }
      case _ => null
    }
  }

  def getItemStackInUse(id: Int, player: EntityPlayer): ItemStack = {
    val mainItem: ItemStack = player.getHeldItemMainhand
    Delegator.subItem(mainItem) match {
      case Some(drive: item.traits.FileSystemLike) if id == GuiType.Drive.id => mainItem
      case Some(database: item.UpgradeDatabase) if id == GuiType.Database.id => mainItem
      case Some(server: item.Server) if id == GuiType.Server.id => mainItem
      case Some(tablet: item.Tablet) if id == GuiType.Tablet.id => mainItem
      case Some(tablet: item.Tablet) if id == GuiType.TabletInner.id => mainItem
      case Some(terminal: item.Terminal) if id == GuiType.Terminal.id => mainItem
      case _ => player.inventory.offHandInventory(0)
    }
  }
}
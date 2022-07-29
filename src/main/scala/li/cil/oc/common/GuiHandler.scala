package li.cil.oc.common

import li.cil.oc.common.inventory.{DatabaseInventory, DiskDriveMountableInventory, ServerInventory}
import li.cil.oc.util.BlockPosition
import li.cil.oc.util.ExtendedWorld._
import li.cil.oc.server.component.{DiskDriveMountable, Server}
import net.minecraft.entity.player.PlayerEntity
import net.minecraft.item.ItemStack
import net.minecraft.util.Hand
import net.minecraft.world.World

@Deprecated
abstract class GuiHandler {
  def getServerGuiElement(id: Int, containerId: Int, player: PlayerEntity, world: World, x: Int, y: Int, z: Int): AnyRef = {
    GuiType.Categories.get(id) match {
      case Some(GuiType.Category.Block) =>
        world.getBlockEntity(BlockPosition(x, GuiType.extractY(y), z)) match {
          case t: tileentity.Adapter if id == GuiType.Adapter.id =>
            new container.Adapter(containerId, player.inventory, t)
          case t: tileentity.Assembler if id == GuiType.Assembler.id =>
            new container.Assembler(containerId, player.inventory, t)
          case t: tileentity.Charger if id == GuiType.Charger.id =>
            new container.Charger(containerId, player.inventory, t)
          case t: tileentity.Case if id == GuiType.Case.id =>
            new container.Case(containerId, player.inventory, t)
          case t: tileentity.Disassembler if id == GuiType.Disassembler.id =>
            new container.Disassembler(containerId, player.inventory, t)
          case t: tileentity.DiskDrive if id == GuiType.DiskDrive.id =>
            new container.DiskDrive(containerId, player.inventory, t)
          case t: tileentity.Printer if id == GuiType.Printer.id =>
            new container.Printer(containerId, player.inventory, t)
          case t: tileentity.Raid if id == GuiType.Raid.id =>
            new container.Raid(containerId, player.inventory, t)
          case t: tileentity.Relay if id == GuiType.Relay.id =>
            new container.Relay(containerId, player.inventory, t)
          case t: tileentity.RobotProxy if id == GuiType.Robot.id =>
            new container.Robot(containerId, player.inventory, t.robot)
          case t: tileentity.Rack if id == GuiType.Rack.id =>
            new container.Rack(containerId, player.inventory, t)
          case t: tileentity.Rack if id == GuiType.ServerInRack.id =>
            val slot = GuiType.extractSlot(y)
            val server = t.getMountable(slot).asInstanceOf[Server]
            new container.Server(containerId, player.inventory, server, Option(server))
          case t: tileentity.Rack if id == GuiType.DiskDriveMountableInRack.id =>
            val slot = GuiType.extractSlot(y)
            val drive = t.getMountable(slot).asInstanceOf[DiskDriveMountable]
            new container.DiskDrive(containerId, player.inventory, drive)
          case _ => null
        }
      case Some(GuiType.Category.Entity) =>
        world.getEntity(x) match {
          case drone: entity.Drone if id == GuiType.Drone.id =>
            new container.Drone(containerId, player.inventory, drone)
          case _ => null
        }
      case Some(GuiType.Category.Item) => {
        val itemStackInUse = getItemStackInUse(id, player)
        itemStackInUse.getItem match {
          case database: item.UpgradeDatabase if id == GuiType.Database.id =>
            new container.Database(containerId, player.inventory, new DatabaseInventory {
              override def container = itemStackInUse

              override def stillValid(player: PlayerEntity) = player == player
            })
          case server: item.Server if id == GuiType.Server.id =>
            new container.Server(containerId, player.inventory, new ServerInventory {
              override def container = itemStackInUse

              override def stillValid(player: PlayerEntity) = player == player
            })
          case tablet: item.Tablet if id == GuiType.TabletInner.id =>
            val stack = itemStackInUse
            if (stack.hasTag)
              new container.Tablet(containerId, player.inventory, item.Tablet.get(stack, player))
            else
              null
          case drive: item.DiskDriveMountable if id == GuiType.DiskDriveMountable.id =>
            new container.DiskDrive(containerId, player.inventory, new DiskDriveMountableInventory {
              override def container: ItemStack = itemStackInUse

              override def stillValid(player: PlayerEntity) = player == player
            })
          case _ => null
        }
      }
      case _ => null
    }
  }

  def getClientGuiElement(id: Int, containerId: Int, player: PlayerEntity, world: World, x: Int, y: Int, z: Int): AnyRef = null

  def getItemStackInUse(id: Int, player: PlayerEntity): ItemStack = {
    val mainItem: ItemStack = player.getItemInHand(Hand.MAIN_HAND)
    mainItem.getItem match {
      case drive: item.traits.FileSystemLike if id == GuiType.Drive.id => mainItem
      case database: item.UpgradeDatabase if id == GuiType.Database.id => mainItem
      case server: item.Server if id == GuiType.Server.id => mainItem
      case tablet: item.Tablet if id == GuiType.Tablet.id => mainItem
      case tablet: item.Tablet if id == GuiType.TabletInner.id => mainItem
      case terminal: item.Terminal if id == GuiType.Terminal.id => mainItem
      case drive: item.DiskDriveMountable if id == GuiType.DiskDriveMountable.id => mainItem
      case _ => player.getItemInHand(Hand.OFF_HAND)
    }
  }
}

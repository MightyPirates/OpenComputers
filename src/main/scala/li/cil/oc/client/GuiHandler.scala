package li.cil.oc.client

import com.google.common.base.Strings
import li.cil.oc.Localization
import li.cil.oc.Settings
import li.cil.oc.api
import li.cil.oc.common.GuiType
import li.cil.oc.common.component
import li.cil.oc.common.entity
import li.cil.oc.common.inventory.{DatabaseInventory, DiskDriveMountableInventory, ServerInventory}
import li.cil.oc.common.item
import li.cil.oc.common.tileentity
import li.cil.oc.common.tileentity.traits.TileEntity
import li.cil.oc.common.{GuiHandler => CommonGuiHandler}
import li.cil.oc.util.BlockPosition
import li.cil.oc.util.ExtendedWorld._
import net.minecraft.client.Minecraft
import net.minecraft.entity.player.PlayerEntity
import net.minecraft.world.World
import net.minecraft.item.ItemStack

@Deprecated
object GuiHandler extends CommonGuiHandler {
  override def getClientGuiElement(id: Int, containerId: Int, player: PlayerEntity, world: World, x: Int, y: Int, z: Int): AnyRef = {
    GuiType.Categories.get(id) match {
      case Some(GuiType.Category.Block) =>
        world.getBlockEntity(BlockPosition(x, GuiType.extractY(y), z)) match {
          case t: tileentity.Adapter if id == GuiType.Adapter.id =>
            new gui.Adapter(containerId, player.inventory, t)
          case t: tileentity.Assembler if id == GuiType.Assembler.id =>
            new gui.Assembler(containerId, player.inventory, t)
          case t: tileentity.Case if id == GuiType.Case.id =>
            new gui.Case(containerId, player.inventory, t)
          case t: tileentity.Charger if id == GuiType.Charger.id =>
            new gui.Charger(containerId, player.inventory, t)
          case t: tileentity.Disassembler if id == GuiType.Disassembler.id =>
            new gui.Disassembler(containerId, player.inventory, t)
          case t: tileentity.DiskDrive if id == GuiType.DiskDrive.id =>
            new gui.DiskDrive(containerId, player.inventory, t)
          case t: tileentity.Printer if id == GuiType.Printer.id =>
            new gui.Printer(containerId, player.inventory, t)
          case t: tileentity.Rack if id == GuiType.Rack.id =>
            new gui.Rack(containerId, player.inventory, t)
          case t: tileentity.Raid if id == GuiType.Raid.id =>
            new gui.Raid(containerId, player.inventory, t)
          case t: tileentity.Relay if id == GuiType.Relay.id =>
            new gui.Relay(containerId, player.inventory, t)
          case t: tileentity.RobotProxy if id == GuiType.Robot.id =>
            new gui.Robot(containerId, player.inventory, t.robot)
          case t: tileentity.Screen if id == GuiType.Screen.id =>
            new gui.Screen(t.origin.buffer, t.tier > 0, () => t.origin.hasKeyboard, () => t.origin.buffer.isRenderingEnabled)
          case t: tileentity.Rack if id == GuiType.ServerInRack.id =>
            val slot = GuiType.extractSlot(y)
            new gui.Server(containerId, player.inventory, new ServerInventory {
              override def container = t.getItem(slot)

              override def stillValid(player: PlayerEntity) = t.stillValid(player)
            }, Option(t), slot)
          case t: tileentity.Rack if id == GuiType.DiskDriveMountableInRack.id =>
            val slot = GuiType.extractSlot(y)
            new gui.DiskDrive(containerId, player.inventory, new DiskDriveMountableInventory {
              override def container: ItemStack = t.getItem(slot)

              override def stillValid(player: PlayerEntity): Boolean = t.stillValid(player)
            })
          case t: tileentity.Waypoint if id == GuiType.Waypoint.id =>
            new gui.Waypoint(t)
          case _ => null
        }
      case Some(GuiType.Category.Entity) =>
        world.getEntity(x) match {
          case drone: entity.Drone if id == GuiType.Drone.id =>
            new gui.Drone(containerId, player.inventory, drone)
          case _ => null
        }
      case Some(GuiType.Category.Item) => {
        val itemStackInUse = getItemStackInUse(id, player)
        itemStackInUse.getItem match {
          case drive: item.traits.FileSystemLike if id == GuiType.Drive.id =>
            new gui.Drive(player.inventory, () => itemStackInUse)
          case database: item.UpgradeDatabase if id == GuiType.Database.id =>
            new gui.Database(containerId, player.inventory, new DatabaseInventory {
              override def container = itemStackInUse

              override def stillValid(player: PlayerEntity) = player == player
            })
          case server: item.Server if id == GuiType.Server.id =>
            new gui.Server(containerId, player.inventory, new ServerInventory {
              override def container = itemStackInUse

              override def stillValid(player: PlayerEntity) = player == player
            })
          case tablet: item.Tablet if id == GuiType.Tablet.id =>
            val stack = itemStackInUse
            if (stack.hasTag) {
              item.Tablet.get(stack, player).components.collect {
                case Some(buffer: api.internal.TextBuffer) => buffer
              }.headOption match {
                case Some(buffer: api.internal.TextBuffer) => new gui.Screen(buffer, true, () => true, () => buffer.isRenderingEnabled)
                case _ => null
              }
            }
            else null
          case tablet: item.Tablet if id == GuiType.TabletInner.id =>
            val stack = itemStackInUse
            if (stack.hasTag) {
              new gui.Tablet(containerId, player.inventory, item.Tablet.get(stack, player))
            }
            else null
          case _: item.DiskDriveMountable if id == GuiType.DiskDriveMountable.id =>
            new gui.DiskDrive(containerId, player.inventory, new DiskDriveMountableInventory {
              override def container = itemStackInUse
              override def stillValid(activePlayer : PlayerEntity): Boolean = activePlayer == player
            })
          case terminal: item.Terminal if id == GuiType.Terminal.id =>
            val stack = itemStackInUse
            if (stack.hasTag) {
              val address = stack.getTag.getString(Settings.namespace + "server")
              val key = stack.getTag.getString(Settings.namespace + "key")
              if (!Strings.isNullOrEmpty(key) && !Strings.isNullOrEmpty(address)) {
                component.TerminalServer.loaded.find(address) match {
                  case Some(term) if term != null && term.rack != null => term.rack match {
                    case rack: TileEntity with api.internal.Rack =>
                      def inRange = player.isAlive && !rack.isRemoved && player.distanceToSqr(rack.x + 0.5, rack.y + 0.5, rack.z + 0.5) < term.range * term.range
                    if (inRange) {
                        if (term.sidedKeys.contains(key)) return new gui.Screen(term.buffer, true, () => true, () => {
                        // Check if someone else bound a term to our server.
                        if (stack.getTag.getString(Settings.namespace + "key") != key) Minecraft.getInstance.popGuiLayer
                        // Check whether we're still in range.
                        if (!inRange) Minecraft.getInstance.popGuiLayer
                        true
                      })
                      else player.displayClientMessage(Localization.Terminal.InvalidKey, true)
                    }
                    else player.displayClientMessage(Localization.Terminal.OutOfRange, true)
                    case _ => // Eh?
                  }
                  case _ => player.displayClientMessage(Localization.Terminal.OutOfRange, true)
                }
              }
            }
            null
          case _ => null
        }
      }
      case Some(GuiType.Category.None) =>
        if (id == GuiType.Manual.id) new gui.Manual()
        else null
      case _ => null
    }
  }
}

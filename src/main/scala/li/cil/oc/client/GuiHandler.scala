package li.cil.oc.client

import com.google.common.base.Strings
import li.cil.oc.Localization
import li.cil.oc.Settings
import li.cil.oc.api
import li.cil.oc.common.GuiType
import li.cil.oc.common.component
import li.cil.oc.common.entity
import li.cil.oc.common.inventory.DatabaseInventory
import li.cil.oc.common.inventory.ServerInventory
import li.cil.oc.common.item
import li.cil.oc.common.item.Delegator
import li.cil.oc.common.tileentity
import li.cil.oc.common.{GuiHandler => CommonGuiHandler}
import net.minecraft.client.Minecraft
import net.minecraft.entity.player.EntityPlayer
import net.minecraft.world.World

object GuiHandler extends CommonGuiHandler {
  override def getClientGuiElement(id: Int, player: EntityPlayer, world: World, x: Int, y: Int, z: Int): AnyRef = {
    GuiType.Categories.get(id) match {
      case Some(GuiType.Category.Block) =>
        world.getTileEntity(x, GuiType.extractY(y), z) match {
          case t: tileentity.Adapter if id == GuiType.Adapter.id =>
            new gui.Adapter(player.inventory, t)
          case t: tileentity.Assembler if id == GuiType.Assembler.id =>
            new gui.Assembler(player.inventory, t)
          case t: tileentity.Case if id == GuiType.Case.id =>
            new gui.Case(player.inventory, t)
          case t: tileentity.Charger if id == GuiType.Charger.id =>
            new gui.Charger(player.inventory, t)
          case t: tileentity.Disassembler if id == GuiType.Disassembler.id =>
            new gui.Disassembler(player.inventory, t)
          case t: tileentity.DiskDrive if id == GuiType.DiskDrive.id =>
            new gui.DiskDrive(player.inventory, t)
          case t: tileentity.Printer if id == GuiType.Printer.id =>
            new gui.Printer(player.inventory, t)
          case t: tileentity.Rack if id == GuiType.Rack.id =>
            new gui.Rack(player.inventory, t)
          case t: tileentity.Raid if id == GuiType.Raid.id =>
            new gui.Raid(player.inventory, t)
          case t: tileentity.Relay if id == GuiType.Relay.id =>
            new gui.Relay(player.inventory, t)
          case t: tileentity.RobotProxy if id == GuiType.Robot.id =>
            new gui.Robot(player.inventory, t.robot)
          case t: tileentity.Screen if id == GuiType.Screen.id =>
            new gui.Screen(t.origin.buffer, t.tier > 0, () => t.origin.hasKeyboard, () => t.origin.buffer.isRenderingEnabled)
          case t: tileentity.Rack if id == GuiType.ServerInRack.id =>
            val slot = GuiType.extractSlot(y)
            new gui.Server(player.inventory, new ServerInventory {
              override def container = t.getStackInSlot(slot)

              override def isUseableByPlayer(player: EntityPlayer) = t.isUseableByPlayer(player)
            }, Option(t), slot)
          case t: tileentity.Switch if id == GuiType.Switch.id =>
            new gui.Switch(player.inventory, t)
          case t: tileentity.Waypoint if id == GuiType.Waypoint.id =>
            new gui.Waypoint(t)
          case _ => null
        }
      case Some(GuiType.Category.Entity) =>
        world.getEntityByID(x) match {
          case drone: entity.Drone if id == GuiType.Drone.id =>
            new gui.Drone(player.inventory, drone)
          case _ => null
        }
      case Some(GuiType.Category.Item) =>
        Delegator.subItem(player.getHeldItem) match {
          case Some(drive: item.traits.FileSystemLike) if id == GuiType.Drive.id =>
            new gui.Drive(player.inventory, () => player.getHeldItem)
          case Some(database: item.UpgradeDatabase) if id == GuiType.Database.id =>
            new gui.Database(player.inventory, new DatabaseInventory {
              override def container = player.getHeldItem

              override def isUseableByPlayer(player: EntityPlayer) = player == player
            })
          case Some(server: item.Server) if id == GuiType.Server.id =>
            new gui.Server(player.inventory, new ServerInventory {
              override def container = player.getHeldItem

              override def isUseableByPlayer(player: EntityPlayer) = player == player
            })
          case Some(tablet: item.Tablet) if id == GuiType.Tablet.id =>
            val stack = player.getHeldItem
            if (stack.hasTagCompound) {
              item.Tablet.get(stack, player).components.collect {
                case Some(buffer: api.internal.TextBuffer) => buffer
              }.headOption match {
                case Some(buffer: api.internal.TextBuffer) => new gui.Screen(buffer, true, () => true, () => true)
                case _ => null
              }
            }
            else null
          case Some(tablet: item.Tablet) if id == GuiType.TabletInner.id =>
            val stack = player.getHeldItem
            if (stack.hasTagCompound) {
              new gui.Tablet(player.inventory, item.Tablet.get(stack, player))
            }
            else null
          case Some(terminal: item.Terminal) if id == GuiType.Terminal.id =>
            val stack = player.getHeldItem
            if (stack.hasTagCompound) {
              val address = stack.getTagCompound.getString(Settings.namespace + "server")
              val key = stack.getTagCompound.getString(Settings.namespace + "key")
              if (!Strings.isNullOrEmpty(key) && !Strings.isNullOrEmpty(address)) {
                component.TerminalServer.loaded.find(_.address == address) match {
                  case Some(term) =>
                    def inRange = player.isEntityAlive && !term.rack.isInvalid && term.rack.getDistanceFrom(player.posX, player.posY, player.posZ) < term.range * term.range
                    if (inRange) {
                      if (term.sidedKeys.contains(key)) return new gui.Screen(term.buffer, true, () => true, () => {
                        // Check if someone else bound a term to our server.
                        if (stack.getTagCompound.getString(Settings.namespace + "key") != key) {
                          Minecraft.getMinecraft.displayGuiScreen(null)
                        }
                        // Check whether we're still in range.
                        if (!inRange) {
                          Minecraft.getMinecraft.displayGuiScreen(null)
                        }
                        true
                      })
                      else player.addChatMessage(Localization.Terminal.InvalidKey)
                    }
                    else player.addChatMessage(Localization.Terminal.OutOfRange)
                  case _ => player.addChatMessage(Localization.Terminal.OutOfRange)
                }
              }
            }
            null
          case _ => null
        }
      case Some(GuiType.Category.None) =>
        if (id == GuiType.Manual.id) new gui.Manual()
        else null
      case _ => null
    }
  }
}

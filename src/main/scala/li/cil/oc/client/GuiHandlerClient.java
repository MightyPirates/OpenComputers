package li.cil.oc.client;

import li.cil.oc.client.gui.GuiAssembler;
import li.cil.oc.client.gui.GuiCase;
import li.cil.oc.client.gui.GuiCharger;
import li.cil.oc.client.gui.GuiRelay;
import li.cil.oc.client.gui.GuiWaypoint;
import li.cil.oc.common.AbstractGuiHandler;
import li.cil.oc.common.GuiType;
import li.cil.oc.common.tileentity.TileEntityAssembler;
import li.cil.oc.common.tileentity.TileEntityCase;
import li.cil.oc.common.tileentity.TileEntityCharger;
import li.cil.oc.common.tileentity.TileEntityNetworkBridge;
import li.cil.oc.common.tileentity.TileEntityWaypoint;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;

import javax.annotation.Nullable;

public final class GuiHandlerClient extends AbstractGuiHandler {
    @Nullable
    @Override
    public Object getClientGuiElement(final int id, final EntityPlayer player, final World world, final int x, final int y, final int z) {
        final GuiType guiType = GuiType.VALUES[id];
        switch (guiType.category) {
            case BLOCK: {
                final TileEntity tileEntity = world.getTileEntity(new BlockPos(x, y, z));
                switch (guiType) {
                    case Assembler:
                        if (tileEntity instanceof TileEntityAssembler) {
                            return new GuiAssembler(player.inventory, (TileEntityAssembler) tileEntity);
                        }
                        break;
                    case Case:
                        if (tileEntity instanceof TileEntityCase) {
                            return new GuiCase(player.inventory, (TileEntityCase) tileEntity);
                        }
                        break;
                    case Charger:
                        if (tileEntity instanceof TileEntityCharger) {
                            return new GuiCharger(player.inventory, (TileEntityCharger) tileEntity);
                        }
                        break;
                    case Printer:
//                        if (tileEntity instanceof TileEntityPrinter) {
//                            return new GuiPrinter(player.inventory, (TileEntityPrinter) tileEntity);
//                        }
                        break;
                    case Rack:
//                        if (tileEntity instanceof TileEntityRack) {
//                            return new GuiRack(player.inventory, (TileEntityRack) tileEntity);
//                        }
                        break;
                    case Raid:
//                        if (tileEntity instanceof TileEntityRaid) {
//                            return new GuiRaid(player.inventory, (TileEntityRaid) tileEntity);
//                        }
                        break;
                    case Relay:
                        if (tileEntity instanceof TileEntityNetworkBridge) {
                            return new GuiRelay(player.inventory, (TileEntityNetworkBridge) tileEntity);
                        }
                        break;
                    case Robot:
//                        if (tileEntity instanceof TileEntityRobot) {
//                            return new GuiRobot(player.inventory, (TileEntityRobot) tileEntity);
//                        }
                        break;
                    case Screen:
//            new gui.Screen(t.origin.buffer, t.tier > 0, () => t.origin.hasKeyboard, () => t.origin.buffer.isRenderingEnabled)
//                        if (tileEntity instanceof TileEntityScreen) {
//                            return new GuiScreen(player.inventory, (TileEntityScreen) tileEntity);
//                        }
                        break;
                    case ServerInRack:
//            val slot = GuiType.extractSlot(y)
//            new gui.Server(player.inventory, new ServerInventory {
//              override def container = t.getStackInSlot(slot)
//
//              override def isUsableByPlayer(player: EntityPlayer) = t.isUsableByPlayer(player)
//            }, Option(t), slot)

//                        if (tileEntity instanceof TileEntityRack) {
//                            return new GuiServer(player.inventory, (TileEntityRack) tileEntity);
//                        }
                        break;
                    case Waypoint:
                        if (tileEntity instanceof TileEntityWaypoint) {
                            return new GuiWaypoint((TileEntityWaypoint) tileEntity);
                        }
                        break;
                }
                break;
            }
            case ENTITY:
                break;
            case ITEM:
                break;
        }
        return null;
    }

//      case Some(GuiType.Category.Entity) =>
//        world.getEntityByID(x) match {
//          case drone: entity.Drone if id == GuiType.Drone.id =>
//            new gui.Drone(player.inventory, drone)
//          case _ => null
//        }
//      case Some(GuiType.Category.Item) =>
//        Delegator.subItem(player.getHeldItemMainhand) match {
//          case Some(drive: item.traits.FileSystemLike) if id == GuiType.Drive.id =>
//            new gui.Drive(player.inventory, () => player.getHeldItemMainhand)
//          case Some(database: item.UpgradeDatabase) if id == GuiType.Database.id =>
//            new gui.Database(player.inventory, new DatabaseInventory {
//              override def container = player.getHeldItemMainhand
//
//              override def isUsableByPlayer(player: EntityPlayer) = player == player
//            })
//          case Some(server: item.Server) if id == GuiType.Server.id =>
//            new gui.Server(player.inventory, new ServerInventory {
//              override def container = player.getHeldItemMainhand
//
//              override def isUsableByPlayer(player: EntityPlayer) = player == player
//            })
//          case Some(tablet: item.Tablet) if id == GuiType.Tablet.id =>
//            val stack = player.getHeldItemMainhand
//            if (stack.hasTagCompound) {
//              item.Tablet.get(stack, player).components.collect {
//                case Some(buffer: api.internal.TextBuffer) => buffer
//              }.headOption match {
//                case Some(buffer: api.internal.TextBuffer) => new gui.Screen(buffer, true, () => true, () => true)
//                case _ => null
//              }
//            }
//            else null
//          case Some(tablet: item.Tablet) if id == GuiType.TabletInner.id =>
//            val stack = player.getHeldItemMainhand
//            if (stack.hasTagCompound) {
//              new gui.Tablet(player.inventory, item.Tablet.get(stack, player))
//            }
//            else null
//          case Some(terminal: item.Terminal) if id == GuiType.Terminal.id =>
//            val stack = player.getHeldItemMainhand
//            if (stack.hasTagCompound) {
//              val address = stack.getTagCompound.getString(Constants.namespace + "server")
//              val key = stack.getTagCompound.getString(Constants.namespace + "key")
//              if (!Strings.isNullOrEmpty(key) && !Strings.isNullOrEmpty(address)) {
//                component.TerminalServer.loaded.find(_.address == address) match {
//                  case Some(term) => term.rack match {
//                    case rack: TileEntity with api.internal.Rack =>
//                      def inRange = player.isEntityAlive && !rack.isInvalid && rack.getDistanceSq(player.posX, player.posY, player.posZ) < term.range * term.range
//                    if (inRange) {
//                        if (term.sidedKeys.contains(key)) return new gui.Screen(term.buffer, true, () => true, () => {
//                        // Check if someone else bound a term to our server.
//                        if (stack.getTagCompound.getString(Constants.namespace + "key") != key) {
//                          Minecraft.getMinecraft.displayGuiScreen(null)
//                        }
//                        // Check whether we're still in range.
//                        if (!inRange) {
//                          Minecraft.getMinecraft.displayGuiScreen(null)
//                        }
//                        true
//                      })
//                      else player.sendMessage(Localization.Terminal.InvalidKey)
//                    }
//                    else player.sendMessage(Localization.Terminal.OutOfRange)
//                    case _ => // Eh?
//                  }
//                  case _ => player.sendMessage(Localization.Terminal.OutOfRange)
//                }
//              }
//            }
//            null
//          case _ => null
//        }
//      case Some(GuiType.Category.None) =>
//        if (id == GuiType.Manual.id) new gui.Manual()
//        else null
//      case _ => null
//    }
//  }
}

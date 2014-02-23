package li.cil.oc.server

import cpw.mods.fml.common.eventhandler.SubscribeEvent
import cpw.mods.fml.common.network.FMLNetworkEvent.ServerCustomPacketEvent
import li.cil.oc.common.PacketType
import li.cil.oc.common.tileentity._
import li.cil.oc.common.{PacketHandler => CommonPacketHandler}
import li.cil.oc.server.component.machine.Machine
import li.cil.oc.Settings
import net.minecraft.entity.player.{EntityPlayer, EntityPlayerMP}
import net.minecraft.network.NetHandlerPlayServer
import net.minecraft.util.ChatComponentTranslation
import net.minecraftforge.common.DimensionManager
import net.minecraftforge.common.util.ForgeDirection

object PacketHandler extends CommonPacketHandler {
  override protected def world(player: EntityPlayer, dimension: Int) =
    Option(DimensionManager.getWorld(dimension))

  @SubscribeEvent
  def onPacket(e: ServerCustomPacketEvent) {
    val p = new PacketParser(e.packet.payload, e.handler.asInstanceOf[NetHandlerPlayServer].playerEntity)
    p.packetType match {
      case PacketType.ComputerPower => onComputerPower(p)
      case PacketType.KeyDown => onKeyDown(p)
      case PacketType.KeyUp => onKeyUp(p)
      case PacketType.Clipboard => onClipboard(p)
      case PacketType.MouseClickOrDrag => onMouseClick(p)
      case PacketType.MouseScroll => onMouseScroll(p)
      case PacketType.RobotStateRequest => onRobotStateRequest(p)
      case PacketType.ServerRange => onServerRange(p)
      case PacketType.ServerSide => onServerSide(p)
      case _ => // Invalid packet.
    }
  }

  def onComputerPower(p: PacketParser) =
    p.readTileEntity[TileEntity]() match {
      case Some(t: Computer) => p.player match {
        case player: EntityPlayerMP => trySetComputerPower(t.computer, p.readBoolean(), player)
        case _ =>
      }
      case Some(r: Rack) => r.servers(p.readInt()) match {
        case Some(server) => p.player match {
          case player: EntityPlayerMP => trySetComputerPower(server.machine, p.readBoolean(), player)
          case _ =>
        }
        case _ => // Invalid packet.
      }
      case _ => // Invalid packet.
    }

  private def trySetComputerPower(computer: Machine, value: Boolean, player: EntityPlayerMP) {
    if (computer.canInteract(player.getCommandSenderName)) {
      if (value) {
        if (!computer.isPaused) {
          computer.start()
          computer.lastError match {
            case Some(message) => player.addChatMessage(new ChatComponentTranslation(message))
            case _ =>
          }
        }
      }
      else computer.stop()
    }
  }

  def onKeyDown(p: PacketParser) =
    p.readTileEntity[TileEntity]() match {
      case Some(t: Screen) =>
        val char = Char.box(p.readChar())
        val code = Int.box(p.readInt())
        t.screens.foreach(_.node.sendToNeighbors("keyboard.keyDown", p.player, char, code))
      case Some(t: Buffer) => t.buffer.node.sendToNeighbors("keyboard.keyDown", p.player, Char.box(p.readChar()), Int.box(p.readInt()))
      case Some(t: Rack) => t.terminals(p.readInt()).buffer.node.sendToNeighbors("keyboard.keyDown", p.player, Char.box(p.readChar()), Int.box(p.readInt()))
      case _ => // Invalid packet.
    }

  def onKeyUp(p: PacketParser) =
    p.readTileEntity[TileEntity]() match {
      case Some(t: Screen) =>
        val char = Char.box(p.readChar())
        val code = Int.box(p.readInt())
        t.screens.foreach(_.node.sendToNeighbors("keyboard.keyUp", p.player, char, code))
      case Some(t: Buffer) => t.buffer.node.sendToNeighbors("keyboard.keyUp", p.player, Char.box(p.readChar()), Int.box(p.readInt()))
      case Some(t: Rack) => t.terminals(p.readInt()).buffer.node.sendToNeighbors("keyboard.keyUp", p.player, Char.box(p.readChar()), Int.box(p.readInt()))
      case _ => // Invalid packet.
    }

  def onClipboard(p: PacketParser) =
    p.readTileEntity[TileEntity]() match {
      case Some(t: Screen) =>
        val value = p.readUTF()
        t.screens.foreach(_.node.sendToNeighbors("keyboard.clipboard", p.player, value))
      case Some(t: Buffer) => t.buffer.node.sendToNeighbors("keyboard.clipboard", p.player, p.readUTF())
      case Some(t: Rack) => t.terminals(p.readInt()).buffer.node.sendToNeighbors("keyboard.clipboard", p.player, p.readUTF())
      case _ => // Invalid packet.
    }

  def onMouseClick(p: PacketParser) {
    p.player match {
      case player: EntityPlayerMP =>
        val node = p.readTileEntity[TileEntity]() match {
          case Some(t: Screen) => t.origin.node
          case Some(t: Rack) => t.terminals(p.readInt()).buffer.node
          case _ => return // Invalid packet.
        }
        val x = p.readInt()
        val y = p.readInt()
        val what = if (p.readBoolean()) "drag" else "touch"
        val button = p.readByte()
        if (Settings.get.inputUsername) {
          node.sendToReachable("computer.checked_signal", player, what, Int.box(x), Int.box(y), Int.box(button), player.getCommandSenderName)
        }
        else {
          node.sendToReachable("computer.checked_signal", player, what, Int.box(x), Int.box(y), Int.box(button))
        }
      case _ => // Invalid packet.
    }
  }

  def onMouseScroll(p: PacketParser) {
    p.player match {
      case player: EntityPlayerMP =>
        val node = p.readTileEntity[TileEntity]() match {
          case Some(t: Screen) => t.origin.node
          case Some(t: Rack) => t.terminals(p.readInt()).buffer.node
          case _ => return // Invalid packet.
        }
        val x = p.readInt()
        val y = p.readInt()
        val scroll = p.readByte()
        if (Settings.get.inputUsername) {
          node.sendToReachable("computer.checked_signal", player, "scroll", Int.box(x), Int.box(y), Int.box(scroll), player.getCommandSenderName)
        }
        else {
          node.sendToReachable("computer.checked_signal", player, "scroll", Int.box(x), Int.box(y), Int.box(scroll))
        }
      case _ => // Invalid packet.
    }
  }

  def onRobotStateRequest(p: PacketParser) =
    p.readTileEntity[RobotProxy]() match {
      case Some(proxy) => proxy.world.markBlockForUpdate(proxy.x, proxy.y, proxy.z)
      case _ => // Invalid packet.
    }

  def onServerRange(p: PacketParser) =
    p.readTileEntity[Rack]() match {
      case Some(rack) => p.player match {
        case player: EntityPlayerMP if rack.isUseableByPlayer(player) =>
          rack.range = math.min(math.max(0, p.readInt()), Settings.get.maxWirelessRange).toInt
          PacketSender.sendServerState(rack)
        case _ =>
      }
      case _ => // Invalid packet.
    }

  def onServerSide(p: PacketParser) =
    p.readTileEntity[Rack]() match {
      case Some(rack) => p.player match {
        case player: EntityPlayerMP if rack.isUseableByPlayer(player) =>
          val number = p.readInt()
          val side = p.readDirection()
          if (rack.sides(number) != side && side != ForgeDirection.SOUTH && (!rack.sides.contains(side) || side == ForgeDirection.UNKNOWN)) {
            rack.sides(number) = side
            rack.servers(number) match {
              case Some(server) => rack.reconnectServer(number, server)
              case _ =>
            }
            PacketSender.sendServerState(rack, number)
          }
          else PacketSender.sendServerState(rack, number, Some(player))
        case _ =>
      }
      case _ => // Invalid packet.
    }
}
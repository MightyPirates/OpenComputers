package li.cil.oc.server

import cpw.mods.fml.common.network.Player
import li.cil.oc.common.PacketType
import li.cil.oc.common.tileentity._
import li.cil.oc.common.{PacketHandler => CommonPacketHandler}
import net.minecraft.entity.player.EntityPlayer
import net.minecraftforge.common.{ForgeDirection, DimensionManager}
import scala.Some

class PacketHandler extends CommonPacketHandler {
  protected def world(player: Player, dimension: Int) =
    Option(DimensionManager.getWorld(dimension))

  def dispatch(p: PacketParser) =
    p.packetType match {
      case PacketType.ComputerPower => onComputerPower(p)
      case PacketType.KeyDown => onKeyDown(p)
      case PacketType.KeyUp => onKeyUp(p)
      case PacketType.Clipboard => onClipboard(p)
      case PacketType.MouseClickOrDrag => onMouseClick(p)
      case PacketType.MouseScroll => onMouseScroll(p)
      case PacketType.ServerSide => onServerSide(p)
      case _ => // Invalid packet.
    }

  def onComputerPower(p: PacketParser) =
    p.readTileEntity[TileEntity]() match {
      case Some(t: Computer) => p.player match {
        case player: EntityPlayer => trySetComputerPower(t.computer, p.readBoolean(), player)
        case _ =>
      }
      case Some(r: Rack) => r.servers(p.readInt()) match {
        case Some(server) => p.player match {
          case player: EntityPlayer => trySetComputerPower(server.machine, p.readBoolean(), player)
          case _ =>
        }
        case _ => // Invalid packet.
      }
      case _ => // Invalid packet.
    }

  private def trySetComputerPower(computer: component.Machine, value: Boolean, player: EntityPlayer) {
    if (computer.canInteract(player.getCommandSenderName)) {
      if (value) {
        if (!computer.isPaused) {
          computer.start()
          computer.lastError match {
            case Some(message) => player.addChatMessage(message)
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
      case player: EntityPlayer =>
        val node = p.readTileEntity[TileEntity]() match {
          case Some(t: Screen) => t.origin.node
          case Some(t: Rack) => t.terminals(p.readInt()).buffer.node
          case _ => return // Invalid packet.
        }
        val x = p.readInt()
        val y = p.readInt()
        val what = if (p.readBoolean()) "drag" else "touch"
        node.sendToReachable("computer.checked_signal", player, what, Int.box(x), Int.box(y), player.getCommandSenderName)
      case _ => // Invalid packet.
    }
  }

  def onMouseScroll(p: PacketParser) {
    p.player match {
      case player: EntityPlayer =>
        val node = p.readTileEntity[TileEntity]() match {
          case Some(t: Screen) => t.origin.node
          case Some(t: Rack) => t.terminals(p.readInt()).buffer.node
          case _ => return // Invalid packet.
        }
        val x = p.readInt()
        val y = p.readInt()
        val scroll = p.readByte()
        node.sendToReachable("computer.checked_signal", player, "scroll", Int.box(x), Int.box(y), Int.box(scroll), player.getCommandSenderName)
      case _ => // Invalid packet.
    }
  }

  def onServerSide(p: PacketParser) =
    p.readTileEntity[Rack]() match {
      case Some(rack) => p.player match {
        case player: EntityPlayer if rack.isUseableByPlayer(player) =>
          val number = p.readInt()
          val side = p.readDirection()
          if (rack.sides(number) != side && side != ForgeDirection.SOUTH) {
            rack.sides(number) = side
            rack.servers(number) match {
              case Some(server) => rack.reconnectServer(number, server)
              case _ =>
            }
          }
          PacketSender.sendServerState(rack, number)
        case _ =>
      }
      case _ => // Invalid packet.
    }
}
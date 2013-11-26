package li.cil.oc.server

import cpw.mods.fml.common.network.Player
import li.cil.oc.common.PacketType
import li.cil.oc.common.tileentity._
import li.cil.oc.common.{PacketHandler => CommonPacketHandler}
import net.minecraftforge.common.DimensionManager
import scala.Some

class PacketHandler extends CommonPacketHandler {
  protected def world(player: Player, dimension: Int) =
    Option(DimensionManager.getWorld(dimension))

  def dispatch(p: PacketParser) =
    p.packetType match {
      case PacketType.ComputerStateRequest => onComputerStateRequest(p)
      case PacketType.PowerStateRequest => onPowerStateRequest(p)
      case PacketType.RedstoneStateRequest => onRedstoneStateRequest(p)
      case PacketType.RobotStateRequest => onRobotStateRequest(p)
      case PacketType.RotatableStateRequest => onRotatableStateRequest(p)
      case PacketType.ScreenBufferRequest => onScreenBufferRequest(p)
      case PacketType.KeyDown => onKeyDown(p)
      case PacketType.KeyUp => onKeyUp(p)
      case PacketType.Clipboard => onClipboard(p)
      case _ => // Invalid packet.
    }

  def onComputerStateRequest(p: PacketParser) =
    p.readTileEntity[Computer]() match {
      case Some(t) => PacketSender.sendComputerState(t, t.isRunning, Option(p.player))
      case _ => // Invalid packet.
    }

  def onPowerStateRequest(p: PacketParser) =
    p.readTileEntity[PowerInformation]() match {
      case Some(t) => PacketSender.sendPowerState(t, Option(p.player))
      case _ => // Invalid packet.
    }

  def onRedstoneStateRequest(p: PacketParser) =
    p.readTileEntity[Redstone]() match {
      case Some(t) => PacketSender.sendRedstoneState(t, Option(p.player))
      case _ => // Invalid packet.
    }

  def onRobotStateRequest(p: PacketParser) =
    p.readTileEntity[RobotProxy]() match {
      case Some(t) => PacketSender.sendRobotState(t.robot, Option(p.player))
      case _ => // Invalid packet.
    }

  def onRotatableStateRequest(p: PacketParser) =
    p.readTileEntity[Rotatable]() match {
      case Some(t) => PacketSender.sendRotatableState(t, Option(p.player))
      case _ => // Invalid packet.
    }

  def onScreenBufferRequest(p: PacketParser) =
    p.readTileEntity[Buffer]() match {
      case Some(t) => PacketSender.sendScreenBufferState(t, Option(p.player))
      case _ => // Invalid packet.
    }

  def onKeyDown(p: PacketParser) =
    p.readTileEntity[Buffer]() match {
      case Some(s: Screen) =>
        val char = Char.box(p.readChar())
        val code = Int.box(p.readInt())
        s.screens.foreach(_.node.sendToNeighbors("keyboard.keyDown", p.player, char, code))
      case Some(e) => e.buffer.node.sendToNeighbors("keyboard.keyDown", p.player, Char.box(p.readChar()), Int.box(p.readInt()))
      case _ => // Invalid packet.
    }

  def onKeyUp(p: PacketParser) =
    p.readTileEntity[Buffer]() match {
      case Some(s: Screen) =>
        val char = Char.box(p.readChar())
        val code = Int.box(p.readInt())
        s.screens.foreach(_.node.sendToNeighbors("keyboard.keyUp", p.player, char, code))
      case Some(e) => e.buffer.node.sendToNeighbors("keyboard.keyUp", p.player, Char.box(p.readChar()), Int.box(p.readInt()))
      case _ => // Invalid packet.
    }

  def onClipboard(p: PacketParser) =
    p.readTileEntity[Buffer]() match {
      case Some(s: Screen) =>
        val value = p.readUTF()
        s.screens.foreach(_.node.sendToNeighbors("keyboard.clipboard", p.player, value))
      case Some(e) => e.buffer.node.sendToNeighbors("keyboard.clipboard", p.player, p.readUTF())
      case _ => // Invalid packet.
    }
}
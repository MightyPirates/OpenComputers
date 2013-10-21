package li.cil.oc.server

import cpw.mods.fml.common.network.Player
import li.cil.oc.api.network.Node
import li.cil.oc.common.PacketType
import li.cil.oc.common.tileentity
import li.cil.oc.common.{PacketHandler => CommonPacketHandler}
import li.cil.oc.server.component.Redstone
import net.minecraft.tileentity.TileEntity
import net.minecraftforge.common.DimensionManager

class PacketHandler extends CommonPacketHandler {
  protected def world(player: Player, dimension: Int) =
    Option(DimensionManager.getWorld(dimension))

  def dispatch(p: PacketParser) =
    p.packetType match {
      case PacketType.ScreenBufferRequest => onScreenBufferRequest(p)
      case PacketType.ComputerStateRequest => onComputerStateRequest(p)
      case PacketType.RotatableStateRequest => onRotatableStateRequest(p)
      case PacketType.RedstoneStateRequest => onRedstoneStateRequest(p)
      case PacketType.KeyDown => onKeyDown(p)
      case PacketType.KeyUp => onKeyUp(p)
      case PacketType.Clipboard => onClipboard(p)
      case _ => // Invalid packet.
    }

  def onScreenBufferRequest(p: PacketParser) =
    p.readTileEntity[tileentity.Screen]() match {
      case Some(t) =>
        val (w, h) = t.instance.resolution
        PacketSender.sendScreenBufferState(t, w, h, t.instance.text, Option(p.player))
      case _ => // Invalid packet.
    }

  def onComputerStateRequest(p: PacketParser) =
    p.readTileEntity[tileentity.Computer]() match {
      case Some(t) => PacketSender.sendComputerState(t, t.isOn, Option(p.player))
      case _ => // Invalid packet.
    }

  def onRotatableStateRequest(p: PacketParser) =
    p.readTileEntity[tileentity.Rotatable]() match {
      case Some(t) => PacketSender.sendRotatableState(t, Option(p.player))
      case _ => // Invalid packet.
    }

  def onRedstoneStateRequest(p: PacketParser) =
    p.readTileEntity[TileEntity with Redstone]() match {
      case Some(t) => PacketSender.sendRedstoneState(t, Option(p.player))
      case _ => // Invalid packet.
    }

  def onKeyDown(p: PacketParser) =
    p.readTileEntity[Node]() match {
      case Some(s: tileentity.Screen) =>
        val char = p.readChar()
        val code = p.readInt()
        val network = s.origin.network
        s.screens.foreach(n => network.foreach(_.sendToNeighbors(n, "keyboard.keyDown", p.player, char, code)))
      case Some(n) => n.network.foreach(_.sendToNeighbors(n, "keyboard.keyDown", p.player, p.readChar(), p.readInt()))
      case _ => // Invalid packet.
    }

  def onKeyUp(p: PacketParser) =
    p.readTileEntity[Node]() match {
      case Some(s: tileentity.Screen) =>
        val char = p.readChar()
        val code = p.readInt()
        val network = s.origin.network
        s.screens.foreach(n => network.foreach(_.sendToNeighbors(n, "keyboard.keyUp", p.player, char, code)))
      case Some(n) => n.network.foreach(_.sendToNeighbors(n, "keyboard.keyUp", p.player, p.readChar(), p.readInt()))
      case _ => // Invalid packet.
    }

  def onClipboard(p: PacketParser) =
    p.readTileEntity[Node]() match {
      case Some(s: tileentity.Screen) =>
        val value = p.readUTF()
        val network = s.origin.network
        s.screens.foreach(n => network.foreach(_.sendToNeighbors(n, "keyboard.clipboard", p.player, value)))
      case Some(n) => n.network.foreach(_.sendToNeighbors(n, "keyboard.clipboard", p.player, p.readUTF()))
      case _ => // Invalid packet.
    }
}
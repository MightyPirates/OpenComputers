package li.cil.oc.server

import cpw.mods.fml.common.network.Player
import li.cil.oc.api.network.Node
import li.cil.oc.common.PacketBuilder
import li.cil.oc.common.PacketType
import li.cil.oc.common.tileentity.Computer
import li.cil.oc.common.tileentity.Rotatable
import li.cil.oc.common.tileentity.Screen
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
    p.readTileEntity[Screen]() match {
      case None => // Invalid packet.
      case Some(t) => {
        val pb = new PacketBuilder(PacketType.ScreenBufferResponse)

        pb.writeTileEntity(t)
        pb.writeUTF(t.screen.text)

        pb.sendToPlayer(p.player)
      }
    }

  def onComputerStateRequest(p: PacketParser) =
    p.readTileEntity[Computer]() match {
      case None => // Invalid packet.
      case Some(t) => PacketSender.sendComputerState(t, t.isOn)
    }

  def onRotatableStateRequest(p: PacketParser) =
    p.readTileEntity[Rotatable]() match {
      case None => // Invalid packet.
      case Some(t) => PacketSender.sendRotatableState(t)
    }

  def onRedstoneStateRequest(p: PacketParser) =
    p.readTileEntity[TileEntity with Redstone]() match {
      case None => // Invalid packet.
      case Some(t) => PacketSender.sendRedstoneState(t)
    }

  def onKeyDown(p: PacketParser) =
    p.readTileEntity[Node]() match {
      case None => // Invalid packet.
      case Some(n) => n.network.foreach(_.sendToNeighbors(n, "keyboard.keyDown", p.player, p.readChar(), p.readInt()))
    }

  def onKeyUp(p: PacketParser) =
    p.readTileEntity[Node]() match {
      case None => // Invalid packet.
      case Some(n) => n.network.foreach(_.sendToNeighbors(n, "keyboard.keyUp", p.player, p.readChar(), p.readInt()))
    }

  def onClipboard(p: PacketParser) =
    p.readTileEntity[Node]() match {
      case None => // Invalid packet.
      case Some(n) => n.network.foreach(_.sendToNeighbors(n, "keyboard.clipboard", p.player, p.readUTF()))
    }
}
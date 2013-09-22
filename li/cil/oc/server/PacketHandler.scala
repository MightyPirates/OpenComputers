package li.cil.oc.server

import cpw.mods.fml.common.network.Player
import li.cil.oc.common.PacketBuilder
import li.cil.oc.common.{PacketHandler => CommonPacketHandler}
import li.cil.oc.common.PacketType
import li.cil.oc.common.tileentity.TileEntityComputer
import li.cil.oc.common.tileentity.TileEntityRotatable
import li.cil.oc.common.tileentity.TileEntityScreen
import net.minecraftforge.common.DimensionManager
import li.cil.oc.api.INetworkNode

class PacketHandler extends CommonPacketHandler {
  protected def world(player: Player, dimension: Int) =
    Option(DimensionManager.getWorld(dimension))

  def dispatch(p: PacketParser) =
    p.packetType match {
      case PacketType.ScreenBufferRequest => onScreenBufferRequest(p)
      case PacketType.ComputerStateRequest => onComputerStateRequest(p)
      case PacketType.RotatableStateRequest => onRotatableStateRequest(p)
      case PacketType.KeyDown => onKeyDown(p)
      case PacketType.KeyUp => onKeyUp(p)
      case _ => // Invalid packet.
    }

  def onScreenBufferRequest(p: PacketParser) =
    p.readTileEntity[TileEntityScreen]() match {
      case None => // Invalid packet.
      case Some(t) => {
        val pb = new PacketBuilder(PacketType.ScreenBufferResponse)

        pb.writeTileEntity(t)
        pb.writeUTF(t.screen.text)

        pb.sendToPlayer(p.player)
      }
    }

  def onComputerStateRequest(p: PacketParser) =
    p.readTileEntity[TileEntityComputer]() match {
      case None => // Invalid packet.
      case Some(t) => {
        val pb = new PacketBuilder(PacketType.ComputerStateResponse)

        pb.writeTileEntity(t)
        pb.writeBoolean(t.isOn)

        pb.sendToPlayer(p.player)
      }
    }

  def onRotatableStateRequest(p: PacketParser) =
    p.readTileEntity[TileEntityRotatable]() match {
      case None => // Invalid packet.
      case Some(t) => {
        val pb = new PacketBuilder(PacketType.RotatableStateResponse)

        pb.writeTileEntity(t)
        pb.writeDirection(t.pitch)
        pb.writeDirection(t.yaw)

        pb.sendToPlayer(p.player)
      }
    }

  def onKeyDown(p: PacketParser) =
    p.readTileEntity[INetworkNode]() match {
      case None => // Invalid packet.
      case Some(n) => n.getNetwork.sendToAll(n, "tryKeyDown", p.player, char2Character(p.readChar()))
    }

  def onKeyUp(p: PacketParser) =
    p.readTileEntity[INetworkNode]() match {
      case None => // Invalid packet.
      case Some(n) => n.getNetwork.sendToAll(n, "tryKeyUp", p.player, char2Character(p.readChar()))
    }
}
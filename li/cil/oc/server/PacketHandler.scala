package li.cil.oc.server

import cpw.mods.fml.common.network.Player
import li.cil.oc.common.PacketBuilder
import li.cil.oc.common.{ PacketHandler => CommonPacketHandler }
import li.cil.oc.common.PacketType
import li.cil.oc.common.tileentity.TileEntityScreen
import net.minecraftforge.common.DimensionManager

class PacketHandler extends CommonPacketHandler {
  protected def world(player: Player, dimension: Int) =
    Option(DimensionManager.getWorld(dimension))

  def dispatch(p: PacketParser) =
    p.packetType match {
      case PacketType.ScreenBufferRequest => onScreenBufferRequest(p)
      case _ => // Invalid packet.
    }

  def onScreenBufferRequest(p: PacketParser) =
    p.readTileEntity[TileEntityScreen] match {
      case None => // Invalid packet.
      case Some(t) => {
        val pb = new PacketBuilder(PacketType.ScreenBufferResponse)

        pb.writeTileEntity(t)
        pb.writeUTF(t.component.text)

        pb.sendToPlayer(p.player)
      }
    }
}
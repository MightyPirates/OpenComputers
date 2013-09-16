package li.cil.oc.common

import java.io.ByteArrayOutputStream
import java.io.DataOutputStream

import cpw.mods.fml.common.network.PacketDispatcher
import cpw.mods.fml.common.network.Player
import net.minecraft.network.packet.Packet
import net.minecraft.network.packet.Packet250CustomPayload
import net.minecraft.tileentity.TileEntity

/** Utility class for packet creation. */
class PacketBuilder(packetType: PacketType.Value, private val stream: ByteArrayOutputStream = new ByteArrayOutputStream) extends DataOutputStream(stream) {
  writeByte(packetType.id)

  def writeTileEntity(t: TileEntity) = {
    writeInt(t.worldObj.provider.dimensionId)
    writeInt(t.xCoord)
    writeInt(t.yCoord)
    writeInt(t.zCoord)
  }

  def sendToAllPlayers() = PacketDispatcher.sendPacketToAllPlayers(packet)

  def sendToPlayer(player: Player) = PacketDispatcher.sendPacketToPlayer(packet, player)

  def sendToServer() = PacketDispatcher.sendPacketToServer(packet)

  private def packet: Packet = {
    val p = new Packet250CustomPayload
    p.channel = "OpenComp"
    p.data = stream.toByteArray
    p.length = stream.size
    return p
  }
}
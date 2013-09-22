package li.cil.oc.common

import java.io.ByteArrayOutputStream
import java.io.DataOutputStream

import cpw.mods.fml.common.network.PacketDispatcher
import cpw.mods.fml.common.network.Player
import net.minecraft.network.packet.Packet250CustomPayload
import net.minecraft.tileentity.TileEntity
import net.minecraftforge.common.ForgeDirection

/** Utility class for packet creation. */
class PacketBuilder(packetType: PacketType.Value, private val stream: ByteArrayOutputStream = new ByteArrayOutputStream) extends DataOutputStream(stream) {
  writeByte(packetType.id)

  def writeTileEntity(t: TileEntity) = {
    writeInt(t.worldObj.provider.dimensionId)
    writeInt(t.xCoord)
    writeInt(t.yCoord)
    writeInt(t.zCoord)
  }

  def writeDirection(d: ForgeDirection) = writeInt(d.ordinal)

  def sendToAllPlayers() = PacketDispatcher.sendPacketToAllPlayers(packet)

  def sendToPlayer(player: Player) = PacketDispatcher.sendPacketToPlayer(packet, player)

  def sendToServer() = PacketDispatcher.sendPacketToServer(packet)

  private def packet = {
    val p = new Packet250CustomPayload
    p.channel = "OpenComp"
    p.data = stream.toByteArray
    p.length = stream.size
    p
  }
}
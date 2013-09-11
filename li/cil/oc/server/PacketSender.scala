package li.cil.oc.server

import java.io.ByteArrayOutputStream
import java.io.DataOutputStream
import cpw.mods.fml.common.network.PacketDispatcher
import li.cil.oc.common.PacketType
import net.minecraft.network.packet.Packet
import net.minecraft.network.packet.Packet250CustomPayload
import net.minecraft.tileentity.TileEntity

/** Centralized packet dispatcher for sending updates to the client. */
object PacketSender {
  def sendScreenResolutionChange(t: TileEntity, w: Int, h: Int) = {
    val p = new PacketBuilder(PacketType.ScreenResolutionChange)

    p.writeTileEntity(t)
    p.writeInt(w)
    p.writeInt(h)

    p.sendToAllPlayers()
  }

  def sendScreenSet(t: TileEntity, col: Int, row: Int, s: String) = {
    val p = new PacketBuilder(PacketType.ScreenSet)

    p.writeTileEntity(t)
    p.writeInt(col)
    p.writeInt(row)
    p.writeUTF(s)

    p.sendToAllPlayers()
  }

  def sendScreenFill(t: TileEntity, col: Int, row: Int, w: Int, h: Int, c: Char) = {
    val p = new PacketBuilder(PacketType.ScreenFill)

    p.writeTileEntity(t)
    p.writeInt(col)
    p.writeInt(row)
    p.writeInt(w)
    p.writeInt(h)
    p.writeChar(c)

    p.sendToAllPlayers()
  }

  def sendScreenCopy(t: TileEntity, col: Int, row: Int, w: Int, h: Int, tx: Int, ty: Int) = {
    val p = new PacketBuilder(PacketType.ScreenCopy)

    p.writeTileEntity(t)
    p.writeInt(col)
    p.writeInt(row)
    p.writeInt(w)
    p.writeInt(h)
    p.writeInt(tx)
    p.writeInt(ty)

    p.sendToAllPlayers()
  }

  /** Utility class for packet creation. */
  private class PacketBuilder(packetType: PacketType.Value, private val stream: ByteArrayOutputStream = new ByteArrayOutputStream) extends DataOutputStream(stream) {
    writeByte(packetType.id)

    def writeTileEntity(t: TileEntity) = {
      writeInt(t.xCoord)
      writeInt(t.yCoord)
      writeInt(t.zCoord)
    }

    def sendToAllPlayers() = PacketDispatcher.sendPacketToAllPlayers(packet)

    private def packet: Packet = {
      val p = new Packet250CustomPayload
      p.channel = "OpenComp"
      p.data = stream.toByteArray
      p.length = stream.size
      return p
    }
  }
}
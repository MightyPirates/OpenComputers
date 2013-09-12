package li.cil.oc.client

import java.io.ByteArrayInputStream
import java.io.DataInputStream

import cpw.mods.fml.common.network.IPacketHandler
import cpw.mods.fml.common.network.Player
import li.cil.oc.common.PacketType
import li.cil.oc.common.tileentity.TileEntityScreen
import net.minecraft.entity.player.EntityPlayer
import net.minecraft.network.INetworkManager
import net.minecraft.network.packet.Packet250CustomPayload
import net.minecraft.tileentity.TileEntity

/**
 * Client side packet handler, processes packets sent from the server.
 *
 * @see li.cil.oc.server.PacketSender
 */
class PacketHandler extends IPacketHandler {
  /** Top level dispatcher based on packet type. */
  def onPacketData(manager: INetworkManager, packet: Packet250CustomPayload, player: Player) {
    val p = new PacketParser(packet, player)
    p.packetType match {
      case PacketType.ScreenResolutionChange => onScreenResolutionChange(p)
      case PacketType.ScreenSet => onScreenSet(p)
      case PacketType.ScreenFill => onScreenFill(p)
      case PacketType.ScreenCopy => onScreenCopy(p)
    }
  }

  def onScreenResolutionChange(p: PacketParser) = {
    val t = p.readTileEntity[TileEntityScreen]()
    val w = p.readInt()
    val h = p.readInt()
    t.component.resolution = (w, h)
  }

  def onScreenSet(p: PacketParser) = {
    val t = p.readTileEntity[TileEntityScreen]()
    val col = p.readInt()
    val row = p.readInt()
    val s = p.readUTF()
    t.component.set(col, row, s)
  }

  def onScreenFill(p: PacketParser) = {
    val t = p.readTileEntity[TileEntityScreen]()
    val col = p.readInt()
    val row = p.readInt()
    val w = p.readInt()
    val h = p.readInt()
    val c = p.readChar()
    t.component.fill(col, row, w, h, c)
  }

  def onScreenCopy(p: PacketParser) = {
    val t = p.readTileEntity[TileEntityScreen]()
    val col = p.readInt()
    val row = p.readInt()
    val w = p.readInt()
    val h = p.readInt()
    val tx = p.readInt()
    val ty = p.readInt()
    t.component.copy(col, row, w, h, tx, ty)
  }

  /** Utility class for packet parsing. */
  private class PacketParser(packet: Packet250CustomPayload, player: Player) extends DataInputStream(new ByteArrayInputStream(packet.data)) {
    val world = player.asInstanceOf[EntityPlayer].worldObj
    val packetType = PacketType(readByte())

    def readTileEntity[T <: TileEntity]() = {
      val x = readInt()
      val y = readInt()
      val z = readInt()
      world.getBlockTileEntity(x, y, z).asInstanceOf[T]
    }
  }
}
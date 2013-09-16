package li.cil.oc.server

import li.cil.oc.common.PacketBuilder
import li.cil.oc.common.PacketType
import net.minecraft.tileentity.TileEntity

/** Centralized packet dispatcher for sending updates to the client. */
object PacketSender {
  def sendScreenResolutionChange(t: TileEntity, w: Int, h: Int) = {
    val pb = new PacketBuilder(PacketType.ScreenResolutionChange)

    pb.writeTileEntity(t)
    pb.writeInt(w)
    pb.writeInt(h)

    pb.sendToAllPlayers()
  }

  def sendScreenSet(t: TileEntity, col: Int, row: Int, s: String) = {
    val pb = new PacketBuilder(PacketType.ScreenSet)

    pb.writeTileEntity(t)
    pb.writeInt(col)
    pb.writeInt(row)
    pb.writeUTF(s)

    pb.sendToAllPlayers()
  }

  def sendScreenFill(t: TileEntity, col: Int, row: Int, w: Int, h: Int, c: Char) = {
    val pb = new PacketBuilder(PacketType.ScreenFill)

    pb.writeTileEntity(t)
    pb.writeInt(col)
    pb.writeInt(row)
    pb.writeInt(w)
    pb.writeInt(h)
    pb.writeChar(c)

    pb.sendToAllPlayers()
  }

  def sendScreenCopy(t: TileEntity, col: Int, row: Int, w: Int, h: Int, tx: Int, ty: Int) = {
    val pb = new PacketBuilder(PacketType.ScreenCopy)

    pb.writeTileEntity(t)
    pb.writeInt(col)
    pb.writeInt(row)
    pb.writeInt(w)
    pb.writeInt(h)
    pb.writeInt(tx)
    pb.writeInt(ty)

    pb.sendToAllPlayers()
  }
}
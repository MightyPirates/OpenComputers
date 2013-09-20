package li.cil.oc.server

import li.cil.oc.common.PacketBuilder
import li.cil.oc.common.PacketBuilder._
import li.cil.oc.common.PacketType
import li.cil.oc.common.tileentity.TileEntityRotatable
import net.minecraft.tileentity.TileEntity
import net.minecraftforge.common.ForgeDirection

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

  def sendComputerState(t: TileEntity, value: Boolean) = {
    val pb = new PacketBuilder(PacketType.ComputerStateResponse)

    pb.writeTileEntity(t)
    pb.writeBoolean(value)

    pb.sendToAllPlayers()
  }

  def sendRotatableRotate(t: TileEntityRotatable, pitch: ForgeDirection, yaw: ForgeDirection) = {
    val pb = new PacketBuilder(PacketType.RotatableStateResponse)

    pb.writeTileEntity(t)
    pb.writeDirection(pitch)
    pb.writeDirection(yaw)

    pb.sendToAllPlayers()
  }
}
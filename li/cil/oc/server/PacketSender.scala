package li.cil.oc.server

import cpw.mods.fml.common.network.Player
import li.cil.oc.common.PacketBuilder
import li.cil.oc.common.PacketType
import li.cil.oc.common.tileentity.Rotatable
import li.cil.oc.server.component.Redstone
import net.minecraft.tileentity.TileEntity
import net.minecraftforge.common.ForgeDirection

/** Centralized packet dispatcher for sending updates to the client. */
object PacketSender {
  def sendScreenBufferState(t: TileEntity, w: Int, h: Int, text: String, player: Option[Player] = None) = {
    val pb = new PacketBuilder(PacketType.ScreenBufferResponse)

    pb.writeTileEntity(t)
    pb.writeInt(w)
    pb.writeInt(h)
    pb.writeUTF(text)

    player match {
      case Some(p) => pb.sendToPlayer(p)
      case _ => pb.sendToAllPlayers()
    }
  }

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

  def sendComputerState(t: TileEntity, value: Boolean, player: Option[Player] = None) = {
    val pb = new PacketBuilder(PacketType.ComputerStateResponse)

    pb.writeTileEntity(t)
    pb.writeBoolean(value)

    player match {
      case Some(p) => pb.sendToPlayer(p)
      case _ => pb.sendToAllPlayers()
    }
  }

  def sendRotatableState(t: Rotatable, player: Option[Player] = None) = {
    val pb = new PacketBuilder(PacketType.RotatableStateResponse)

    pb.writeTileEntity(t)
    pb.writeDirection(t.pitch)
    pb.writeDirection(t.yaw)

    player match {
      case Some(p) => pb.sendToPlayer(p)
      case _ => pb.sendToAllPlayers()
    }
  }

  def sendRedstoneState(t: TileEntity with Redstone, player: Option[Player] = None) = {
    val pb = new PacketBuilder(PacketType.RedstoneStateResponse)

    pb.writeTileEntity(t)
    pb.writeBoolean(t.isOutputEnabled)
    for (d <- ForgeDirection.VALID_DIRECTIONS) {
      pb.writeByte(t.output(d))
    }

    player match {
      case Some(p) => pb.sendToPlayer(p)
      case _ => pb.sendToAllPlayers()
    }
  }
}
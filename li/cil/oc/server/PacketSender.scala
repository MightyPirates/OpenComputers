package li.cil.oc.server

import cpw.mods.fml.common.network.Player
import li.cil.oc.common.PacketBuilder
import li.cil.oc.common.PacketType
import li.cil.oc.common.component.Buffer
import li.cil.oc.common.tileentity.{Redstone, PowerDistributor, Rotatable}
import li.cil.oc.util.PackedColor
import net.minecraft.tileentity.TileEntity
import net.minecraftforge.common.ForgeDirection

/** Centralized packet dispatcher for sending updates to the client. */
object PacketSender {
  def sendClipboard(value: String, player: Player) {
    val pb = new PacketBuilder(PacketType.Clipboard)

    pb.writeUTF(value)

    pb.sendToPlayer(player)
  }

  def sendComputerState(t: TileEntity, value: Boolean, player: Option[Player] = None) {
    val pb = new PacketBuilder(PacketType.ComputerStateResponse)

    pb.writeTileEntity(t)
    pb.writeBoolean(value)

    player match {
      case Some(p) => pb.sendToPlayer(p)
      case _ => pb.sendToAllPlayers()
    }
  }

  def sendPowerState(t: PowerDistributor, player: Option[Player] = None) {
    val pb = new PacketBuilder(PacketType.PowerStateResponse)

    pb.writeTileEntity(t)
    pb.writeDouble(t.average)

    player match {
      case Some(p) => pb.sendToPlayer(p)
      case _ => pb.sendToAllPlayers()
    }
  }

  def sendRedstoneState(t: TileEntity with Redstone, player: Option[Player] = None) {
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

  def sendRotatableState(t: TileEntity with Rotatable, player: Option[Player] = None) {
    val pb = new PacketBuilder(PacketType.RotatableStateResponse)

    pb.writeTileEntity(t)
    pb.writeDirection(t.pitch)
    pb.writeDirection(t.yaw)

    player match {
      case Some(p) => pb.sendToPlayer(p)
      case _ => pb.sendToAllPlayers()
    }
  }

  def sendScreenBufferState(t: TileEntity with Buffer.Environment, player: Option[Player] = None) {
    val pb = new PacketBuilder(PacketType.ScreenBufferResponse)

    pb.writeTileEntity(t)

    val screen = t.instance
    val (w, h) = screen.resolution
    pb.writeInt(w)
    pb.writeInt(h)
    pb.writeUTF(screen.text)
    pb.writeInt(screen.depth.id)
    pb.writeInt(screen.foreground)
    pb.writeInt(screen.background)
    for (cs <- screen.colors) for (c <- cs) pb.writeShort(c)

    player match {
      case Some(p) => pb.sendToPlayer(p)
      case _ => pb.sendToAllPlayers()
    }
  }

  def sendScreenColorChange(t: TileEntity, foreground: Int, background: Int) {
    val pb = new PacketBuilder(PacketType.ScreenColorChange)

    pb.writeTileEntity(t)
    pb.writeInt(foreground)
    pb.writeInt(background)

    pb.sendToAllPlayers()
  }

  def sendScreenCopy(t: TileEntity, col: Int, row: Int, w: Int, h: Int, tx: Int, ty: Int) {
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

  def sendScreenDepthChange(t: TileEntity, value: PackedColor.Depth.Value) {
    val pb = new PacketBuilder(PacketType.ScreenDepthChange)

    pb.writeTileEntity(t)
    pb.writeInt(value.id)

    pb.sendToAllPlayers()
  }

  def sendScreenFill(t: TileEntity, col: Int, row: Int, w: Int, h: Int, c: Char) {
    val pb = new PacketBuilder(PacketType.ScreenFill)

    pb.writeTileEntity(t)
    pb.writeInt(col)
    pb.writeInt(row)
    pb.writeInt(w)
    pb.writeInt(h)
    pb.writeChar(c)

    pb.sendToAllPlayers()
  }

  def sendScreenResolutionChange(t: TileEntity, w: Int, h: Int) {
    val pb = new PacketBuilder(PacketType.ScreenResolutionChange)

    pb.writeTileEntity(t)
    pb.writeInt(w)
    pb.writeInt(h)

    pb.sendToAllPlayers()
  }

  def sendScreenSet(t: TileEntity, col: Int, row: Int, s: String) {
    val pb = new PacketBuilder(PacketType.ScreenSet)

    pb.writeTileEntity(t)
    pb.writeInt(col)
    pb.writeInt(row)
    pb.writeUTF(s)

    pb.sendToAllPlayers()
  }
}
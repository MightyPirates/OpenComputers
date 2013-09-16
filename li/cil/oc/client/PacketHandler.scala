package li.cil.oc.client

import li.cil.oc.common.{ PacketHandler => CommonPacketHandler }
import li.cil.oc.common.PacketType
import li.cil.oc.common.tileentity.TileEntityScreen
import cpw.mods.fml.common.network.Player
import net.minecraft.entity.player.EntityPlayer

class PacketHandler extends CommonPacketHandler {
  protected def world(player: Player, dimension: Int) = {
    val world = player.asInstanceOf[EntityPlayer].worldObj
    if (world.provider.dimensionId == dimension) Some(world)
    else None
  }

  def dispatch(p: PacketParser) =
    p.packetType match {
      case PacketType.ScreenResolutionChange => onScreenResolutionChange(p)
      case PacketType.ScreenSet => onScreenSet(p)
      case PacketType.ScreenFill => onScreenFill(p)
      case PacketType.ScreenCopy => onScreenCopy(p)
      case PacketType.ScreenBufferResponse => onScreenBufferResponse(p)
      case _ => // Invalid packet.
    }

  def onScreenResolutionChange(p: PacketParser) =
    p.readTileEntity[TileEntityScreen] match {
      case None => // Invalid packet.
      case Some(t) => {
        val w = p.readInt()
        val h = p.readInt()
        t.component.resolution = (w, h)
      }
    }

  def onScreenSet(p: PacketParser) =
    p.readTileEntity[TileEntityScreen] match {
      case None => // Invalid packet.
      case Some(t) => {
        val col = p.readInt()
        val row = p.readInt()
        val s = p.readUTF()
        println("client apply screen set")
        t.component.set(col, row, s)
      }
    }

  def onScreenFill(p: PacketParser) =
    p.readTileEntity[TileEntityScreen] match {
      case None => // Invalid packet.
      case Some(t) => {
        val col = p.readInt()
        val row = p.readInt()
        val w = p.readInt()
        val h = p.readInt()
        val c = p.readChar()
        t.component.fill(col, row, w, h, c)
      }
    }

  def onScreenCopy(p: PacketParser) =
    p.readTileEntity[TileEntityScreen] match {
      case None => // Invalid packet.
      case Some(t) => {
        val col = p.readInt()
        val row = p.readInt()
        val w = p.readInt()
        val h = p.readInt()
        val tx = p.readInt()
        val ty = p.readInt()
        t.component.copy(col, row, w, h, tx, ty)
      }
    }

  def onScreenBufferResponse(p: PacketParser) =
    p.readTileEntity[TileEntityScreen] match {
      case None => // Invalid packet.
      case Some(t) => p.readUTF.split('\n').zipWithIndex.
        foreach { case (line, i) => t.component.set(0, i, line) }
    }
}
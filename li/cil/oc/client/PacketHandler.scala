package li.cil.oc.client

import cpw.mods.fml.common.network.Player
import li.cil.oc.common.PacketType
import li.cil.oc.common.tileentity._
import li.cil.oc.common.{PacketHandler => CommonPacketHandler}
import li.cil.oc.util.PackedColor
import net.minecraft.client.gui.GuiScreen
import net.minecraft.entity.player.EntityPlayer
import net.minecraft.tileentity.TileEntity
import net.minecraftforge.common.ForgeDirection
import org.lwjgl.input.Keyboard
import scala.Some

class PacketHandler extends CommonPacketHandler {
  protected override def world(player: Player, dimension: Int) = {
    val world = player.asInstanceOf[EntityPlayer].worldObj
    if (world.provider.dimensionId == dimension) Some(world)
    else None
  }

  override def dispatch(p: PacketParser) =
    p.packetType match {
      case PacketType.Clipboard => onClipboard(p)
      case PacketType.ComputerStateResponse => onComputerStateResponse(p)
      case PacketType.PowerStateResponse => onPowerStateResponse(p)
      case PacketType.RedstoneStateResponse => onRedstoneStateResponse(p)
      case PacketType.RotatableStateResponse => onRotatableStateResponse(p)
      case PacketType.ScreenBufferResponse => onScreenBufferResponse(p)
      case PacketType.ScreenColorChange => onScreenColorChange(p)
      case PacketType.ScreenCopy => onScreenCopy(p)
      case PacketType.ScreenDepthChange => onScreenDepthChange(p)
      case PacketType.ScreenFill => onScreenFill(p)
      case PacketType.ScreenResolutionChange => onScreenResolutionChange(p)
      case PacketType.ScreenSet => onScreenSet(p)
      case _ => // Invalid packet.
    }

  def onClipboard(p: PacketParser) = {
    if (Keyboard.isKeyDown(Keyboard.KEY_LCONTROL) || Keyboard.isKeyDown(Keyboard.KEY_LCONTROL)) {
      GuiScreen.setClipboardString(p.readUTF())
      p.player.asInstanceOf[EntityPlayer].addChatMessage("Copied to clipboard.")
    }
  }

  def onComputerStateResponse(p: PacketParser) =
    p.readTileEntity[Case]() match {
      case Some(t) => {
        t.isOn = p.readBoolean()
      }
      case _ => // Invalid packet.
    }

  def onPowerStateResponse(p: PacketParser) =
    p.readTileEntity[PowerDistributor]() match {
      case Some(t) => {
        t.average = p.readDouble()
      }
      case _ => // Invalid packet.
    }

  def onRedstoneStateResponse(p: PacketParser) =
    p.readTileEntity[TileEntity with Redstone]() match {
      case Some(t) =>
        t.isOutputEnabled = p.readBoolean()
        for (d <- ForgeDirection.VALID_DIRECTIONS) {
          t.output(d, p.readByte())
        }
      case _ => // Invalid packet.
    }

  def onRotatableStateResponse(p: PacketParser) =
    p.readTileEntity[TileEntity with Rotatable]() match {
      case Some(t) =>
        t.pitch = p.readDirection()
        t.yaw = p.readDirection()
      case _ => // Invalid packet.
    }

  def onScreenBufferResponse(p: PacketParser) =
    p.readTileEntity[Screen]() match {
      case Some(t) =>
        val screen = t.instance
        val w = p.readInt()
        val h = p.readInt()
        screen.resolution = (w, h)
        p.readUTF.split('\n').zipWithIndex.foreach {
          case (line, i) => screen.set(0, i, line)
        }
        screen.depth = PackedColor.Depth(p.readInt())
        screen.foreground = p.readInt()
        screen.background = p.readInt()
        for (row <- 0 until h) {
          val rowColor = screen.colors(row)
          for (col <- 0 until w) {
            rowColor(col) = p.readShort()
          }
        }
      case _ => // Invalid packet.
    }

  def onScreenColorChange(p: PacketParser) =
    p.readTileEntity[Screen]() match {
      case Some(t) => {
        t.instance.foreground = p.readInt()
        t.instance.background = p.readInt()
      }
      case _ => // Invalid packet.
    }

  def onScreenCopy(p: PacketParser) =
    p.readTileEntity[Screen]() match {
      case Some(t) => {
        val col = p.readInt()
        val row = p.readInt()
        val w = p.readInt()
        val h = p.readInt()
        val tx = p.readInt()
        val ty = p.readInt()
        t.instance.copy(col, row, w, h, tx, ty)
      }
      case _ => // Invalid packet.
    }

  def onScreenDepthChange(p: PacketParser) =
    p.readTileEntity[Screen]() match {
      case Some(t) => {
        t.instance.depth = PackedColor.Depth(p.readInt())
      }
      case _ => // Invalid packet.
    }

  def onScreenFill(p: PacketParser) =
    p.readTileEntity[Screen]() match {
      case Some(t) => {
        val col = p.readInt()
        val row = p.readInt()
        val w = p.readInt()
        val h = p.readInt()
        val c = p.readChar()
        t.instance.fill(col, row, w, h, c)
      }
      case _ => // Invalid packet.
    }

  def onScreenResolutionChange(p: PacketParser) =
    p.readTileEntity[Screen]() match {
      case Some(t) => {
        val w = p.readInt()
        val h = p.readInt()
        t.instance.resolution = (w, h)
      }
      case _ => // Invalid packet.
    }

  def onScreenSet(p: PacketParser) =
    p.readTileEntity[Screen]() match {
      case Some(t) => {
        val col = p.readInt()
        val row = p.readInt()
        val s = p.readUTF()
        t.instance.set(col, row, s)
      }
      case _ => // Invalid packet.
    }
}
package li.cil.oc.client

import li.cil.oc.common.tileentity._
import li.cil.oc.common.tileentity.traits.{Computer, TextBuffer}
import li.cil.oc.common.{CompressedPacketBuilder, PacketBuilder, PacketType, component}
import net.minecraft.client.Minecraft
import net.minecraftforge.common.ForgeDirection

object PacketSender {
  // Timestamp after which the next clipboard message may be sent. Used to
  // avoid spamming large packets on key repeat.
  protected var clipboardCooldown = 0L

  def sendComputerPower(t: Computer, power: Boolean) {
    val pb = new PacketBuilder(PacketType.ComputerPower)

    pb.writeTileEntity(t)
    pb.writeBoolean(power)

    pb.sendToServer()
  }

  def sendKeyDown(b: component.Buffer, char: Char, code: Int) {
    val pb = new PacketBuilder(PacketType.KeyDown)

    b.owner match {
      case t: TextBuffer if t.hasKeyboard =>
        pb.writeTileEntity(t)
      case t: component.Terminal =>
        pb.writeTileEntity(t.rack)
        pb.writeInt(t.number)
      case _ => return
    }
    pb.writeChar(char)
    pb.writeInt(code)

    pb.sendToServer()
  }

  def sendKeyUp(b: component.Buffer, char: Char, code: Int) {
    val pb = new PacketBuilder(PacketType.KeyUp)

    b.owner match {
      case t: TextBuffer if t.hasKeyboard =>
        pb.writeTileEntity(t)
      case t: component.Terminal =>
        pb.writeTileEntity(t.rack)
        pb.writeInt(t.number)
      case _ => return
    }
    pb.writeChar(char)
    pb.writeInt(code)

    pb.sendToServer()
  }

  def sendClipboard(b: component.Buffer, value: String) {
    if (value != null && !value.isEmpty) {
      if (System.currentTimeMillis() < clipboardCooldown) {
        Minecraft.getMinecraft.sndManager.playSoundFX("note.harp", 3, 1)
      }
      else {
        clipboardCooldown = System.currentTimeMillis() + value.length / 10
        val pb = new CompressedPacketBuilder(PacketType.Clipboard)

        b.owner match {
          case t: TextBuffer if t.hasKeyboard =>
            pb.writeTileEntity(t)
          case t: component.Terminal =>
            pb.writeTileEntity(t.rack)
            pb.writeInt(t.number)
          case _ => return
        }
        pb.writeUTF(value.substring(0, math.min(value.length, 64 * 1024)))

        pb.sendToServer()
      }
    }
  }

  def sendMouseClick(b: component.Buffer, x: Int, y: Int, drag: Boolean, button: Int) {
    val pb = new PacketBuilder(PacketType.MouseClickOrDrag)

    b.owner match {
      case t: TextBuffer if t.tier > 0 =>
        pb.writeTileEntity(t)
      case t: component.Terminal =>
        pb.writeTileEntity(t.rack)
        pb.writeInt(t.number)
      case _ => return
    }
    pb.writeInt(x)
    pb.writeInt(y)
    pb.writeBoolean(drag)
    pb.writeByte(button.toByte)

    pb.sendToServer()
  }

  def sendMouseScroll(b: component.Buffer, x: Int, y: Int, scroll: Int) {
    val pb = new PacketBuilder(PacketType.MouseScroll)

    b.owner match {
      case t: TextBuffer if t.tier > 0 =>
        pb.writeTileEntity(t)
      case t: component.Terminal =>
        pb.writeTileEntity(t.rack)
        pb.writeInt(t.number)
      case _ => return
    }
    pb.writeInt(x)
    pb.writeInt(y)
    pb.writeByte(scroll)

    pb.sendToServer()
  }

  def sendMouseUp(b: component.Buffer, x: Int, y: Int, button: Int) {
    val pb = new PacketBuilder(PacketType.MouseUp)

    b.owner match {
      case t: TextBuffer if t.tier > 0 =>
        pb.writeTileEntity(t)
      case t: component.Terminal =>
        pb.writeTileEntity(t.rack)
        pb.writeInt(t.number)
      case _ => return
    }
    pb.writeInt(x)
    pb.writeInt(y)
    pb.writeByte(button.toByte)

    pb.sendToServer()
  }

  def sendMultiPlace() {
    val pb = new PacketBuilder(PacketType.MultiPartPlace)
    pb.sendToServer()
  }

  def sendRobotStateRequest(dimension: Int, x: Int, y: Int, z: Int) {
    val pb = new PacketBuilder(PacketType.RobotStateRequest)

    pb.writeInt(dimension)
    pb.writeInt(x)
    pb.writeInt(y)
    pb.writeInt(z)

    pb.sendToServer()
  }

  def sendServerPower(t: Rack, number: Int, power: Boolean) {
    val pb = new PacketBuilder(PacketType.ComputerPower)

    pb.writeTileEntity(t)
    pb.writeInt(number)
    pb.writeBoolean(power)

    pb.sendToServer()
  }

  def sendServerRange(t: Rack, range: Int) {
    val pb = new PacketBuilder(PacketType.ServerRange)

    pb.writeTileEntity(t)
    pb.writeInt(range)

    pb.sendToServer()
  }

  def sendServerSide(t: Rack, number: Int, side: ForgeDirection) {
    val pb = new PacketBuilder(PacketType.ServerSide)

    pb.writeTileEntity(t)
    pb.writeInt(number)
    pb.writeDirection(side)

    pb.sendToServer()
  }
}
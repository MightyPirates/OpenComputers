package li.cil.oc.client

import li.cil.oc.common.PacketBuilder
import li.cil.oc.common.PacketType
import li.cil.oc.common.tileentity._

object PacketSender {
  def sendComputerPower(t: Computer, power: Boolean) {
    val pb = new PacketBuilder(PacketType.ComputerPower)

    pb.writeTileEntity(t)
    pb.writeBoolean(power)

    pb.sendToServer()
  }

  def sendKeyDown(t: Buffer, char: Char, code: Int) =
    if (t.hasKeyboard) {
      val pb = new PacketBuilder(PacketType.KeyDown)

      pb.writeTileEntity(t)
      pb.writeChar(char)
      pb.writeInt(code)

      pb.sendToServer()
    }

  def sendKeyUp(t: Buffer, char: Char, code: Int) =
    if (t.hasKeyboard) {
      val pb = new PacketBuilder(PacketType.KeyUp)

      pb.writeTileEntity(t)
      pb.writeChar(char)
      pb.writeInt(code)

      pb.sendToServer()
    }

  def sendClipboard(t: Buffer, value: String) =
    if (value != null && !value.isEmpty && t.hasKeyboard) {
      val pb = new PacketBuilder(PacketType.Clipboard)

      pb.writeTileEntity(t)
      pb.writeUTF(value.substring(0, math.min(value.length, 1024)))

      pb.sendToServer()
    }

  def sendMouseClick(t: Buffer, x: Int, y: Int, drag: Boolean) =
    if (t.tier > 0) {
      val pb = new PacketBuilder(PacketType.MouseClickOrDrag)

      pb.writeTileEntity(t)
      pb.writeInt(x)
      pb.writeInt(y)
      pb.writeBoolean(drag)

      pb.sendToServer()
    }

  def sendMouseScroll(t: Buffer, x: Int, y: Int, scroll: Int) =
    if (t.tier > 0) {
      val pb = new PacketBuilder(PacketType.MouseScroll)

      pb.writeTileEntity(t)
      pb.writeInt(x)
      pb.writeInt(y)
      pb.writeByte(scroll)

      pb.sendToServer()
    }

  def sendServerPower(t: Rack, number: Int, power: Boolean) {
    val pb = new PacketBuilder(PacketType.ComputerPower)

    pb.writeTileEntity(t)
    pb.writeInt(number)
    pb.writeBoolean(power)

    pb.sendToServer()
  }
}
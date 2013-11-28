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

  def sendKeyDown[T <: Buffer](t: T, char: Char, code: Int) = if (t.hasKeyboard) {
    val pb = new PacketBuilder(PacketType.KeyDown)

    pb.writeTileEntity(t)
    pb.writeChar(char)
    pb.writeInt(code)

    pb.sendToServer()
  }

  def sendKeyUp[T <: Buffer](t: T, char: Char, code: Int) = if (t.hasKeyboard) {
    val pb = new PacketBuilder(PacketType.KeyUp)

    pb.writeTileEntity(t)
    pb.writeChar(char)
    pb.writeInt(code)

    pb.sendToServer()
  }

  def sendClipboard[T <: Buffer](t: T, value: String) = if (!value.isEmpty && t.hasKeyboard) {
    val pb = new PacketBuilder(PacketType.Clipboard)

    pb.writeTileEntity(t)
    pb.writeUTF(value.substring(0, value.length min 1024))

    pb.sendToServer()
  }
}
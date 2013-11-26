package li.cil.oc.client

import li.cil.oc.common.PacketBuilder
import li.cil.oc.common.PacketType
import li.cil.oc.common.tileentity._

object PacketSender {
  def sendComputerStateRequest(t: Computer) {
    val pb = new PacketBuilder(PacketType.ComputerStateRequest)

    pb.writeTileEntity(t)

    pb.sendToServer()
  }

  def sendPowerStateRequest(t: PowerInformation) {
    val pb = new PacketBuilder(PacketType.PowerStateRequest)

    pb.writeTileEntity(t)

    pb.sendToServer()
  }

  def sendRedstoneStateRequest(t: Redstone) {
    val pb = new PacketBuilder(PacketType.RedstoneStateRequest)

    pb.writeTileEntity(t)

    pb.sendToServer()
  }

  def sendRobotStateRequest(t: Robot) {
    val pb = new PacketBuilder(PacketType.RobotStateRequest)

    pb.writeTileEntity(t)

    pb.sendToServer()
  }

  def sendRotatableStateRequest(t: Rotatable) {
    val pb = new PacketBuilder(PacketType.RotatableStateRequest)

    pb.writeTileEntity(t)

    pb.sendToServer()
  }

  def sendScreenBufferRequest(t: Buffer) {
    val pb = new PacketBuilder(PacketType.ScreenBufferRequest)

    pb.writeTileEntity(t)

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
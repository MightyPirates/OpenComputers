package li.cil.oc.client

import li.cil.oc.api.network.environment.Environment
import li.cil.oc.common.PacketBuilder
import li.cil.oc.common.PacketType
import li.cil.oc.common.tileentity.Computer
import li.cil.oc.common.tileentity.Rotatable
import li.cil.oc.common.tileentity.Screen
import li.cil.oc.server.component.Redstone
import net.minecraft.tileentity.TileEntity

object PacketSender {
  def sendScreenBufferRequest(t: Screen) = {
    val pb = new PacketBuilder(PacketType.ScreenBufferRequest)

    pb.writeTileEntity(t)

    pb.sendToServer()
  }

  def sendComputerStateRequest(t: Computer) = {
    val pb = new PacketBuilder(PacketType.ComputerStateRequest)

    pb.writeTileEntity(t)

    pb.sendToServer()
  }

  def sendRotatableStateRequest(t: Rotatable) = {
    val pb = new PacketBuilder(PacketType.RotatableStateRequest)

    pb.writeTileEntity(t)

    pb.sendToServer()
  }

  def sendRedstoneStateRequest(t: TileEntity with Redstone) = {
    val pb = new PacketBuilder(PacketType.RedstoneStateRequest)

    pb.writeTileEntity(t)

    pb.sendToServer()
  }

  def sendKeyDown[T <: TileEntity with Environment](t: T, char: Char, code: Int) = {
    val pb = new PacketBuilder(PacketType.KeyDown)

    pb.writeTileEntity(t)
    pb.writeChar(char)
    pb.writeInt(code)

    pb.sendToServer()
  }

  def sendKeyUp[T <: TileEntity with Environment](t: T, char: Char, code: Int) = {
    val pb = new PacketBuilder(PacketType.KeyUp)

    pb.writeTileEntity(t)
    pb.writeChar(char)
    pb.writeInt(code)

    pb.sendToServer()
  }

  def sendClipboard[T <: TileEntity with Environment](t: T, value: String) = if (!value.isEmpty) {
    val pb = new PacketBuilder(PacketType.Clipboard)

    pb.writeTileEntity(t)
    pb.writeUTF(value)

    pb.sendToServer()
  }
}
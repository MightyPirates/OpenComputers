package li.cil.oc.client

import li.cil.oc.api.network.Node
import li.cil.oc.common.PacketBuilder
import li.cil.oc.common.PacketType
import li.cil.oc.common.tileentity.TileEntityComputer
import li.cil.oc.common.tileentity.TileEntityRotatable
import li.cil.oc.common.tileentity.TileEntityScreen
import net.minecraft.tileentity.TileEntity

object PacketSender {
  def sendScreenBufferRequest(t: TileEntityScreen) = {
    val pb = new PacketBuilder(PacketType.ScreenBufferRequest)
    pb.writeTileEntity(t)
    pb.sendToServer()
  }

  def sendComputerStateRequest(t: TileEntityComputer) = {
    val pb = new PacketBuilder(PacketType.ComputerStateRequest)
    pb.writeTileEntity(t)
    pb.sendToServer()
  }

  def sendRotatableStateRequest(t: TileEntityRotatable) = {
    val pb = new PacketBuilder(PacketType.RotatableStateRequest)
    pb.writeTileEntity(t)
    pb.sendToServer()
  }

  def sendKeyDown[T <: TileEntity with Node](t: T, char: Char, code: Int) = {
    val pb = new PacketBuilder(PacketType.KeyDown)

    pb.writeTileEntity(t)
    pb.writeChar(char)
    pb.writeInt(code)

    pb.sendToServer()
  }

  def sendKeyUp[T <: TileEntity with Node](t: T, char: Char, code: Int) = {
    val pb = new PacketBuilder(PacketType.KeyUp)

    pb.writeTileEntity(t)
    pb.writeChar(char)
    pb.writeInt(code)

    pb.sendToServer()
  }

  def sendClipboard[T <: TileEntity with Node](t: T, value: String) = if (!value.isEmpty) {
    val pb = new PacketBuilder(PacketType.Clipboard)

    pb.writeTileEntity(t)
    pb.writeUTF(value)

    pb.sendToServer()
  }
}
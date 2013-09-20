package li.cil.oc.client

import net.minecraft.tileentity.TileEntity
import li.cil.oc.common.PacketBuilder
import li.cil.oc.common.PacketType

object PacketSender {
  def sendScreenBufferRequest(t: TileEntity) = {
    val pb = new PacketBuilder(PacketType.ScreenBufferRequest)
    pb.writeTileEntity(t)
    pb.sendToServer()
  }

  def sendComputerStateRequest(t: TileEntity) = {
    val pb = new PacketBuilder(PacketType.ComputerStateRequest)
    pb.writeTileEntity(t)
    pb.sendToServer()
  }

  def sendRotatableStateRequest(t: TileEntity) = {
    val pb = new PacketBuilder(PacketType.RotatableStateRequest)
    pb.writeTileEntity(t)
    pb.sendToServer()
  }
}
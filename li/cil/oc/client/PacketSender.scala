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
}
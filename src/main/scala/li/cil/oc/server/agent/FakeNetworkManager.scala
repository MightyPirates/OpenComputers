package li.cil.oc.server.agent

import io.netty.util.concurrent.Future
import io.netty.util.concurrent.GenericFutureListener
import net.minecraft.network.EnumPacketDirection
import net.minecraft.network.INetHandler
import net.minecraft.network.NetworkManager
import net.minecraft.network.Packet

object FakeNetworkManager extends NetworkManager(EnumPacketDirection.CLIENTBOUND) {
  override def sendPacket(packetIn: Packet[_ <: INetHandler]): Unit = {}

  override def sendPacket(packetIn: Packet[_ <: INetHandler], listener: GenericFutureListener[_ <: Future[_ >: Void]], listeners: GenericFutureListener[_ <: Future[_ >: Void]]*): Unit = {}
}

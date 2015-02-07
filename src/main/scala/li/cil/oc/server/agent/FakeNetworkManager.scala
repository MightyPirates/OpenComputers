package li.cil.oc.server.agent

import io.netty.util.concurrent.Future
import io.netty.util.concurrent.GenericFutureListener
import net.minecraft.network.NetworkManager
import net.minecraft.network.Packet

object FakeNetworkManager extends NetworkManager(false) {
  override def scheduleOutboundPacket(packet: Packet, listener: GenericFutureListener[_ <: Future[_]]*) {}
}

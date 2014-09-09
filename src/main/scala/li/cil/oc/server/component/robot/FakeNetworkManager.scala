package li.cil.oc.server.component.robot

import io.netty.util.concurrent.{Future, GenericFutureListener}
import net.minecraft.network.{NetworkManager, Packet}

object FakeNetworkManager extends NetworkManager(false) {
  override def scheduleOutboundPacket(packet: Packet, listener: GenericFutureListener[_ <: Future[_]]*) {}
}

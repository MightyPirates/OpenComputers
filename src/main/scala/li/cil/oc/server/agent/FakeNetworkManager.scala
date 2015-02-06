package li.cil.oc.server.agent

import io.netty.util.concurrent.Future
import io.netty.util.concurrent.GenericFutureListener
import net.minecraft.network.EnumPacketDirection
import net.minecraft.network.NetworkManager
import net.minecraft.network.Packet

object FakeNetworkManager extends NetworkManager(EnumPacketDirection.CLIENTBOUND) {
  override def sendPacket(packetIn: Packet) {}

  override def sendPacket(packetIn: Packet, listener: GenericFutureListener[_ <: Future[_]], listeners: GenericFutureListener[_ <: Future[_]]*) {}
}

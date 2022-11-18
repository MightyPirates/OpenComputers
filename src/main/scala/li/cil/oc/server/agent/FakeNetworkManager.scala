package li.cil.oc.server.agent

import io.netty.util.concurrent.Future
import io.netty.util.concurrent.GenericFutureListener
import net.minecraft.network.NetworkManager
import net.minecraft.network.IPacket
import net.minecraft.network.PacketDirection

object FakeNetworkManager extends NetworkManager(PacketDirection.CLIENTBOUND) {
  override def send(packetIn: IPacket[_]): Unit = {}

  override def send(packetIn: IPacket[_], listener: GenericFutureListener[_ <: Future[_ >: Void]]): Unit = {}
}

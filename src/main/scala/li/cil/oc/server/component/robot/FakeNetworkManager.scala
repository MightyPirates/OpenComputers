package li.cil.oc.server.component.robot

import java.net.InetSocketAddress

import cpw.mods.fml.relauncher.{Side, SideOnly}
import net.minecraft.network.INetworkManager
import net.minecraft.network.packet.{NetHandler, Packet}

object FakeNetworkManager extends INetworkManager {
  private val Address = new InetSocketAddress("127.0.0.1", 0)

  override def setNetHandler(handler: NetHandler) {}

  override def addToSendQueue(packet: Packet) {}

  override def wakeThreads() {}

  override def processReadPackets() {}

  override def getSocketAddress = Address

  override def serverShutdown() {}

  override def packetSize() = 0

  override def networkShutdown(reason: String, params: AnyRef*) {}

  @SideOnly(Side.CLIENT)
  override def closeConnections() {}
}

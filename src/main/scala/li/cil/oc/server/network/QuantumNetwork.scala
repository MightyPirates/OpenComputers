package li.cil.oc.server.network

import li.cil.oc.api.network.Packet

import scala.collection.mutable

// Just because the name is so fancy!
object QuantumNetwork {
  val tunnels = mutable.Map.empty[String, mutable.WeakHashMap[QuantumNode, Unit]]

  def add(card: QuantumNode) {
    tunnels.getOrElseUpdate(card.tunnel, mutable.WeakHashMap.empty).put(card, Unit)
  }

  def remove(card: QuantumNode) {
    tunnels.get(card.tunnel).foreach(_.remove(card))
  }

  def getEndpoints(tunnel: String) = tunnels.get(tunnel).fold(Iterable.empty[QuantumNode])(_.keys)

  trait QuantumNode {
    def tunnel: String

    def receivePacket(packet: Packet): Unit
  }

}

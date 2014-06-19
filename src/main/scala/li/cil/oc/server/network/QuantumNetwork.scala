package li.cil.oc.server.network

import li.cil.oc.server.component.LinkedCard

import scala.collection.mutable

// Just because the name if so fancy!
object QuantumNetwork {
  val tunnels = mutable.Map.empty[String, mutable.WeakHashMap[LinkedCard, Unit]]

  def add(card: LinkedCard) {
    tunnels.getOrElseUpdate(card.tunnel, mutable.WeakHashMap.empty).put(card, Unit)
  }

  def remove(card: LinkedCard) {
    tunnels.get(card.tunnel).foreach(_.remove(card))
  }

  def getEndpoints(tunnel: String) = tunnels.get(tunnel).fold(Iterable.empty[LinkedCard])(_.keys)
}

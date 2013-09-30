package li.cil.oc.server.component

import li.cil.oc.api.Network
import li.cil.oc.api.network.{Message, Visibility, Node}

trait ItemComponent extends Node {
  override def visibility = Visibility.Neighbors

  override def receive(message: Message) = {
    super.receive(message)
    network match {
      case None => None
      case Some(net) =>
        if (net.neighbors(this).exists(_ == message.source)) receiveFromNeighbor(net, message)
        else None
    }
  }

  protected def receiveFromNeighbor(network: Network, message: Message): Option[Array[Any]] = None
}

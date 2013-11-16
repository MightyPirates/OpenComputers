package li.cil.oc.server.network

import li.cil.oc.api.network.{Environment, Visibility, Node => ImmutableNode}
import li.cil.oc.util.Persistable
import li.cil.oc.{Config, api}
import net.minecraft.nbt.NBTTagCompound
import scala.collection.convert.WrapAsJava._
import scala.collection.convert.WrapAsScala._

trait Node extends api.network.Node with Persistable {
  val host: Environment
  val reachability: Visibility

  final var address: String = null

  final var network: api.network.Network = null

  def canBeReachedFrom(other: ImmutableNode) = reachability match {
    case Visibility.None => false
    case Visibility.Neighbors => isNeighborOf(other)
    case Visibility.Network => isInSameNetwork(other)
  }

  def isNeighborOf(other: ImmutableNode) =
    isInSameNetwork(other) && network.neighbors(this).exists(_ == other)

  def reachableNodes: java.lang.Iterable[ImmutableNode] =
    if (network == null) Iterable.empty[ImmutableNode].toSeq
    else network.nodes(this)

  def neighbors: java.lang.Iterable[ImmutableNode] =
    if (network == null) Iterable.empty[ImmutableNode].toSeq
    else network.neighbors(this)

  def connect(node: ImmutableNode) = network.connect(this, node)

  def disconnect(node: ImmutableNode) =
    if (network != null) network.disconnect(this, node)

  def remove() = if (network != null) network.remove(this)

  def sendToAddress(target: String, name: String, data: AnyRef*) =
    if (network != null) network.sendToAddress(this, target, name, data: _*)

  def sendToNeighbors(name: String, data: AnyRef*) =
    if (network != null) network.sendToNeighbors(this, name, data: _*)

  def sendToReachable(name: String, data: AnyRef*) =
    if (network != null) network.sendToReachable(this, name, data: _*)

  private def isInSameNetwork(other: ImmutableNode) = network != null && network == other.network

  // ----------------------------------------------------------------------- //

  def onConnect(node: ImmutableNode) {
    host.onConnect(node)
  }

  def onDisconnect(node: ImmutableNode) {
    host.onDisconnect(node)
  }

  // ----------------------------------------------------------------------- //

  override def load(nbt: NBTTagCompound) = {
    super.load(nbt)
    if (nbt.hasKey(Config.namespace + "node.address")) {
      address = nbt.getString(Config.namespace + "node.address")
    }
  }

  override def save(nbt: NBTTagCompound) = {
    super.save(nbt)
    if (address != null) {
      nbt.setString(Config.namespace + "node.address", address)
    }
  }
}
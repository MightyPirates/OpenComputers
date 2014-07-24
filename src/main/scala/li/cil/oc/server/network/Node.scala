package li.cil.oc.server.network

import li.cil.oc.api.network.{Environment, Visibility, Node => ImmutableNode}
import li.cil.oc.{OpenComputers, api}
import net.minecraft.nbt.NBTTagCompound

import scala.collection.convert.WrapAsJava._
import scala.collection.convert.WrapAsScala._

trait Node extends ImmutableNode {
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
    if (network != null && isInSameNetwork(node)) network.disconnect(this, node)

  def remove() = if (network != null) network.remove(this)

  private def isInSameNetwork(other: ImmutableNode) = network != null && network == other.network

  // ----------------------------------------------------------------------- //

  def onConnect(node: ImmutableNode) {
    try {
      host.onConnect(node)
    } catch {
      case e: Throwable => OpenComputers.log.warn("A component of type '%s' threw an error while being connected to the component network.".format(host.getClass.getName), e)
    }
  }

  def onDisconnect(node: ImmutableNode) {
    try {
      host.onDisconnect(node)
    } catch {
      case e: Throwable => OpenComputers.log.warn("A component of type '%s' threw an error while being disconnected from the component network.".format(host.getClass.getName), e)
    }
  }

  // ----------------------------------------------------------------------- //

  def load(nbt: NBTTagCompound) = {
    if (nbt.hasKey("address")) {
      val oldAddress = address
      address = nbt.getString("address")
      if (address != oldAddress) network match {
        case wrapper: Network.Wrapper => wrapper.network.remap(this)
        case _ =>
      }
    }
  }

  def save(nbt: NBTTagCompound) = {
    if (address != null) {
      nbt.setString("address", address)
    }
  }
}

// We have to mixin the vararg methods individually in the actual
// implementations of the different node variants (see Network class) because
// for some reason it fails compiling on Linux otherwise (no clue why).
trait NodeVarargPart extends ImmutableNode {
  def sendToAddress(target: String, name: String, data: AnyRef*) =
    if (network != null) network.sendToAddress(this, target, name, data: _*)

  def sendToNeighbors(name: String, data: AnyRef*) =
    if (network != null) network.sendToNeighbors(this, name, data: _*)

  def sendToReachable(name: String, data: AnyRef*) =
    if (network != null) network.sendToReachable(this, name, data: _*)

  def sendToVisible(name: String, data: AnyRef*) =
    if (network != null) network.sendToVisible(this, name, data: _*)
}
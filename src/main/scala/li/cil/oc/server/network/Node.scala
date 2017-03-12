package li.cil.oc.server.network

import com.google.common.base.Strings
import li.cil.oc.OpenComputers
import li.cil.oc.api
import li.cil.oc.api.network.{NodeContainer, NodeHost, Visibility, Node => ImmutableNode}
import net.minecraft.nbt.NBTTagCompound

import scala.collection.convert.WrapAsJava._
import scala.collection.convert.WrapAsScala._

trait Node extends ImmutableNode {
  def host: NodeContainer

  def reachability: Visibility

  final var getAddress: String = null

  final var getNetwork: api.network.Network = null

  def canBeReachedFrom(other: ImmutableNode) = getReachability match {
    case Visibility.NONE => false
    case Visibility.NEIGHBORS => isNeighborOf(other)
    case Visibility.NETWORK => isInSameNetwork(other)
  }

  def isNeighborOf(other: ImmutableNode) =
    isInSameNetwork(other) && getNetwork.neighbors(this).exists(_ == other)

  def getReachableNodes: java.lang.Iterable[ImmutableNode] =
    if (getNetwork == null) Iterable.empty[ImmutableNode].toSeq
    else getNetwork.nodes(this)

  def getNeighbors: java.lang.Iterable[ImmutableNode] =
    if (getNetwork == null) Iterable.empty[ImmutableNode].toSeq
    else getNetwork.neighbors(this)

  def connect(node: ImmutableNode) = getNetwork.connect(this, node)

  def disconnect(node: ImmutableNode) =
    if (getNetwork != null && isInSameNetwork(node)) getNetwork.disconnect(this, node)

  def remove() = if (getNetwork != null) getNetwork.remove(this)

  private def isInSameNetwork(other: ImmutableNode) = getNetwork != null && other != null && getNetwork == other.getNetwork

  // ----------------------------------------------------------------------- //

  def onConnect(node: ImmutableNode) {
    try {
      getContainer.onConnect(node)
    } catch {
      case e: Throwable => OpenComputers.log.warn(s"A component of type '${getContainer.getClass.getName}' threw an error while being connected to the component network.", e)
    }
  }

  def onDisconnect(node: ImmutableNode) {
    try {
      getContainer.onDisconnect(node)
    } catch {
      case e: Throwable => OpenComputers.log.warn(s"A component of type '${getContainer.getClass.getName}' threw an error while being disconnected from the component network.", e)
    }
  }

  // ----------------------------------------------------------------------- //

  def load(nbt: NBTTagCompound) = {
    if (nbt.hasKey("address")) {
      val newAddress = nbt.getString("address")
      if (!Strings.isNullOrEmpty(newAddress) && newAddress != getAddress) getNetwork match {
        case wrapper: Network.Wrapper => wrapper.network.remap(this, newAddress)
        case _ => getAddress = newAddress
      }
    }
  }

  def save(nbt: NBTTagCompound) = {
    if (getAddress != null) {
      nbt.setString("address", getAddress)
    }
  }

  override def toString = s"Node($getAddress, $getContainer)"
}

// We have to mixin the vararg methods individually in the actual
// implementations of the different node variants (see Network class) because
// for some reason it fails compiling on Linux otherwise (no clue why).
trait NodeVarargPart extends ImmutableNode {
  def sendToAddress(target: String, name: String, data: AnyRef*) =
    if (getNetwork != null) getNetwork.sendToAddress(this, target, name, data: _*)

  def sendToNeighbors(name: String, data: AnyRef*) =
    if (getNetwork != null) getNetwork.sendToNeighbors(this, name, data: _*)

  def sendToReachable(name: String, data: AnyRef*) =
    if (getNetwork != null) getNetwork.sendToReachable(this, name, data: _*)

  def sendToVisible(name: String, data: AnyRef*) =
    if (getNetwork != null) getNetwork.sendToVisible(this, name, data: _*)
}
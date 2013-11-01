package li.cil.oc.server.network

import li.cil.oc.api
import li.cil.oc.api.network.{Environment, Visibility, Node => ImmutableNode}
import net.minecraft.nbt.NBTTagCompound
import scala.collection.convert.WrapAsScala._

class Node(val host: Environment, val name: String, val reachability: Visibility) extends api.network.Node {
  final var address: String = null

  final var network: api.network.Network = null

  def canBeReachedFrom(other: ImmutableNode) = reachability match {
    case Visibility.None => false
    case Visibility.Neighbors => isNeighborOf(other)
    case Visibility.Network => isInSameNetwork(other)
  }

  def isNeighborOf(other: ImmutableNode) =
    isInSameNetwork(other) && network.neighbors(this).exists(_ == other)

  private def isInSameNetwork(other: ImmutableNode) =
    network != null && network == other.network

  // ----------------------------------------------------------------------- //

  def load(nbt: NBTTagCompound) = {
    if (nbt.hasKey("oc.node.address"))
      address = nbt.getString("oc.node.address")
  }

  def save(nbt: NBTTagCompound) = {
    if (address != null)
      nbt.setString("oc.node.address", address)
  }
}
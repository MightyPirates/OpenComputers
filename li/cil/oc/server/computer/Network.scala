package li.cil.oc.server.computer

import li.cil.oc.api.INetwork
import li.cil.oc.api.INetworkMessage
import li.cil.oc.api.INetworkNode
import net.minecraft.block.Block
import net.minecraft.tileentity.TileEntity
import net.minecraft.world.IBlockAccess
import net.minecraftforge.common.ForgeDirection
import scala.beans.BeanProperty
import scala.collection.mutable
import scala.collection.mutable.ArrayBuffer

/**
 * Network implementation for component networks.
 *
 * This network interconnects components in a geometry-agnostic fashion. It
 * builds an internal graph of network nodes and the connections between them,
 * and takes care of merges when adding connections, as well as net splits on
 * node removal.
 *
 * It keeps the list of nodes as a lookup table for fast id->node resolving.
 * Note that it is possible for multiple nodes to have the same ID, though.
 */
class Network private(private val nodes: mutable.Map[Int, ArrayBuffer[Network.Node]]) extends INetwork {
  def this(node: INetworkNode) = {
    this(mutable.Map({
      node.address = 1
      node.address -> ArrayBuffer(new Network.Node(node))
    }))
  }

  /** Do not allow modification of the network while it's updating. */
  private var locked = false

  nodes.values.flatten.foreach(_.data.network = this)

  def connect(nodeA: INetworkNode, nodeB: INetworkNode) = try {
    if (locked) throw new IllegalStateException(
      "Cannot modify network while it is already updating its structure.")
    locked = true

    val containsA = nodes.get(nodeA.address).exists(_.exists(_.data == nodeA))
    val containsB = nodes.get(nodeB.address).exists(_.exists(_.data == nodeB))
    if (!containsA && !containsB) throw new IllegalArgumentException(
      "At least one of the nodes must already be in this network.")

    if (containsA && containsB) {
      // Both nodes already exist in the network but there is a new connection.
      // This can happen if a new node sequentially connects to multiple nodes
      // in an existing network, e.g. in a setup like so:
      // O O   Where O is an old node, and N is the new Node. It would connect
      // O N   to the node above and left to it (in no particular order).
      val oldNodeA = nodes(nodeA.address).find(_.data == nodeA).get
      val oldNodeB = nodes(nodeB.address).find(_.data == nodeB).get
      if (!oldNodeA.edges.exists(edge => edge.other(oldNodeA) == oldNodeB)) {
        val edge = new Network.Edge(oldNodeA, oldNodeB)
        oldNodeA.edges += edge
        oldNodeB.edges += edge
        true
      }
      // That connection already exists.
      else false
    }
    // New node for this network, order the nodes and add the new one.
    else if (containsA) add(nodes(nodeA.address).find(_.data == nodeA).get, nodeB)
    else add(nodes(nodeB.address).find(_.data == nodeB).get, nodeA)
  }
  finally {
    locked = false
  }

  def remove(node: INetworkNode) = nodes.get(node.address) match {
    case None => false
    case Some(list) => list.find(_.data == node) match {
      case None => false
      case Some(entry) => if (list.contains(entry)) {
        if ((list -= entry).isEmpty)
          nodes -= node.address
        node.network = null
        entry.remove().foreach(_.sendToAll(node, "network.disconnect"))
        sendToAll(node, "network.disconnect")
        true
      }
      else false
    }
  }

  def sendToNode(source: INetworkNode, target: Int, name: String, data: Any*) =
    nodes.get(target) match {
      case None => None
      case Some(list) => send(new Network.Message(source, name, Array(data: _*)), list.map(_.data).iterator)
    }

  def sendToAll(source: INetworkNode, name: String, data: Any*) =
    send(new Network.Message(source, name, Array(data: _*)), nodes.values.flatten.map(_.data).iterator)

  private def send(message: Network.Message, nodes: Iterator[INetworkNode]) = {
    var result = None: Option[Array[Any]]
    while (!message.isCanceled && nodes.hasNext) {
      nodes.next().receive(message) match {
        case None => // Ignore.
        case r => result = r
      }
    }
    result
  }

  private def add(oldNode: Network.Node, node: INetworkNode) = {
    // The node is new to this network, check if we have to merge networks.
    val newNode = if (node.network == null) {
      // Other node is not yet in a network, create internal node and add it
      // to our lookup table of internal nodes.
      val newNode = new Network.Node(node)
      node.address = findId()
      nodes.getOrElseUpdate(node.address, new ArrayBuffer[Network.Node]) += newNode
      node.network = this
      sendToAll(node, "network.connect")
      newNode
    }
    else {
      val otherNetwork = node.network.asInstanceOf[Network]
      // We have to merge. First create a copy of the old nodes to have the
      // list of nodes to which to send "network.connect" messages.
      val oldNodes = nodes.values.flatten.map(_.data).toArray
      // Then get the list of nodes in the other network. This is, among the
      // iteration to merge into this network, used to send "network.reconnect"
      // messages to old nodes in case we have to change a node's address to
      // ensure unique addresses in the merged network.
      val otherNodes = otherNetwork.nodes.values.flatten.map(_.data)
      // Pre-merge step: ensure addresses are unique.
      for (node <- otherNodes if nodes.contains(node.address)) {
        val oldAddress = node.address
        node.address = findId(otherNetwork)
        if (node.address != oldAddress) {
          // If we successfully changed the address send message.
          send(new Network.Message(node, "network.reconnect", Array(int2Integer(oldAddress))), otherNodes.iterator)
        }
      }
      // Merge step: add nodes from other network into this network.
      for (node <- otherNetwork.nodes.values.flatten) {
        nodes.getOrElseUpdate(node.data.address, new ArrayBuffer[Network.Node]) += node
        node.data.network = this
        send(new Network.Message(node.data, "network.connect"), oldNodes.iterator)
      }
      // Return the node object of the newly connected node for the next step.
      nodes(node.address).find(_.data == node).get
    }
    // Either way, add the connection between the two nodes.
    val edge = new Network.Edge(oldNode, newNode)
    oldNode.edges += edge
    newNode.edges += edge
    true
  }

  private def findId() = Range(1, Int.MaxValue).find(!nodes.contains(_)).get

  private def findId(other: Network) = Range(1, Int.MaxValue).find(
    address => !nodes.contains(address) && !other.nodes.contains(address)).get
}

object Network {

  def joinOrCreateNetwork(world: IBlockAccess, x: Int, y: Int, z: Int): Unit =
    getNetworkNode(world, x, y, z) match {
      case None => // Invalid block.
      case Some(node) => {
        for (side <- ForgeDirection.VALID_DIRECTIONS) {
          getNetworkNode(world, x + side.offsetX, y + side.offsetY, z + side.offsetZ) match {
            case None => // Ignore.
            case Some(neighborNode) =>
              if (neighborNode != null && neighborNode.network != null) {
                neighborNode.network.connect(neighborNode, node)
              }
          }
        }
        if (node.network == null) new Network(node)
      }
    }

  private def getNetworkNode(world: IBlockAccess, x: Int, y: Int, z: Int): Option[TileEntity with INetworkNode] =
    Option(Block.blocksList(world.getBlockId(x, y, z))) match {
      case Some(block) if block.hasTileEntity(world.getBlockMetadata(x, y, z)) =>
        world.getBlockTileEntity(x, y, z) match {
          case tileEntity: TileEntity with INetworkNode => Some(tileEntity)
          case _ => None
        }
      case _ => None
    }

  private class Node(val data: INetworkNode) {
    val edges = ArrayBuffer.empty[Edge]

    def remove() = {
      // Remove self from neighbors.
      for (edge <- edges) {
        edge.other(this).edges -= edge
      }
      // Build neighbor graphs to see if our removal resulted in a split.
      val subGraphs = mutable.MutableList.empty[(mutable.Map[Int, ArrayBuffer[Node]], mutable.Queue[Node])]
      for (edge <- edges) {
        val other = edge.other(this)
        subGraphs += ((mutable.Map(other.data.address -> ArrayBuffer(other)), mutable.Queue(other.edges.map(_.other(other)): _*)))
      }
      // Breadth-first search to make early merges more likely.
      while (!subGraphs.forall {
        case (_, queue) => queue.isEmpty
      }) {
        for (subGraph <- subGraphs.filter {
          case (_, queue) => !queue.isEmpty
        }) {
          val (nodes, queue) = subGraph
          val node = queue.dequeue()
          // See if the node is already in some other graph, in which case we
          // merge this graph into the other graph.
          if (!subGraphs.filter(_ != subGraph).exists(otherSubGraph => {
            val (otherNodes, _) = otherSubGraph
            otherNodes.get(node.data.address) match {
              case Some(list) if list.contains(node) => {
                // Merge.
                otherNodes ++= nodes
                nodes.clear()
                queue.clear()
                true
              }
              case _ => false
            }
          })) {
            // Not in any other graph yet.
            nodes.getOrElseUpdate(node.data.address, new ArrayBuffer[Network.Node]) += node
            // Add nodes this node is connected to to the queue if they're not
            // already in this graph.
            queue ++= node.edges.map(_.other(node)).
              filter(node => !nodes.get(node.data.address).exists(_.contains(node)))
          }
        }
      }
      // Create new sub networks for separated sub-networks. Skip the first one
      // to re-use the originating network and avoid re-creation if there is no
      // split at all.
      subGraphs map (_._1) filter (!_.isEmpty) drop 1 map (new Network(_))
    }
  }

  private class Edge(val left: Node, val right: Node) {
    def other(side: Node) = if (side == left) right else left
  }

  private class Message(@BeanProperty val source: INetworkNode,
                        @BeanProperty val name: String,
                        @BeanProperty val data: Array[Any] = Array()) extends INetworkMessage {
    var isCanceled = false

    def cancel() = isCanceled = true
  }

}
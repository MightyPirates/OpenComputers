package li.cil.oc.server.computer

import java.util.logging.Level
import li.cil.oc.OpenComputers
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
class Network private(private val nodeMap: mutable.Map[Int, ArrayBuffer[Network.Node]]) extends INetwork {
  def this(node: INetworkNode) = {
    this(mutable.Map({
      node.address = 1
      node.address -> ArrayBuffer(new Network.Node(node))
    }))
    Network.send(new Network.ConnectMessage(node), List(node))
  }

  nodes.foreach(_.network = this)

  def connect(nodeA: INetworkNode, nodeB: INetworkNode) = {
    val containsA = nodeMap.get(nodeA.address).exists(_.exists(_.data == nodeA))
    val containsB = nodeMap.get(nodeB.address).exists(_.exists(_.data == nodeB))
    if (!containsA && !containsB) throw new IllegalArgumentException(
      "At least one of the nodes must already be in this network.")

    def oldNodeA = nodeMap(nodeA.address).find(_.data == nodeA).get
    def oldNodeB = nodeMap(nodeB.address).find(_.data == nodeB).get
    if (containsA && containsB) {
      // Both nodes already exist in the network but there is a new connection.
      // This can happen if a new node sequentially connects to multiple nodes
      // in an existing network, e.g. in a setup like so:
      // O O   Where O is an old node, and N is the new Node. It would connect
      // O N   to the node above and left to it (in no particular order).
      if (!oldNodeA.edges.exists(_.isBetween(oldNodeA, oldNodeB))) {
        assert(!oldNodeB.edges.exists(_.isBetween(oldNodeA, oldNodeB)))
        Network.Edge(oldNodeA, oldNodeB)
        true
      }
      // That connection already exists.
      else false
    }
    // New node for this network, order the nodes and add the new one.
    else if (containsA) add(oldNodeA, nodeB)
    else add(oldNodeB, nodeA)
  }

  private def add(oldNode: Network.Node, addedNode: INetworkNode) = {
    // Check if the other node is new or if we have to merge networks.
    val (newNode, sendQueue) = if (addedNode.network == null) {
      val sendQueue = mutable.Buffer.empty[(Network.Message, Iterable[INetworkNode])]
      sendQueue += ((new Network.ConnectMessage(addedNode), List(addedNode) ++ nodes))
      nodes.foreach(node => sendQueue += ((new Network.ConnectMessage(node), List(addedNode))))
      val newNode = new Network.Node(addedNode)
      if (nodeMap.contains(addedNode.address) || addedNode.address < 1)
        addedNode.address = findId() // Assign address first since it may be ignored.
      nodeMap.getOrElseUpdate(addedNode.address, new ArrayBuffer[Network.Node]) += newNode
      addedNode.network = this
      (newNode, sendQueue)
    }
    else {
      // Queue any messages to avoid side effects from receivers.
      val sendQueue = mutable.Buffer.empty[(Network.Message, Iterable[INetworkNode])]
      val thisNodes = nodes.toBuffer
      val otherNetwork = addedNode.network.asInstanceOf[Network]
      val otherNodes = otherNetwork.nodes.toBuffer
      otherNodes.foreach(node => sendQueue += ((new Network.ConnectMessage(node), thisNodes)))
      thisNodes.foreach(node => sendQueue += ((new Network.ConnectMessage(node), otherNodes)))

      // Change addresses for conflicting nodes in other network.
      val reserved = mutable.Set(otherNetwork.nodeMap.keySet.toSeq: _*)
      otherNodes.filter(node => nodeMap.contains(node.address)).foreach(node => {
        val oldAddress = node.address
        node.address = findId(reserved)
        if (node.address != oldAddress) {
          reserved += node.address
          // Prepend to notify old nodes of address changes first.
          sendQueue.+=:((new Network.ReconnectMessage(node, oldAddress), otherNodes))
        }
      })

      // Add nodes from other network into this network.
      otherNetwork.nodeMap.values.flatten.foreach(node => {
        nodeMap.getOrElseUpdate(node.data.address, new ArrayBuffer[Network.Node]) += node
        node.data.network = this
      })

      // Return the node object of the newly connected node for the next step.
      (nodeMap(addedNode.address).find(_.data == addedNode).get, sendQueue)
    }

    // Add the connection between the two nodes.
    Network.Edge(oldNode, newNode)

    // Send all generated messages.
    for ((message, nodes) <- sendQueue) Network.send(message, nodes)

    true
  }

  def disconnect(nodeA: INetworkNode, nodeB: INetworkNode) = {
    val containsA = nodeMap.get(nodeA.address).exists(_.exists(_.data == nodeA))
    val containsB = nodeMap.get(nodeB.address).exists(_.exists(_.data == nodeB))
    if (!containsA || !containsB) throw new IllegalArgumentException(
      "Both of the nodes must be in this network.")

    def oldNodeA = nodeMap(nodeA.address).find(_.data == nodeA).get
    def oldNodeB = nodeMap(nodeB.address).find(_.data == nodeB).get
    oldNodeA.edges.find(_.isBetween(oldNodeA, oldNodeB)) match {
      case None => false // That connection doesn't exists.
      case Some(edge) => {
        handleSplit(edge.remove())
        true
      }
    }
  }

  def remove(node: INetworkNode) = nodeMap.get(node.address) match {
    case None => false
    case Some(list) => list.find(_.data == node) match {
      case None => false
      case Some(entry) => {
        node.network = null

        // Removing a node may result in a net split, leaving us with multiple
        // networks. The remove function returns all resulting networks, one
        // of which we'll re-use for this network. For all additional ones we
        // create new network instances.
        handleSplit(entry.remove(), nodes => {
          nodes.foreach(n => Network.send(new Network.DisconnectMessage(n), List(node)))
          Network.send(new Network.DisconnectMessage(node), nodes)
        })
        Network.send(new Network.DisconnectMessage(node), List(node))
        true
      }
    }
  }

  private def handleSplit(subGraphs: Seq[mutable.Map[Int, ArrayBuffer[Network.Node]]],
                          messageCallback: Iterable[INetworkNode] => Unit = _ => {}) = {
    // Sending the removal messages can have side effects, so we'll keep a
    // copy of the original list of nodes in each sub network.
    val subNodes = subGraphs.map(_.values.flatten.map(_.data).toBuffer).toBuffer

    // We re-use this network by assigning the first sub graph to it. For
    // all additional sub graphs (if any) we'll have to create new ones.
    nodeMap.clear()
    // Empty for the last node removed from a network.
    if (!subGraphs.isEmpty) {
      nodeMap ++= subGraphs.head
      subGraphs.tail.foreach(new Network(_))
    }

    // Send removal messages. First, to the removed node itself (for its
    // onDisconnect handler), then one for the removed node to all sub
    // networks, for each node in the sub networks back to the removed node
    // and if there was a net split (we have multiple networks) also for
    // each node now longer belonging to one of the resulting sub networks.
    for (indexA <- 0 until subNodes.length) {
      val nodesA = subNodes(indexA)
      for (indexB <- (indexA + 1) until subNodes.length) {
        val nodesB = subNodes(indexB)
        nodesA.foreach(nodeA => Network.send(new Network.DisconnectMessage(nodeA), nodesB))
        nodesB.foreach(nodeB => Network.send(new Network.DisconnectMessage(nodeB), nodesA))
      }
      messageCallback(nodesA)
    }
  }

  def node(address: Int) = nodeMap.get(address) match {
    case None => None
    case Some(list) => Some(list.last.data)
  }

  def nodes = nodeMap.values.flatten.map(_.data)

  def sendToNode(source: INetworkNode, target: Int, name: String, data: Any*) =
    nodeMap.get(target) match {
      case None => None
      case Some(list) => Network.send(new Network.Message(source, name, Array(data: _*)), list.map(_.data))
    }

  def sendToAll(source: INetworkNode, name: String, data: Any*) =
    Network.send(new Network.Message(source, name, Array(data: _*)), nodes)

  private def findId() = Range(1, Int.MaxValue).find(!nodeMap.contains(_)).get

  private def findId(reserved: collection.Set[Int]) = Range(1, Int.MaxValue).find(
    address => !nodeMap.contains(address) && !reserved.contains(address)).get
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

  private def send(message: Network.Message, nodes: Iterable[INetworkNode]) = {
    //println("send(" + message.name + "(" + message.data.mkString(", ") + "): " + message.source.address + " -> [" + nodes.map(_.address).mkString(", ") + "])")
    val iterator = nodes.iterator
    var result = None: Option[Array[Any]]
    while (!message.isCanceled && iterator.hasNext) {
      try {
        iterator.next().receive(message) match {
          case None => // Ignore.
          case r => result = r
        }
      } catch {
        case e: Throwable => OpenComputers.log.log(Level.WARNING, "Error in message handler", e)
      }
    }
    result
  }

  private class Node(val data: INetworkNode) {
    val edges = ArrayBuffer.empty[Edge]

    def remove() = {
      val edgesCopy = edges.toBuffer
      edges.foreach(edge => edge.other(this).edges -= edge)
      edges.clear()
      edgesCopy.map(_.remove().filter(_.values.head.head != this)).flatten
    }
  }

  private case class Edge(left: Node, right: Node) {
    left.edges += this
    right.edges += this

    def other(side: Node) = if (side == left) right else left

    def isBetween(a: Node, b: Node) = (a == left && b == right) || (b == left && a == right)

    def remove() = {
      left.edges -= this
      right.edges -= this
      // Build neighbor graphs to see if our removal resulted in a split.
      val subGraphs = List(
        (mutable.Map(left.data.address -> ArrayBuffer(left)), mutable.Queue(left.edges.map(_.other(left)): _*)),
        (mutable.Map(right.data.address -> ArrayBuffer(right)), mutable.Queue(right.edges.map(_.other(right)): _*)))
      // Breadth-first search to make early merges more likely.
      while (!subGraphs.forall {
        case (_, queue) => queue.isEmpty
      }) for (subGraph <- subGraphs.filter {
        case (_, queue) => !queue.isEmpty
      }) {
        val (nodes, queue) = subGraph
        val node = queue.dequeue()
        // See if the node is already in some other graph, in which case we
        // merge this graph into the other graph.
        if (!subGraphs.filter(_ != subGraph).exists {
          case (otherNodes, otherQueue) => otherNodes.get(node.data.address) match {
            case Some(list) if list.contains(node) => {
              otherNodes ++= nodes
              otherQueue ++= queue
              nodes.clear()
              queue.clear()
              true
            }
            case _ => false
          }
        }) {
          nodes.getOrElseUpdate(node.data.address, new ArrayBuffer[Network.Node]) += node
          queue ++= node.edges.map(_.other(node)).filter(n => !nodes.get(n.data.address).exists(_.contains(n)))
        }
      }
      subGraphs map (_._1) filter (!_.isEmpty)
    }
  }

  private class Message(@BeanProperty val source: INetworkNode,
                        @BeanProperty val name: String,
                        @BeanProperty val data: Array[Any] = Array()) extends INetworkMessage {
    var isCanceled = false

    def cancel() = isCanceled = true
  }

  private class ConnectMessage(source: INetworkNode) extends Message(source, "network.connect")

  private class DisconnectMessage(source: INetworkNode) extends Message(source, "network.disconnect")

  private class ReconnectMessage(source: INetworkNode, oldAddress: Int) extends Message(source, "network.reconnect", Array(oldAddress.asInstanceOf[Any]))

}
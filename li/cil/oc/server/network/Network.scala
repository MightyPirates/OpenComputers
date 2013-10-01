package li.cil.oc.server.network

import _root_.net.minecraft.block.Block
import _root_.net.minecraft.tileentity.TileEntity
import _root_.net.minecraft.world.{IBlockAccess, World}
import _root_.net.minecraftforge.common.ForgeDirection
import _root_.net.minecraftforge.event.ForgeSubscribe
import _root_.net.minecraftforge.event.world.ChunkEvent
import java.util.logging.Level
import li.cil.oc.api.network.Visibility
import li.cil.oc.api.{network => net}
import li.cil.oc.server.network.Network.Node
import li.cil.oc.{api, OpenComputers}
import scala.beans.BeanProperty
import scala.collection.JavaConverters._
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
class Network private(private val nodeMap: mutable.Map[Int, ArrayBuffer[Network.Node]]) extends api.Network {
  def this(node: net.Node) = {
    this(mutable.Map({
      if (node.address < 1)
        node.address = 1
      node.address -> ArrayBuffer(new Network.Node(node))
    }))
    send(new Network.ConnectMessage(node), List(node))
  }

  nodes.foreach(_.network = Some(this))

  def connect(nodeA: net.Node, nodeB: net.Node) = {
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

  private def add(oldNode: Network.Node, addedNode: net.Node) = {
    // Check if the other node is new or if we have to merge networks.
    val (newNode, sendQueue) = if (addedNode.network.isEmpty) {
      val newNode = new Network.Node(addedNode)
      if (nodeMap.contains(addedNode.address) || addedNode.address < 1)
        addedNode.address = findId()
      // Store everything with an invalid address in slot zero.
      val address = addedNode.address match {
        case a if a > 0 => a
        case _ => 0
      }
      // Create the message queue. The address check is purely for performance,
      // since we can skip all that if the node is non-valid.
      val sendQueue = mutable.Buffer.empty[(Network.Message, Iterable[net.Node])]
      if (address > 0 && addedNode.visibility != Visibility.None) {
        sendQueue += ((new Network.ConnectMessage(addedNode), List(addedNode) ++ nodes))
        nodes.foreach(node => sendQueue += ((new Network.ConnectMessage(node), List(addedNode))))
      }
      nodeMap.getOrElseUpdate(address, new ArrayBuffer[Network.Node]) += newNode
      addedNode.network = Some(this)
      (newNode, sendQueue)
    }
    else {
      // Queue any messages to avoid side effects from receivers.
      val sendQueue = mutable.Buffer.empty[(Network.Message, Iterable[net.Node])]
      val thisNodes = nodes.toBuffer
      val otherNetwork = addedNode.network.get.asInstanceOf[Network]
      val otherNodes = otherNetwork.nodes.toBuffer
      otherNodes.foreach(node => sendQueue += ((Network.ConnectMessage(node), thisNodes)))
      thisNodes.foreach(node => sendQueue += ((Network.ConnectMessage(node), otherNodes)))

      // Change addresses for conflicting nodes in other network. We can queue
      // these messages because we're storing references to the nodes, so they
      // will send the change notification to the right node even if that node
      // also changes its address.
      val reserved = mutable.Set(otherNetwork.nodeMap.keySet.toSeq: _*)
      otherNodes.filter(node => nodeMap.contains(node.address)).foreach(node => {
        val oldAddress = node.address
        node.address = findId(reserved)
        if (node.address != oldAddress) {
          reserved += node.address
          // Prepend to notify old nodes of address changes first.
          sendQueue.+=:((Network.ReconnectMessage(node, oldAddress), otherNodes))
        }
      })

      // Add nodes from other network into this network, including invalid nodes.
      otherNetwork.nodeMap.values.flatten.foreach(node => {
        nodeMap.getOrElseUpdate(node.data.address, new ArrayBuffer[Network.Node]) += node
        node.data.network = Some(this)
      })

      // Return the node object of the newly connected node for the next step.
      (nodeMap(addedNode.address).find(_.data == addedNode).get, sendQueue)
    }

    // Add the connection between the two nodes.
    Network.Edge(oldNode, newNode)

    // Send all generated messages.
    for ((message, nodes) <- sendQueue) send(message, nodes)

    true
  }

  def reconnect(node: net.Node, address: Int): Boolean = {
    if (!nodeMap.get(node.address).exists(_.exists(_.data == node))) throw new IllegalArgumentException(
      "The node must already be in this network.")

    val oldAddress = node.address
    if (address == oldAddress) false
    else {
      val otherMessage = if (address < 1) {
        node.address = 0
        None
      }
      else {
        // Check if there's a simple collision, if so resolve it.
        nodeMap.get(address) match {
          case None =>
            // No collision.
            node.address = address
            None
          case Some(otherList) =>
            if (otherList.size > 1)
              return false // Already multiple nodes with that address...
            else {
              // Simple collision.
              val other = otherList.head
              otherList -= other

              other.data.address = findId()
              nodeMap.getOrElseUpdate(other.data.address, new mutable.ArrayBuffer[Node]) += other
              Some((Network.ReconnectMessage(other.data, address), nodes))
            }
        }
      }

      val oldList = nodeMap(oldAddress)
      val innerNode = oldList.remove(oldList.indexWhere(_.data == node))
      if (oldList.isEmpty)
        nodeMap -= oldAddress
      nodeMap.getOrElseUpdate(node.address, new mutable.ArrayBuffer[Node]) += innerNode

      otherMessage.foreach {
        case (message, targets) => send(message, targets)
      }
      send(Network.ReconnectMessage(node, oldAddress), nodes)

      true
    }
  }

  def disconnect(nodeA: net.Node, nodeB: net.Node) = {
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

  def remove(node: net.Node) = nodeMap.get(node.address) match {
    case None => false
    case Some(list) => list.find(_.data == node) match {
      case None => false
      case Some(entry) => {
        node.network = None

        // Removing a node may result in a net split, leaving us with multiple
        // networks. The remove function returns all resulting networks, one
        // of which we'll re-use for this network. For all additional ones we
        // create new network instances.
        handleSplit(entry.remove(), nodes => {
          nodes.foreach(n => send(new Network.DisconnectMessage(n), List(node)))
          send(new Network.DisconnectMessage(node), nodes)
        })
        send(new Network.DisconnectMessage(node), List(node))
        true
      }
    }
  }

  private def handleSplit(subGraphs: Seq[mutable.Map[Int, ArrayBuffer[Network.Node]]],
                          messageCallback: Iterable[net.Node] => Unit = _ => {}) = {
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
        nodesA.foreach(nodeA => send(new Network.DisconnectMessage(nodeA), nodesB))
        nodesB.foreach(nodeB => send(new Network.DisconnectMessage(nodeB), nodesA))
      }
      messageCallback(nodesA)
    }
  }

  def node(address: Int) = nodeMap.get(address) match {
    case Some(list) if address > 0 => list.map(_.data).filter(_.visibility != Visibility.None).lastOption
    case _ => None
  }

  def nodes(reference: net.Node) = {
    val referenceNeighbors = neighbors(reference).toSet
    nodes.filter(node => node.visibility == Visibility.Network || referenceNeighbors.contains(node))
  }

  def nodes = nodeMap.filter(_._1 > 0).values.flatten.map(_.data).filter(_.visibility != Visibility.None)

  def neighbors(node: net.Node) = nodeMap.get(node.address) match {
    case None => throw new IllegalArgumentException("Node must be in this network.")
    case Some(list) => list.find(_.data == node) match {
      case None => throw new IllegalArgumentException("Node must be in this network.")
      case Some(n) => n.edges.map(_.other(n).data)
    }
  }

  def sendToAddress(source: net.Node, target: Int, name: String, data: Any*) =
    nodeMap.get(target) match {
      case None => None
      case Some(list) => send(new Network.Message(source, name, Array(data: _*)), list.map(_.data))
    }

  def sendToNeighbors(source: net.Node, name: String, data: Any*) =
    send(new Network.Message(source, name, Array(data: _*)), neighbors(source))

  def sendToAll(source: net.Node, name: String, data: Any*) =
    send(new Network.Message(source, name, Array(data: _*)), nodes)

  private def send(message: Network.Message, targets: Iterable[net.Node]) =
    if (message.source.address > 0 && message.source.visibility != Visibility.None) {
      def protectedSend(target: net.Node) = try {
        //println("receive(" + message.name + "(" + message.data.mkString(", ") + "): " + message.source.address + ":" + message.source.name + " -> " + target.address + ":" + target.name + ")")
        target.receive(message)
      } catch {
        case e: Throwable => OpenComputers.log.log(Level.WARNING, "Error in message handler", e); None
      }

      message match {
        case _@(Network.ConnectMessage(_) | Network.ReconnectMessage(_, _)) =>
          // Cannot be canceled but respects visibility.
          (message.source.visibility match {
            case Visibility.Neighbors =>
              // Note: the neighbors() call already filters out invalid nodes.
              val neighborSet = neighbors(message.source).toSet
              targets.filter(target => target == message.source || neighborSet.contains(target))
            case Visibility.Network =>
              targets.filter(_.address > 0).filter(_.visibility == Visibility.Network)
          }).foreach(protectedSend)
          None
        case _@Network.DisconnectMessage(_) =>
          // Cannot be canceled but ignores visibility (because it'd be a pain to implement this otherwise).
          targets.filter(_.address > 0).foreach(protectedSend)
          None
        case _ =>
          // Can be canceled but ignores visibility.
          var result = None: Option[Array[Any]]
          val iterator = targets.filter(_.address > 0).iterator
          while (!message.isCanceled && iterator.hasNext)
            protectedSend(iterator.next()) match {
              case None => // Ignore.
              case r => result = r
            }
          result
      }
    } else None

  private def findId(reserved: collection.Set[Int] = collection.Set.empty[Int]) = Range(1, Int.MaxValue).find(
    address => !nodeMap.contains(address) && !reserved.contains(address)).get
}

object Network extends api.detail.NetworkAPI {
  @ForgeSubscribe
  def onChunkUnload(e: ChunkEvent.Unload) =
    onUnload(e.world, e.getChunk.chunkTileEntityMap.values.asScala.map(_.asInstanceOf[TileEntity]))

  @ForgeSubscribe
  def onChunkLoad(e: ChunkEvent.Load) =
    onLoad(e.world, e.getChunk.chunkTileEntityMap.values.asScala.map(_.asInstanceOf[TileEntity]))

  private def onUnload(w: World, tileEntities: Iterable[TileEntity]) = if (!w.isRemote) {
    // TODO add a more efficient batch remove operation? something along the lines of if #remove > #nodes*factor remove all, re-add remaining?
    tileEntities.
      filter(_.isInstanceOf[net.Node]).
      map(_.asInstanceOf[net.Node]).
      foreach(t => t.network.foreach(_.remove(t)))
  }

  private def onLoad(w: World, tileEntities: Iterable[TileEntity]) = if (!w.isRemote) {
    tileEntities.foreach(t => joinOrCreateNetwork(w, t.xCoord, t.yCoord, t.zCoord))
  }

  def joinOrCreateNetwork(world: IBlockAccess, x: Int, y: Int, z: Int): Unit =
    getNetworkNode(world, x, y, z) match {
      case None => // Invalid block.
      case Some(node) => {
        for (side <- ForgeDirection.VALID_DIRECTIONS) {
          getNetworkNode(world, x + side.offsetX, y + side.offsetY, z + side.offsetZ) match {
            case None => // Ignore.
            case Some(neighborNode) =>
              if (neighborNode.network.isDefined) {
                neighborNode.network.foreach(_.connect(neighborNode, node))
              }
          }
        }
        if (node.network.isEmpty) new Network(node)
      }
    }

  private def getNetworkNode(world: IBlockAccess, x: Int, y: Int, z: Int): Option[TileEntity with net.Node] =
    Option(Block.blocksList(world.getBlockId(x, y, z))) match {
      case Some(block) if block.hasTileEntity(world.getBlockMetadata(x, y, z)) =>
        world.getBlockTileEntity(x, y, z) match {
          case tileEntity: TileEntity with net.Node => Some(tileEntity)
          case _ => None
        }
      case _ => None
    }

  private class Node(val data: net.Node) {
    val edges = ArrayBuffer.empty[Edge]

    def remove() = {
      edges.foreach(edge => edge.other(this).edges -= edge)
      searchGraphs(edges.map(_.other(this)))
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
      searchGraphs(List(left, right))
    }
  }

  private def searchGraphs(seeds: Seq[Node]) = {
    val seen = mutable.Set.empty[Node]
    seeds.map(seed => {
      if (seen.contains(seed)) {
        // If our seed node is contained in another sub graph we have nothing
        // to do, since we're a sub graph of that sub graph.
        mutable.Map.empty[Int, mutable.ArrayBuffer[Node]]
      }
      else {
        // Not yet processed, start growing a network from here. We're
        // guaranteed to not find previously processed nodes, since edges
        // are bidirectional, and we'd be in the other branch otherwise.
        seen += seed
        val subGraph = mutable.Map(seed.data.address -> mutable.ArrayBuffer(seed))
        val queue = mutable.Queue(seed.edges.map(_.other(seed)): _*)
        while (queue.nonEmpty) {
          val node = queue.dequeue()
          seen += node
          subGraph.getOrElseUpdate(node.data.address, new ArrayBuffer[Node]) += node
          queue ++= node.edges.map(_.other(node)).filter(n => !seen.contains(n) && !queue.contains(n))
        }
        subGraph
      }
    }) filter (_.nonEmpty)
  }

  private class Message(@BeanProperty val source: net.Node,
                        @BeanProperty val name: String,
                        @BeanProperty val data: Array[Any] = Array()) extends net.Message {
    var isCanceled = false

    def cancel() = isCanceled = true
  }

  private case class ConnectMessage(override val source: net.Node) extends Message(source, "network.connect")

  private case class DisconnectMessage(override val source: net.Node) extends Message(source, "network.disconnect")

  private case class ReconnectMessage(override val source: net.Node, oldAddress: Int) extends Message(source, "network.reconnect", Array(oldAddress.asInstanceOf[Any]))

}
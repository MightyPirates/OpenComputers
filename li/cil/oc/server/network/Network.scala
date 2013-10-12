package li.cil.oc.server.network

import java.util.logging.Level
import li.cil.oc.api.network.Visibility
import li.cil.oc.{api, OpenComputers}
import net.minecraft.block.Block
import net.minecraft.tileentity.TileEntity
import net.minecraft.world.{IBlockAccess, World}
import net.minecraftforge.common.ForgeDirection
import net.minecraftforge.event.ForgeSubscribe
import net.minecraftforge.event.world.ChunkEvent
import scala.beans.BeanProperty
import scala.collection.JavaConverters._
import scala.collection.mutable
import scala.collection.mutable.ArrayBuffer

class Network private(private val addressedNodes: mutable.Map[String, Network.Node] = mutable.Map.empty,
                      private val unaddressedNodes: mutable.ArrayBuffer[Network.Node] = mutable.ArrayBuffer.empty) extends api.Network {
  def this(node: api.network.Node) = {
    this()
    addNew(node)
    if (node.address.isDefined)
      send(Network.ConnectMessage(node), Iterable(node))
  }

  addressedNodes.values.foreach(_.data.network = Some(this))
  unaddressedNodes.foreach(_.data.network = Some(this))

  // ----------------------------------------------------------------------- //

  override def connect(nodeA: api.network.Node, nodeB: api.network.Node) = {
    if (nodeA == nodeB) throw new IllegalArgumentException(
      "Cannot connect a node to itself.")

    val containsA = contains(nodeA)
    val containsB = contains(nodeB)

    if (!containsA && !containsB) throw new IllegalArgumentException(
      "At least one of the nodes must already be in this network.")

    def oldNodeA = node(nodeA)
    def oldNodeB = node(nodeB)

    if (containsA && containsB) {
      // Both nodes already exist in the network but there is a new connection.
      // This can happen if a new node sequentially connects to multiple nodes
      // in an existing network, e.g. in a setup like so:
      // O O   Where O is an old node, and N is the new Node. It would connect
      // O N   to the node above and left to it (in no particular order).
      if (!oldNodeA.edges.exists(_.isBetween(oldNodeA, oldNodeB))) {
        assert(!oldNodeB.edges.exists(_.isBetween(oldNodeA, oldNodeB)))
        Network.Edge(oldNodeA, oldNodeB)
        if (oldNodeA.data.visibility == Visibility.Neighbors && oldNodeB.data.address.isDefined)
          send(Network.ConnectMessage(oldNodeA.data), Iterable(oldNodeB.data))
        if (oldNodeA.data.visibility == Visibility.Neighbors && oldNodeA.data.address.isDefined)
          send(Network.ConnectMessage(oldNodeA.data), Iterable(oldNodeB.data))
        true
      }
      else false // That connection already exists.
    }
    else if (containsA) add(oldNodeA, nodeB)
    else add(oldNodeB, nodeA)
  }

  override def disconnect(nodeA: api.network.Node, nodeB: api.network.Node) = {
    val containsA = contains(nodeA)
    val containsB = contains(nodeB)

    if (!containsA || !containsB) throw new IllegalArgumentException(
      "Both of the nodes must be in this network.")

    def oldNodeA = node(nodeA)
    def oldNodeB = node(nodeB)

    oldNodeA.edges.find(_.isBetween(oldNodeA, oldNodeB)) match {
      case None => false // That connection doesn't exists.
      case Some(edge) => {
        handleSplit(edge.remove())
        if (edge.left.data.visibility == Visibility.Neighbors && edge.right.data.address.isDefined)
          send(Network.DisconnectMessage(edge.left.data), Iterable(edge.right.data))
        if (edge.right.data.visibility == Visibility.Neighbors && edge.left.data.address.isDefined)
          send(Network.DisconnectMessage(edge.right.data), Iterable(edge.left.data))
        true
      }
    }
  }

  override def remove(node: api.network.Node) = (node.address match {
    case None => unaddressedNodes.indexWhere(_.data == node) match {
      case -1 => None
      case index => Some(unaddressedNodes.remove(index))
    }
    case Some(address) => addressedNodes.remove(address)
  }) match {
    case None => false
    case Some(entry) => {
      node.network = None
      val subGraphs = entry.remove()
      val targets = Iterable(node) ++ (entry.data.visibility match {
        case Visibility.None => Iterable.empty[api.network.Node]
        case Visibility.Neighbors => entry.edges.map(_.other(entry).data)
        case Visibility.Network => subGraphs.map {
          case (addressed, _) => addressed.values.map(_.data)
        }.flatten
      })
      handleSplit(subGraphs)
      send(Network.DisconnectMessage(node), targets)
      true
    }
  }

  // ----------------------------------------------------------------------- //

  override def node(address: String) = addressedNodes.get(address) match {
    case Some(node) => Some(node.data)
    case _ => None
  }

  override def nodes = addressedNodes.values.map(_.data)

  def nodes(reference: api.network.Node) = {
    val referenceNeighbors = neighbors(reference).toSet
    nodes.filter(node => node != reference && (node.visibility == Visibility.Network ||
      (node.visibility == Visibility.Neighbors && referenceNeighbors.contains(node))))
  }

  override def neighbors(node: api.network.Node) = node.address match {
    case None =>
      unaddressedNodes.find(_.data == node) match {
        case None => throw new IllegalArgumentException("Node must be in this network.")
        case Some(n) => n.edges.map(_.other(n).data)
      }
    case Some(address) =>
      addressedNodes.get(address) match {
        case None => throw new IllegalArgumentException("Node must be in this network.")
        case Some(n) => n.edges.map(_.other(n).data)
      }
  }

  // ----------------------------------------------------------------------- //

  override def sendToAddress(source: api.network.Node, target: String, name: String, data: Any*) = {
    if (source.network.isEmpty || source.network.get != this)
      throw new IllegalArgumentException("Source node must be in this network.")
    if (source.address.isDefined) addressedNodes.get(target) match {
      case Some(node) if node.data.visibility == Visibility.Network ||
        (node.data.visibility == Visibility.Neighbors && neighbors(node.data).exists(_ == source)) =>
        send(new Network.Message(source, name, Array(data: _*)), Iterable(node.data))
      case _ => None
    } else None
  }

  override def sendToNeighbors(source: api.network.Node, name: String, data: Any*) = {
    if (source.network.isEmpty || source.network.get != this)
      throw new IllegalArgumentException("Source node must be in this network.")
    if (source.address.isDefined)
      send(new Network.Message(source, name, Array(data: _*)), neighbors(source).filter(_.visibility != Visibility.None))
    else None
  }

  override def sendToVisible(source: api.network.Node, name: String, data: Any*) = {
    if (source.network.isEmpty || source.network.get != this)
      throw new IllegalArgumentException("Source node must be in this network.")
    if (source.address.isDefined)
      send(new Network.Message(source, name, Array(data: _*)), nodes(source))
    else None
  }

  // ----------------------------------------------------------------------- //

  private def contains(node: api.network.Node) = (node.address match {
    case None => unaddressedNodes.find(_.data == node)
    case Some(address) => addressedNodes.get(address)
  }).exists(_.data == node)

  private def node(node: api.network.Node) = (node.address match {
    case None => unaddressedNodes.find(_.data == node)
    case Some(address) => addressedNodes.get(address)
  }).get

  private def addNew(node: api.network.Node) = {
    val newNode = new Network.Node(node)
    if (node.address.isEmpty)
      node.address = Some(java.util.UUID.randomUUID().toString)
    if (node.address.isDefined)
      addressedNodes += node.address.get -> newNode
    else
      unaddressedNodes += newNode
    node.network = Some(this)
    newNode
  }

  private def add(oldNode: Network.Node, addedNode: api.network.Node) = {
    // Queue any messages to avoid side effects from receivers.
    val sendQueue = mutable.Buffer.empty[(Network.Message, Iterable[api.network.Node])]
    // Check if the other node is new or if we have to merge networks.
    if (addedNode.network.isEmpty) {
      val newNode = addNew(addedNode)
      Network.Edge(oldNode, newNode)
      if (addedNode.address.isDefined) addedNode.visibility match {
        case Visibility.None =>
          sendQueue += ((Network.ConnectMessage(addedNode), Iterable(addedNode)))
        case Visibility.Neighbors =>
          sendQueue += ((Network.ConnectMessage(addedNode), Iterable(addedNode) ++ neighbors(addedNode)))
          nodes(addedNode).foreach(node => sendQueue += ((new Network.ConnectMessage(node), Iterable(addedNode))))
        case Visibility.Network =>
          // Explicitly send to the added node itself first.
          sendQueue += ((Network.ConnectMessage(addedNode), Iterable(addedNode) ++ nodes.filter(_ != addedNode)))
          nodes(addedNode).foreach(node => sendQueue += ((new Network.ConnectMessage(node), Iterable(addedNode))))
      }
    }
    else {
      val otherNetwork = addedNode.network.get.asInstanceOf[Network]

      if (addedNode.visibility == Visibility.Neighbors && oldNode.data.address.isDefined)
        sendQueue += ((Network.ConnectMessage(addedNode), Iterable(oldNode.data)))
      if (oldNode.data.visibility == Visibility.Neighbors && addedNode.address.isDefined)
        sendQueue += ((Network.ConnectMessage(oldNode.data), Iterable(addedNode)))

      val oldNodes = nodes
      val newNodes = otherNetwork.nodes
      val oldVisibleNodes = oldNodes.filter(_.visibility == Visibility.Network)
      val newVisibleNodes = newNodes.filter(_.visibility == Visibility.Network)

      newVisibleNodes.foreach(node => sendQueue += ((Network.ConnectMessage(node), oldNodes)))
      oldVisibleNodes.foreach(node => sendQueue += ((Network.ConnectMessage(node), newNodes)))

      addressedNodes ++= otherNetwork.addressedNodes
      unaddressedNodes ++= otherNetwork.unaddressedNodes
      otherNetwork.addressedNodes.values.foreach(_.data.network = Some(this))
      otherNetwork.unaddressedNodes.foreach(_.data.network = Some(this))

      val newNode = addedNode.address match {
        case None => unaddressedNodes.find(_.data == addedNode).get
        case Some(address) => addressedNodes(address)
      }
      Network.Edge(oldNode, newNode)
    }

    for ((message, nodes) <- sendQueue) send(message, nodes)

    true
  }

  private def handleSplit(subGraphs: Seq[(mutable.Map[String, Network.Node], mutable.ArrayBuffer[Network.Node])]) =
    if (subGraphs.size > 1) {
      val nodes = subGraphs.map {
        case (addressed, _) => addressed.values.map(_.data)
      }
      val visibleNodes = nodes.map(_.filter(_.visibility == Visibility.Network))

      addressedNodes.clear()
      unaddressedNodes.clear()

      subGraphs.head match {
        case (addressed, unaddressed) =>
          addressedNodes ++= addressed
          unaddressedNodes ++= unaddressed
      }

      subGraphs.tail.foreach {
        case (addressed, unaddressed) =>
          new Network(addressed, unaddressed)
      }

      for (indexA <- 0 until subGraphs.length) {
        val nodesA = nodes(indexA)
        val visibleNodesA = visibleNodes(indexA)
        for (indexB <- (indexA + 1) until subGraphs.length) {
          val nodesB = nodes(indexB)
          val visibleNodesB = visibleNodes(indexB)
          visibleNodesA.foreach(node => send(new Network.DisconnectMessage(node), nodesB))
          visibleNodesB.foreach(node => send(new Network.DisconnectMessage(node), nodesA))
        }
      }
    }

  private def send(message: Network.Message, targets: Iterable[api.network.Node]) = {
    def protectedSend(target: api.network.Node) = try {
      //println("receive(" + message.name + "(" + message.data.mkString(", ") + "): " + message.source.address.get + ":" + message.source.name + " -> " + target.address.get + ":" + target.name + ")")
      target.receive(message)
    } catch {
      case e: Throwable => OpenComputers.log.log(Level.WARNING, "Error in message handler", e); None
    }

    message match {
      case _@(Network.ConnectMessage(_) | Network.DisconnectMessage(_)) =>
        targets.foreach(protectedSend)
        None
      case _ =>
        var result = None: Option[Array[Any]]
        val iterator = targets.iterator
        while (!message.isCanceled && iterator.hasNext)
          protectedSend(iterator.next()) match {
            case None => // Ignore.
            case r => result = r
          }
        result
    }
  }
}

object Network extends api.detail.NetworkAPI {
  override def joinOrCreateNetwork(world: IBlockAccess, x: Int, y: Int, z: Int): Unit =
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

  private def getNetworkNode(world: IBlockAccess, x: Int, y: Int, z: Int): Option[TileEntity with api.network.Node] =
    Option(Block.blocksList(world.getBlockId(x, y, z))) match {
      case Some(block) if block.hasTileEntity(world.getBlockMetadata(x, y, z)) =>
        world.getBlockTileEntity(x, y, z) match {
          case tileEntity: TileEntity with api.network.Node => Some(tileEntity)
          case _ => None
        }
      case _ => None
    }

  // ----------------------------------------------------------------------- //

  @ForgeSubscribe
  def onChunkUnload(e: ChunkEvent.Unload) =
    onUnload(e.world, e.getChunk.chunkTileEntityMap.values.asScala.map(_.asInstanceOf[TileEntity]))

  @ForgeSubscribe
  def onChunkLoad(e: ChunkEvent.Load) =
    onLoad(e.world, e.getChunk.chunkTileEntityMap.values.asScala.map(_.asInstanceOf[TileEntity]))

  private def onUnload(w: World, tileEntities: Iterable[TileEntity]) = if (!w.isRemote) {
    // TODO add a more efficient batch remove operation? something along the lines of if #remove > #nodes*factor remove all, re-add remaining?
    tileEntities.
      filter(_.isInstanceOf[api.network.Node]).
      map(_.asInstanceOf[api.network.Node]).
      foreach(t => t.network.foreach(_.remove(t)))
  }

  private def onLoad(w: World, tileEntities: Iterable[TileEntity]) = if (!w.isRemote) {
    tileEntities.foreach(t => joinOrCreateNetwork(w, t.xCoord, t.yCoord, t.zCoord))
  }

  // ----------------------------------------------------------------------- //

  private class Node(val data: api.network.Node) {
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
      if (seen.contains(seed)) None
      else {
        val addressed = mutable.Map.empty[String, Node]
        val unaddressed = mutable.ArrayBuffer.empty[Node]
        val queue = mutable.Queue(seed)
        while (queue.nonEmpty) {
          val node = queue.dequeue()
          seen += node
          node.data.address match {
            case None => unaddressed += node
            case Some(address) => addressed += address -> node
          }
          queue ++= node.edges.map(_.other(node)).filter(n => !seen.contains(n) && !queue.contains(n))
        }
        Some((addressed, unaddressed))
      }
    }) filter (_.nonEmpty) map (_.get)
  }

  // ----------------------------------------------------------------------- //

  private class Message(@BeanProperty val source: api.network.Node,
                        @BeanProperty val name: String,
                        @BeanProperty val data: Array[Any] = Array()) extends api.network.Message {
    var isCanceled = false

    override def cancel() = isCanceled = true
  }

  private case class ConnectMessage(override val source: api.network.Node) extends Message(source, "system.connect")

  private case class DisconnectMessage(override val source: api.network.Node) extends Message(source, "system.disconnect")

}
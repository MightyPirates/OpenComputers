package li.cil.oc.server.network

import java.util.logging.Level
import li.cil.oc.api.network.Visibility
import li.cil.oc.api.network.environment.Environment
import li.cil.oc.server.network
import li.cil.oc.{api, OpenComputers}
import net.minecraft.block.Block
import net.minecraft.tileentity.TileEntity
import net.minecraft.world.{IBlockAccess, World}
import net.minecraftforge.common.ForgeDirection
import net.minecraftforge.event.ForgeSubscribe
import net.minecraftforge.event.world.ChunkEvent
import scala.collection.JavaConverters._
import scala.collection.convert.WrapAsJava._
import scala.collection.convert.WrapAsScala._
import scala.collection.mutable
import scala.collection.mutable.ArrayBuffer

class Network private(private val addressedNodes: mutable.Map[String, Network.Node] = mutable.Map.empty,
                      private val unaddressedNodes: mutable.ArrayBuffer[Network.Node] = mutable.ArrayBuffer.empty) extends api.network.Network {
  def this(node: Node) = {
    this()
    addNew(node)
    if (node.address != null)
      send(Network.ConnectMessage(node), Iterable(node))
  }

  addressedNodes.values.foreach(_.data.network = this)
  unaddressedNodes.foreach(_.data.network = this)

  // ----------------------------------------------------------------------- //

  def connect(nodeA: api.network.Node, nodeB: api.network.Node) = {
    if (nodeA == nodeB) throw new IllegalArgumentException(
      "Cannot connect a node to itself.")

    if (!nodeA.isInstanceOf[network.Node]) throw new IllegalArgumentException(
      "Unsupported node implementation. Don't implement the interface yourself!")
    if (!nodeB.isInstanceOf[network.Node]) throw new IllegalArgumentException(
      "Unsupported node implementation. Don't implement the interface yourself!")

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
        if (oldNodeA.data.reachability == Visibility.Neighbors && oldNodeB.data.address != null)
          send(Network.ConnectMessage(oldNodeA.data), Iterable(oldNodeB.data))
        if (oldNodeA.data.reachability == Visibility.Neighbors && oldNodeA.data.address != null)
          send(Network.ConnectMessage(oldNodeA.data), Iterable(oldNodeB.data))
        true
      }
      else false // That connection already exists.
    }
    else if (containsA) add(oldNodeA, nodeB.asInstanceOf[network.Node])
    else add(oldNodeB, nodeA.asInstanceOf[network.Node])
  }

  def disconnect(nodeA: api.network.Node, nodeB: api.network.Node) = {
    if (nodeA == nodeB) throw new IllegalArgumentException(
      "Cannot disconnect a node from itself.")

    if (!nodeA.isInstanceOf[network.Node]) throw new IllegalArgumentException(
      "Unsupported node implementation. Don't implement the interface yourself!")
    if (!nodeB.isInstanceOf[network.Node]) throw new IllegalArgumentException(
      "Unsupported node implementation. Don't implement the interface yourself!")

    val containsA = contains(nodeA)
    val containsB = contains(nodeB)

    if (!containsA || !containsB) throw new IllegalArgumentException(
      "Both of the nodes must be in this network.")

    def oldNodeA = node(nodeA)
    def oldNodeB = node(nodeB)

    oldNodeA.edges.find(_.isBetween(oldNodeA, oldNodeB)) match {
      case Some(edge) => {
        handleSplit(edge.remove())
        if (edge.left.data.reachability == Visibility.Neighbors && edge.right.data.address != null)
          send(Network.DisconnectMessage(edge.left.data), Iterable(edge.right.data))
        if (edge.right.data.reachability == Visibility.Neighbors && edge.left.data.address != null)
          send(Network.DisconnectMessage(edge.right.data), Iterable(edge.left.data))
        true
      }
      case _ => false // That connection doesn't exists.
    }
  }

  def remove(node: api.network.Node) = (Option(node.address) match {
    case Some(address) => addressedNodes.remove(address)
    case _ => unaddressedNodes.indexWhere(_.data == node) match {
      case -1 => None
      case index => Some(unaddressedNodes.remove(index))
    }
  }) match {
    case Some(entry) => {
      node.asInstanceOf[Node].network = null
      val subGraphs = entry.remove()
      val targets = Iterable(node) ++ (entry.data.reachability match {
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
    case _ => false
  }

  // ----------------------------------------------------------------------- //

  def node(address: String) = addressedNodes.get(address) match {
    case Some(node) => node.data
    case _ => null
  }

  def nodes = addressedNodes.values.map(_.data.asInstanceOf[api.network.Node]).asJava

  def nodes(reference: api.network.Node) = {
    val referenceNeighbors = neighbors(reference).toSet.asJava
    nodes.filter(node => node != reference && (node.reachability == Visibility.Network ||
      (node.reachability == Visibility.Neighbors && referenceNeighbors.contains(node)))).asJava
  }

  def neighbors(node: api.network.Node) = Option(node.address) match {
    case Some(address) =>
      addressedNodes.get(address) match {
        case Some(n) => n.edges.map(_.other(n).data)
        case _ => throw new IllegalArgumentException("Node must be in this network.")
      }
    case None =>
      unaddressedNodes.find(_.data == node) match {
        case Some(n) => n.edges.map(_.other(n).data)
        case _ => throw new IllegalArgumentException("Node must be in this network.")
      }
  }

  // ----------------------------------------------------------------------- //

  def sendToAddress(source: api.network.Node, target: String, name: String, data: AnyRef*) = {
    if (source.network == null || source.network != this)
      throw new IllegalArgumentException("Source node must be in this network.")
    if (source.address != null) addressedNodes.get(target) match {
      case Some(node) if node.data.reachability == Visibility.Network ||
        (node.data.reachability == Visibility.Neighbors && neighbors(node.data).exists(_ == source)) =>
        send(new Network.Message(source, name, Array(data: _*)), Iterable(node.data))
      case _ => null
    } else null
  }

  def sendToNeighbors(source: api.network.Node, name: String, data: AnyRef*) = {
    if (source.network == null || source.network != this)
      throw new IllegalArgumentException("Source node must be in this network.")
    if (source.address != null)
      send(new Network.Message(source, name, Array(data: _*)), neighbors(source).filter(_.reachability != Visibility.None))
    else null
  }

  def sendToVisible(source: api.network.Node, name: String, data: AnyRef*) = {
    if (source.network == null || source.network != this)
      throw new IllegalArgumentException("Source node must be in this network.")
    if (source.address != null)
      send(new Network.Message(source, name, Array(data: _*)), nodes(source))
    else null
  }

  // ----------------------------------------------------------------------- //

  private def contains(node: api.network.Node) = (Option(node.address) match {
    case Some(address) => addressedNodes.get(address)
    case None => unaddressedNodes.find(_.data == node)
  }).exists(_.data == node)

  private def node(node: api.network.Node) = (Option(node.address) match {
    case Some(address) => addressedNodes.get(address)
    case None => unaddressedNodes.find(_.data == node)
  }).get

  private def addNew(node: network.Node) = {
    val newNode = new Network.Node(node)
    if (node.address == null)
      node.address = java.util.UUID.randomUUID().toString
    if (node.address != null)
      addressedNodes += node.address -> newNode
    else
      unaddressedNodes += newNode
    node.network = this
    newNode
  }

  private def add(oldNode: Network.Node, addedNode: network.Node) = {
    // Queue any messages to avoid side effects from receivers.
    val sendQueue = mutable.Buffer.empty[(Network.Message, Iterable[api.network.Node])]
    // Check if the other node is new or if we have to merge networks.
    if (addedNode.network == null) {
      val newNode = addNew(addedNode)
      Network.Edge(oldNode, newNode)
      if (addedNode.address != null) addedNode.reachability match {
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
      val otherNetwork = addedNode.network.asInstanceOf[Network]

      if (addedNode.reachability == Visibility.Neighbors && oldNode.data.address != null)
        sendQueue += ((Network.ConnectMessage(addedNode), Iterable(oldNode.data)))
      if (oldNode.data.reachability == Visibility.Neighbors && addedNode.address != null)
        sendQueue += ((Network.ConnectMessage(oldNode.data), Iterable(addedNode)))

      val oldNodes = nodes
      val newNodes = otherNetwork.nodes
      val oldVisibleNodes = oldNodes.filter(_.reachability == Visibility.Network)
      val newVisibleNodes = newNodes.filter(_.reachability == Visibility.Network)

      newVisibleNodes.foreach(node => sendQueue += ((Network.ConnectMessage(node), oldNodes)))
      oldVisibleNodes.foreach(node => sendQueue += ((Network.ConnectMessage(node), newNodes)))

      addressedNodes ++= otherNetwork.addressedNodes
      unaddressedNodes ++= otherNetwork.unaddressedNodes
      otherNetwork.addressedNodes.values.foreach(_.data.network = this)
      otherNetwork.unaddressedNodes.foreach(_.data.network = this)

      val newNode = Option(addedNode.address) match {
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
      val visibleNodes = nodes.map(_.filter(_.reachability == Visibility.Network))

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
      case e: Throwable =>
        OpenComputers.log.log(Level.WARNING, "Error in message handler", e)
        null
    }

    message match {
      case _@(Network.ConnectMessage(_) | Network.DisconnectMessage(_)) =>
        targets.foreach(protectedSend)
        null
      case _ =>
        var result = null: Array[AnyRef]
        val iterator = targets.iterator
        while (!message.isCanceled && iterator.hasNext)
          protectedSend(iterator.next()) match {
            case null => // Ignore.
            case r => result = r
          }
        result
    }
  }
}

object Network extends api.detail.NetworkAPI {
  override def joinOrCreateNetwork(world: World, x: Int, y: Int, z: Int): Unit =
    if (!world.isRemote) getNetworkNode(world, x, y, z) match {
      case Some(node) => {
        for (side <- ForgeDirection.VALID_DIRECTIONS) {
          getNetworkNode(world, x + side.offsetX, y + side.offsetY, z + side.offsetZ) match {
            case Some(neighborNode) =>
              if (neighborNode.network != null) {
                neighborNode.network.connect(neighborNode, node)
              }
            case _ => // Ignore.
          }
        }
        if (node.network == null) new Network(node)
      }
      case _ => // Invalid block.
    }

  private def getNetworkNode(world: IBlockAccess, x: Int, y: Int, z: Int): Option[network.Node] =
    Option(Block.blocksList(world.getBlockId(x, y, z))) match {
      case Some(block) if block.hasTileEntity(world.getBlockMetadata(x, y, z)) =>
        world.getBlockTileEntity(x, y, z) match {
          case host: Environment => Some(host.node.asInstanceOf[network.Node])
          case _ => None
        }
      case _ => None
    }

  // ----------------------------------------------------------------------- //

  def createNode(host: Environment, name: String, reachability: api.network.Visibility) = new network.Node(host, name, reachability)

  def createComponent(node: api.network.Node) = new network.Component(node.host, node.name, node.reachability)

  def createConsumer(node: api.network.Node) = ???

  def createProducer(node: api.network.Node) = ???

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
      filter(_.isInstanceOf[Environment]).
      map(_.asInstanceOf[Environment]).
      foreach(t => t.node.network.remove(t.node))
  }

  private def onLoad(w: World, tileEntities: Iterable[TileEntity]) = if (!w.isRemote) {
    tileEntities.foreach(t => joinOrCreateNetwork(w, t.xCoord, t.yCoord, t.zCoord))
  }

  // ----------------------------------------------------------------------- //

  private class Node(val data: network.Node) {
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
          Option(node.data.address) match {
            case Some(address) => addressed += address -> node
            case _ => unaddressed += node
          }
          queue ++= node.edges.map(_.other(node)).filter(n => !seen.contains(n) && !queue.contains(n))
        }
        Some((addressed, unaddressed))
      }
    }) filter (_.nonEmpty) map (_.get)
  }

  // ----------------------------------------------------------------------- //

  private class Message(val source: api.network.Node,
                        val name: String,
                        val data: Array[AnyRef] = Array()) extends api.network.Message {
    var isCanceled = false

    def cancel() = isCanceled = true

    def checkBoolean(index: Int): Boolean = {
      checkCount(index, "boolean")
      data(index) match {
        case value: java.lang.Boolean => value
        case value => throw typeError(index, value, "boolean")
      }
    }

    def checkDouble(index: Int): Double = {
      checkCount(index, "number")
      data(index) match {
        case value: java.lang.Double => value
        case value => throw typeError(index, value, "number")
      }
    }

    def checkInteger(index: Int): Int = {
      checkCount(index, "number")
      data(index) match {
        case value: java.lang.Double => value.intValue
        case value => throw typeError(index, value, "number")
      }
    }

    def checkByteArray(index: Int): Array[Byte] = {
      checkCount(index, "string")
      data(index) match {
        case value: Array[Byte] => value
        case value => throw typeError(index, value, "string")
      }
    }

    def checkString(index: Int) =
      new String(checkByteArray(index), "UTF-8")

    private def checkCount(count: Int, name: String) =
      if (data.length <= count) throw new IllegalArgumentException(
        "bad arguments #%d (%s expected, got no value)".
          format(count + 1, name))

    private def typeError(index: Int, have: AnyRef, want: String) =
      new IllegalArgumentException(
        "bad argument #%d (%s expected, got %have)".
          format(index + 1, want, typeName(have)))

    private def typeName(value: AnyRef): String = value match {
      case null => "nil"
      case _: java.lang.Boolean => "boolean"
      case _: java.lang.Double => "double"
      case _: java.lang.String => "string"
      case _: Array[Byte] => "string"
      case _ => value.getClass.getSimpleName
    }
  }

  private case class ConnectMessage(override val source: api.network.Node) extends Message(source, "system.connect")

  private case class DisconnectMessage(override val source: api.network.Node) extends Message(source, "system.disconnect")

}
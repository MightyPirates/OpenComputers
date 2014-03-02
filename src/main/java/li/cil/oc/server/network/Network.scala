package li.cil.oc.server.network

import codechicken.multipart.TileMultipart
import cpw.mods.fml.common.FMLCommonHandler
import cpw.mods.fml.relauncher.Side
import li.cil.oc.api.network.{Node => ImmutableNode, SidedEnvironment, Environment, Visibility}
import li.cil.oc.common.multipart.CablePart
import li.cil.oc.common.tileentity.PassiveNode
import li.cil.oc.server.network.{Node => MutableNode}
import li.cil.oc.{Settings, api}
import net.minecraft.tileentity.TileEntity
import net.minecraftforge.common.ForgeDirection
import net.minecraftforge.event.ForgeSubscribe
import net.minecraftforge.event.world.{ChunkEvent, WorldEvent}
import scala.collection.JavaConverters._
import scala.collection.convert.WrapAsScala._
import scala.collection.mutable
import scala.collection.mutable.ArrayBuffer

// Looking at this again after some time, the similarity to const in C++ is somewhat uncanny.
private class Network private(private val data: mutable.Map[String, Network.Vertex] = mutable.Map.empty) extends Distributor {
  def this(node: MutableNode) = {
    this()
    addNew(node)
    node.onConnect(node)
  }

  var globalBuffer = 0.0

  var globalBufferSize = 0.0

  private val connectors = mutable.ArrayBuffer.empty[Connector]

  private lazy val wrapper = new Network.Wrapper(this)

  data.values.foreach(node => {
    node.data match {
      case connector: Connector => addConnector(connector)
      case _ =>
    }
    node.data.network = wrapper
  })

  // ----------------------------------------------------------------------- //

  def connect(nodeA: MutableNode, nodeB: MutableNode) = {
    if (nodeA == nodeB) throw new IllegalArgumentException(
      "Cannot connect a node to itself.")

    val containsA = contains(nodeA)
    val containsB = contains(nodeB)

    if (!containsA && !containsB) throw new IllegalArgumentException(
      "At least one of the nodes must already be in this network.")

    lazy val oldNodeA = node(nodeA)
    lazy val oldNodeB = node(nodeB)

    if (containsA && containsB) {
      // Both nodes already exist in the network but there is a new connection.
      // This can happen if a new node sequentially connects to multiple nodes
      // in an existing network, e.g. in a setup like so:
      // O O   Where O is an old node, and N is the new Node. It would connect
      // O N   to the node above and left to it (in no particular order).
      if (!oldNodeA.edges.exists(_.isBetween(oldNodeA, oldNodeB))) {
        assert(!oldNodeB.edges.exists(_.isBetween(oldNodeA, oldNodeB)))
        Network.Edge(oldNodeA, oldNodeB)
        if (oldNodeA.data.reachability == Visibility.Neighbors)
          oldNodeB.data.onConnect(oldNodeA.data)
        if (oldNodeB.data.reachability == Visibility.Neighbors)
          oldNodeA.data.onConnect(oldNodeB.data)
        true
      }
      else false // That connection already exists.
    }
    else if (containsA) add(oldNodeA, nodeB)
    else add(oldNodeB, nodeA)
  }

  def disconnect(nodeA: MutableNode, nodeB: MutableNode) = {
    if (nodeA == nodeB) throw new IllegalArgumentException(
      "Cannot disconnect a node from itself.")

    val containsA = contains(nodeA)
    val containsB = contains(nodeB)

    if (!containsA || !containsB) throw new IllegalArgumentException(
      "Both of the nodes must be in this network.")

    def oldNodeA = node(nodeA)
    def oldNodeB = node(nodeB)

    oldNodeA.edges.find(_.isBetween(oldNodeA, oldNodeB)) match {
      case Some(edge) =>
        handleSplit(edge.remove())
        if (edge.left.data.reachability == Visibility.Neighbors)
          edge.right.data.onDisconnect(edge.left.data)
        if (edge.right.data.reachability == Visibility.Neighbors)
          edge.left.data.onDisconnect(edge.right.data)
        true
      case _ => false // That connection doesn't exists.
    }
  }

  def remove(node: MutableNode) = {
    data.remove(node.address) match {
      case Some(entry) =>
        node match {
          case connector: Connector => removeConnector(connector)
          case _ =>
        }
        node.network = null
        val subGraphs = entry.remove()
        val targets = Iterable(node) ++ (entry.data.reachability match {
          case Visibility.None => Iterable.empty[ImmutableNode]
          case Visibility.Neighbors => entry.edges.map(_.other(entry).data)
          case Visibility.Network => subGraphs.map(_.values.map(_.data)).flatten
        })
        handleSplit(subGraphs)
        targets.foreach(_.asInstanceOf[MutableNode].onDisconnect(node))
        true
      case _ => false
    }
  }

  // ----------------------------------------------------------------------- //

  def node(address: String) = {
    data.get(address) match {
      case Some(node) => node.data
      case _ => null
    }
  }

  def nodes: Iterable[ImmutableNode] = data.values.map(_.data)

  def nodes(reference: ImmutableNode): Iterable[ImmutableNode] = {
    val referenceNeighbors = neighbors(reference).toSet
    nodes.filter(node => node != reference && (node.reachability == Visibility.Network ||
      (node.reachability == Visibility.Neighbors && referenceNeighbors.contains(node))))
  }

  def neighbors(node: ImmutableNode): Iterable[ImmutableNode] = {
    data.get(node.address) match {
      case Some(n) =>
        assert(n.data == node)
        n.edges.map(_.other(n).data)
      case _ => throw new IllegalArgumentException("Node must be in this network.")
    }
  }

  // ----------------------------------------------------------------------- //

  def sendToAddress(source: ImmutableNode, target: String, name: String, args: AnyRef*) = {
    if (source.network != wrapper)
      throw new IllegalArgumentException("Source node must be in this network.")
    data.get(target) match {
      case Some(node) if node.data.canBeReachedFrom(source) =>
        send(source, Iterable(node.data), name, args: _*)
      case _ =>
    }
  }

  def sendToNeighbors(source: ImmutableNode, name: String, args: AnyRef*) = {
    if (source.network != wrapper)
      throw new IllegalArgumentException("Source node must be in this network.")
    send(source, neighbors(source).filter(_.reachability != Visibility.None), name, args: _*)
  }

  def sendToReachable(source: ImmutableNode, name: String, args: AnyRef*) = {
    if (source.network != wrapper)
      throw new IllegalArgumentException("Source node must be in this network.")
    send(source, nodes(source), name, args: _*)
  }

  def sendToVisible(source: ImmutableNode, name: String, args: AnyRef*) = {
    if (source.network != wrapper)
      throw new IllegalArgumentException("Source node must be in this network.")
    send(source, nodes(source) collect {
      case component: api.network.Component if component.canBeSeenFrom(source) => component
    }, name, args: _*)
  }

  // ----------------------------------------------------------------------- //

  private def contains(node: ImmutableNode) = data.contains(node.address)

  private def node(node: ImmutableNode) = data(node.address)

  private def addNew(node: MutableNode) = {
    val newNode = new Network.Vertex(node)
    if (node.address == null)
      node.address = java.util.UUID.randomUUID().toString
    data += node.address -> newNode
    node match {
      case connector: Connector => addConnector(connector)
      case _ =>
    }
    node.network = wrapper
    newNode
  }

  private def add(oldNode: Network.Vertex, addedNode: MutableNode) = {
    // Queue onConnect calls to avoid side effects from callbacks.
    val connects = mutable.Buffer.empty[(ImmutableNode, Iterable[ImmutableNode])]
    // Check if the other node is new or if we have to merge networks.
    if (addedNode.network == null) {
      val newNode = addNew(addedNode)
      Network.Edge(oldNode, newNode)
      addedNode.reachability match {
        case Visibility.None =>
          connects += ((addedNode, Iterable(addedNode)))
        case Visibility.Neighbors =>
          connects += ((addedNode, Iterable(addedNode) ++ neighbors(addedNode)))
          nodes(addedNode).foreach(node => connects += ((node, Iterable(addedNode))))
        case Visibility.Network =>
          // Explicitly send to the added node itself first.
          connects += ((addedNode, Iterable(addedNode) ++ nodes.filter(_ != addedNode)))
          nodes(addedNode).foreach(node => connects += ((node, Iterable(addedNode))))
      }
    }
    else {
      val otherNetwork = addedNode.network.asInstanceOf[Network.Wrapper].network

      if (addedNode.reachability == Visibility.Neighbors)
        connects += ((addedNode, Iterable(oldNode.data)))
      if (oldNode.data.reachability == Visibility.Neighbors)
        connects += ((oldNode.data, Iterable(addedNode)))

      val oldNodes = nodes
      val newNodes = otherNetwork.nodes
      val oldVisibleNodes = oldNodes.filter(_.reachability == Visibility.Network)
      val newVisibleNodes = newNodes.filter(_.reachability == Visibility.Network)

      newVisibleNodes.foreach(node => connects += ((node, oldNodes)))
      oldVisibleNodes.foreach(node => connects += ((node, newNodes)))

      data ++= otherNetwork.data
      connectors ++= otherNetwork.connectors
      globalBuffer += otherNetwork.globalBuffer
      globalBufferSize += otherNetwork.globalBufferSize
      otherNetwork.data.values.foreach(node => {
        node.data match {
          case connector: Connector => connector.distributor = Some(wrapper)
          case _ =>
        }
        node.data.network = wrapper
      })

      Network.Edge(oldNode, node(addedNode))
    }

    for ((node, nodes) <- connects) nodes.foreach(_.asInstanceOf[MutableNode].onConnect(node))

    true
  }

  private def handleSplit(subGraphs: Seq[mutable.Map[String, Network.Vertex]]) =
    if (subGraphs.size > 1) {
      val nodes = subGraphs.map(_.values.map(_.data))
      val visibleNodes = nodes.map(_.filter(_.reachability == Visibility.Network))

      data.clear()
      connectors.clear()
      globalBuffer = 0
      globalBufferSize = 0
      data ++= subGraphs.head
      for (node <- data.values) node.data match {
        case connector: Connector => addConnector(connector)
        case _ =>
      }
      subGraphs.tail.foreach(new Network(_))

      for (indexA <- 0 until subGraphs.length) {
        val nodesA = nodes(indexA)
        val visibleNodesA = visibleNodes(indexA)
        for (indexB <- (indexA + 1) until subGraphs.length) {
          val nodesB = nodes(indexB)
          val visibleNodesB = visibleNodes(indexB)
          visibleNodesA.foreach(node => nodesB.foreach(_.onDisconnect(node)))
          visibleNodesB.foreach(node => nodesA.foreach(_.onDisconnect(node)))
        }
      }
    }

  private def send(source: ImmutableNode, targets: Iterable[ImmutableNode], name: String, args: AnyRef*) {
    val message = new Network.Message(source, name, Array(args: _*))
    targets.foreach(_.host.onMessage(message))
  }

  // ----------------------------------------------------------------------- //

  def addConnector(connector: Connector) {
    if (connector.localBufferSize > 0) {
      assert(!connectors.contains(connector))
      connectors += connector
      globalBuffer += connector.localBuffer
      globalBufferSize += connector.localBufferSize
    }
    connector.distributor = Some(wrapper)
  }

  def removeConnector(connector: Connector) {
    if (connector.localBufferSize > 0) {
      assert(connectors.contains(connector))
      connectors -= connector
      globalBuffer -= connector.localBuffer
      globalBufferSize -= connector.localBufferSize
    }
  }

  def changeBuffer(delta: Double): Double = {
    if (delta == 0) 0
    else if (Settings.get.ignorePower) {
      if (delta < 0) 0
      else /* if (delta > 0) */ delta
    }
    else this.synchronized {
      val oldBuffer = globalBuffer
      globalBuffer = math.min(math.max(globalBuffer + delta, 0), globalBufferSize)
      if (globalBuffer == oldBuffer) {
        return delta
      }
      if (delta < 0) {
        var remaining = -delta
        for (connector <- connectors if remaining > 0) {
          if (connector.localBuffer > 0) {
            if (connector.localBuffer < remaining) {
              remaining -= connector.localBuffer
              connector.localBuffer = 0
            }
            else {
              connector.localBuffer -= remaining
              remaining = 0
            }
          }
        }
        remaining
      }
      else /* if (delta > 0) */ {
        var remaining = delta
        for (connector <- connectors if remaining > 0) {
          if (connector.localBuffer < connector.localBufferSize) {
            val space = connector.localBufferSize - connector.localBuffer
            if (space < remaining) {
              remaining -= space
              connector.localBuffer = connector.localBufferSize
            }
            else {
              connector.localBuffer += remaining
              remaining = 0
            }
          }
        }
        remaining
      }
    }
  }
}

object Network extends api.detail.NetworkAPI {
  @ForgeSubscribe
  def onWorldLoad(e: WorldEvent.Load) {
    val world = e.world
    if (!world.isRemote) {
      for (t <- world.loadedTileEntityList) t match {
        case p: TileEntity with PassiveNode => p.getBlockType.updateTick(world, p.xCoord, p.yCoord, p.zCoord, world.rand)
        case _ =>
      }
    }
  }

  @ForgeSubscribe
  def onChunkLoad(e: ChunkEvent.Load) {
    val world = e.world
    if (!world.isRemote) {
      for (t <- e.getChunk.chunkTileEntityMap.values) t match {
        case p: TileEntity with PassiveNode => p.getBlockType.updateTick(world, p.xCoord, p.yCoord, p.zCoord, world.rand)
        case _ =>
      }
    }
  }

  override def joinOrCreateNetwork(tileEntity: TileEntity): Unit =
    if (!tileEntity.getWorldObj.isRemote) {
      for (side <- ForgeDirection.VALID_DIRECTIONS) {
        getNetworkNode(tileEntity, side) match {
          case Some(node: MutableNode) =>
            val (nx, ny, nz) = (
              tileEntity.xCoord + side.offsetX,
              tileEntity.yCoord + side.offsetY,
              tileEntity.zCoord + side.offsetZ)
            getNetworkNode(tileEntity.getWorldObj.getBlockTileEntity(nx, ny, nz), side.getOpposite) match {
              case Some(neighbor: MutableNode) if neighbor != node && neighbor.network != null => neighbor.connect(node)
              case _ => // Ignore.
            }
            if (node.network == null) {
              joinNewNetwork(node)
            }
          case _ => // No node for this side or bad environment.
        }
      }
    }

  def joinNewNetwork(node: ImmutableNode): Unit = node match {
    case mutableNode: MutableNode if mutableNode.network == null =>
      new Network(mutableNode)
    case _ =>
  }

  private def getNetworkNode(tileEntity: TileEntity, side: ForgeDirection) =
    tileEntity match {
      case host: SidedEnvironment => Option(host.sidedNode(side))
      case host: Environment => Some(host.node)
      case host: TileMultipart => host.partList.find(_.isInstanceOf[CablePart])
      case _ => None
    }

  // ----------------------------------------------------------------------- //

  def newNode(host: Environment, reachability: Visibility) = new NodeBuilder(host, reachability)

  class NodeBuilder(val _host: Environment, val _reachability: Visibility) extends api.detail.Builder.NodeBuilder {
    def withComponent(name: String, visibility: Visibility) = new Network.ComponentBuilder(_host, _reachability, name, visibility)

    def withComponent(name: String) = withComponent(name, _reachability)

    def withConnector(bufferSize: Double) = new Network.ConnectorBuilder(_host, _reachability, bufferSize)

    def withConnector() = withConnector(0)

    def create() = if (FMLCommonHandler.instance.getEffectiveSide == Side.SERVER) new MutableNode with NodeVarargPart {
      val host = _host
      val reachability = _reachability
    }
    else null
  }

  class ComponentBuilder(val _host: Environment, val _reachability: Visibility, val _name: String, val _visibility: Visibility) extends api.detail.Builder.ComponentBuilder {
    def withConnector(bufferSize: Double) = new Network.ComponentConnectorBuilder(_host, _reachability, _name, _visibility, bufferSize)

    def withConnector() = withConnector(0)

    def create() = if (FMLCommonHandler.instance.getEffectiveSide == Side.SERVER) new Component with NodeVarargPart {
      val host = _host
      val reachability = _reachability
      val name = _name
      setVisibility(_visibility)
    }
    else null
  }

  class ConnectorBuilder(val _host: Environment, val _reachability: Visibility, val _bufferSize: Double) extends api.detail.Builder.ConnectorBuilder {
    def withComponent(name: String, visibility: Visibility) = new Network.ComponentConnectorBuilder(_host, _reachability, name, visibility, _bufferSize)

    def withComponent(name: String) = withComponent(name, _reachability)

    def create() = if (FMLCommonHandler.instance.getEffectiveSide == Side.SERVER) new Connector with NodeVarargPart {
      val host = _host
      val reachability = _reachability
      var localBufferSize = _bufferSize
    }
    else null
  }

  class ComponentConnectorBuilder(val _host: Environment, val _reachability: Visibility, val _name: String, val _visibility: Visibility, val _bufferSize: Double) extends api.detail.Builder.ComponentConnectorBuilder {
    def create() = if (FMLCommonHandler.instance.getEffectiveSide == Side.SERVER) new ComponentConnector with NodeVarargPart {
      val host = _host
      val reachability = _reachability
      val name = _name
      var localBufferSize = _bufferSize
      setVisibility(_visibility)
    }
    else null
  }

  // ----------------------------------------------------------------------- //

  private class Vertex(val data: MutableNode) {
    val edges = ArrayBuffer.empty[Edge]

    def remove() = {
      edges.foreach(edge => edge.other(this).edges -= edge)
      searchGraphs(edges.map(_.other(this)))
    }
  }

  private case class Edge(left: Vertex, right: Vertex) {
    left.edges += this
    right.edges += this

    def other(side: Vertex) = if (side == left) right else left

    def isBetween(a: Vertex, b: Vertex) = (a == left && b == right) || (b == left && a == right)

    def remove() = {
      left.edges -= this
      right.edges -= this
      searchGraphs(List(left, right))
    }
  }

  private def searchGraphs(seeds: Seq[Vertex]) = {
    val seen = mutable.Set.empty[Vertex]
    seeds.map(seed => {
      if (seen.contains(seed)) None
      else {
        val addressed = mutable.Map.empty[String, Vertex]
        val queue = mutable.Queue(seed)
        while (queue.nonEmpty) {
          val node = queue.dequeue()
          seen += node
          addressed += node.data.address -> node
          queue ++= node.edges.map(_.other(node)).filter(n => !seen.contains(n) && !queue.contains(n))
        }
        Some(addressed)
      }
    }) filter (_.nonEmpty) map (_.get)
  }

  // ----------------------------------------------------------------------- //

  private class Message(val source: ImmutableNode, val name: String, val data: Array[AnyRef]) extends api.network.Message {
    var isCanceled = false

    def cancel() = isCanceled = true
  }

  // ----------------------------------------------------------------------- //

  private class Wrapper(val network: Network) extends api.network.Network with Distributor {
    def connect(nodeA: ImmutableNode, nodeB: ImmutableNode) =
      network.connect(nodeA.asInstanceOf[MutableNode], nodeB.asInstanceOf[MutableNode])

    def disconnect(nodeA: ImmutableNode, nodeB: ImmutableNode) =
      network.disconnect(nodeA.asInstanceOf[MutableNode], nodeB.asInstanceOf[MutableNode])

    def remove(node: ImmutableNode) = network.remove(node.asInstanceOf[MutableNode])

    def node(address: String) = network.node(address)

    def nodes = network.nodes.asJava

    def nodes(reference: ImmutableNode) = network.nodes(reference).asJava

    def neighbors(node: ImmutableNode) = network.neighbors(node).asJava

    def sendToAddress(source: ImmutableNode, target: String, name: String, data: AnyRef*) =
      network.sendToAddress(source, target, name, data: _*)

    def sendToNeighbors(source: ImmutableNode, name: String, data: AnyRef*) =
      network.sendToNeighbors(source, name, data: _*)

    def sendToReachable(source: ImmutableNode, name: String, data: AnyRef*) =
      network.sendToReachable(source, name, data: _*)

    def sendToVisible(source: ImmutableNode, name: String, data: AnyRef*) =
      network.sendToVisible(source, name, data: _*)

    def globalBuffer = network.globalBuffer

    def globalBuffer_=(value: Double) = network.globalBuffer = value

    def globalBufferSize = network.globalBufferSize

    def globalBufferSize_=(value: Double) = network.globalBufferSize = value

    def addConnector(connector: Connector) = network.addConnector(connector)

    def removeConnector(connector: Connector) = network.removeConnector(connector)

    def changeBuffer(delta: Double) = network.changeBuffer(delta)
  }

}
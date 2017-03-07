package li.cil.oc.server.network

import li.cil.oc.OpenComputers
import li.cil.oc.Settings
import li.cil.oc.api
import li.cil.oc.api.network
import li.cil.oc.api.network._
import li.cil.oc.api.network.{Node => ImmutableNode}
import li.cil.oc.common.capabilities.Capabilities
import li.cil.oc.common.tileentity
import li.cil.oc.server.network.{Node => MutableNode}
import li.cil.oc.util.{DyeUtils, SideTracker}
import net.minecraft.item.EnumDyeColor
import net.minecraft.nbt._
import net.minecraft.tileentity.TileEntity
import net.minecraft.util.EnumFacing
import net.minecraft.util.math.BlockPos
import net.minecraft.world.IBlockAccess

import scala.collection.JavaConverters._
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
    node.data.getNetwork = wrapper
  })

  // Called by nodes when they want to change address from loading.
  def remap(remappedNode: MutableNode, newAddress: String) {
    data.get(remappedNode.getAddress) match {
      case Some(node) =>
        val neighbors = node.edges.map(_.other(node))
        node.data.remove()
        node.data.getAddress = newAddress
        while (data.contains(node.data.getAddress)) {
          node.data.getAddress = java.util.UUID.randomUUID().toString
        }
        if (neighbors.isEmpty)
          addNew(node.data)
        else
          neighbors.foreach(_.data.connect(node.data))
      case _ => throw new AssertionError("Node believes it belongs to a network it doesn't.")
    }
  }

  // ----------------------------------------------------------------------- //

  def connect(nodeA: MutableNode, nodeB: MutableNode) = {
    if (nodeA == null) throw new NullPointerException("nodeA")
    if (nodeB == null) throw new NullPointerException("nodeB")

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
        if (oldNodeA.data.getReachability == Visibility.NEIGHBORS)
          oldNodeB.data.onConnect(oldNodeA.data)
        if (oldNodeB.data.getReachability == Visibility.NEIGHBORS)
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
        if (edge.left.data.getReachability == Visibility.NEIGHBORS)
          edge.right.data.onDisconnect(edge.left.data)
        if (edge.right.data.getReachability == Visibility.NEIGHBORS)
          edge.left.data.onDisconnect(edge.right.data)
        true
      case _ => false // That connection doesn't exists.
    }
  }

  def remove(node: MutableNode) = {
    data.remove(node.getAddress) match {
      case Some(entry) =>
        node match {
          case connector: Connector => removeConnector(connector)
          case _ =>
        }
        node.getNetwork = null
        val subGraphs = entry.remove()
        val targets = Iterable(node) ++ (entry.data.getReachability match {
          case Visibility.NONE => Iterable.empty[ImmutableNode]
          case Visibility.NEIGHBORS => entry.edges.map(_.other(entry).data)
          case Visibility.NETWORK => subGraphs.flatMap(_.values.map(_.data))
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

  def reachableNodes(reference: ImmutableNode): Iterable[ImmutableNode] = {
    val referenceNeighbors = neighbors(reference).toSet
    nodes.filter(node => node != reference && (node.getReachability == Visibility.NETWORK ||
      (node.getReachability == Visibility.NEIGHBORS && referenceNeighbors.contains(node))))
  }

  def reachingNodes(reference: ImmutableNode): Iterable[ImmutableNode] = {
    if (reference.getReachability == Visibility.NETWORK) nodes.filter(node => node != reference)
    else if (reference.getReachability == Visibility.NEIGHBORS) {
      val referenceNeighbors = neighbors(reference).toSet
      nodes.filter(node => node != reference && referenceNeighbors.contains(node))
    } else Iterable.empty
  }

  def neighbors(node: ImmutableNode): Iterable[ImmutableNode] = {
    data.get(node.getAddress) match {
      case Some(n) if n.data == node => n.edges.map(_.other(n).data)
      case _ => throw new IllegalArgumentException("Node must be in this network.")
    }
  }

  // ----------------------------------------------------------------------- //

  def sendToAddress(source: ImmutableNode, target: String, name: String, args: AnyRef*) = {
    if (source.getNetwork != wrapper)
      throw new IllegalArgumentException("Source node must be in this network.")
    data.get(target) match {
      case Some(node) if node.data.canBeReachedFrom(source) =>
        send(source, Iterable(node.data), name, args: _*)
      case _ =>
    }
  }

  def sendToNeighbors(source: ImmutableNode, name: String, args: AnyRef*) = {
    if (source.getNetwork != wrapper)
      throw new IllegalArgumentException("Source node must be in this network.")
    send(source, neighbors(source).filter(_.getReachability != Visibility.NONE), name, args: _*)
  }

  def sendToReachable(source: ImmutableNode, name: String, args: AnyRef*) = {
    if (source.getNetwork != wrapper)
      throw new IllegalArgumentException("Source node must be in this network.")
    send(source, reachableNodes(source), name, args: _*)
  }

  def sendToVisible(source: ImmutableNode, name: String, args: AnyRef*) = {
    if (source.getNetwork != wrapper)
      throw new IllegalArgumentException("Source node must be in this network.")
    send(source, reachableNodes(source) collect {
      case component: api.network.Component if component.canBeSeenFrom(source) => component
    }, name, args: _*)
  }

  // ----------------------------------------------------------------------- //

  private def contains(node: MutableNode) = node.getNetwork == wrapper && data.contains(node.getAddress)

  private def node(node: ImmutableNode) = data(node.getAddress)

  private def addNew(node: MutableNode) = {
    val newNode = new Network.Vertex(node)
    if (node.getAddress == null || data.contains(node.getAddress))
      node.getAddress = java.util.UUID.randomUUID().toString
    data += node.getAddress -> newNode
    node match {
      case connector: Connector => addConnector(connector)
      case _ =>
    }
    node.getNetwork = wrapper
    newNode
  }

  private def add(oldNode: Network.Vertex, addedNode: MutableNode): Boolean = {
    // Queue onConnect calls to avoid side effects from callbacks.
    val connects = mutable.Buffer.empty[(ImmutableNode, Iterable[ImmutableNode])]
    // Check if the other node is new or if we have to merge networks.
    if (addedNode.getNetwork == null) {
      val newNode = addNew(addedNode)
      Network.Edge(oldNode, newNode)
      addedNode.getReachability match {
        case Visibility.NONE =>
          connects += ((addedNode, Iterable(addedNode)))
        case Visibility.NEIGHBORS =>
          connects += ((addedNode, Iterable(addedNode) ++ neighbors(addedNode)))
          reachingNodes(addedNode).foreach(node => connects += ((node, Iterable(addedNode))))
        case Visibility.NETWORK =>
          // Explicitly send to the added node itself first.
          connects += ((addedNode, Iterable(addedNode) ++ nodes.filter(_ != addedNode)))
          reachingNodes(addedNode).foreach(node => connects += ((node, Iterable(addedNode))))
      }
    }
    else {
      val otherNetwork = addedNode.getNetwork.asInstanceOf[Network.Wrapper].network

      // If the other network contains nodes with addresses used in our local
      // network we'll have to re-assign those... since dynamically handling
      // changes to one's address is not expected of nodes / hosts, we have to
      // remove and reconnect the nodes. This is a pretty shitty solution, and
      // may break things slightly here and there (e.g. if this is the node of
      // a running machine the computer will most likely crash), but it should
      // never happen in normal operation anyway. It *can* happen when NBT
      // editing stuff or using mods to clone blocks (e.g. WorldEdit).
      val duplicates = otherNetwork.data.filter(entry => data.contains(entry._1)).values.toArray
      val otherNetworkAfterReaddress = if (duplicates.isEmpty) {
        otherNetwork
      } else {
        duplicates.foreach(vertex => {
          val node = vertex.data
          val neighbors = vertex.edges.map(_.other(vertex).data).toArray

          var newAddress = ""
          do {
            newAddress = java.util.UUID.randomUUID().toString
          } while (data.contains(newAddress) || otherNetwork.data.contains(newAddress))

          // This may lead to splits, which is the whole reason we have to
          // check the network of the other nodes after the readdressing.
          node.remove()
          node.getAddress = newAddress
          Network.joinNewNetwork(node)

          if (node.getAddress == newAddress) {
            neighbors.filter(_.getNetwork != null).foreach(_.connect(node))
          } else {
            OpenComputers.log.error("I can't see this happening any other way than someone directly setting node addresses, which they shouldn't. So yeah. Shit'll be borked. Deal with it.")
            node.remove() // well screw you then
          }
        })

        duplicates.head.data.getNetwork.asInstanceOf[Network.Wrapper].network
      }

      // The address change can theoretically cause the node to be kicked from
      // its old network (via onConnect callbacks), so we make sure it's still
      // in the same network. If it isn't we start over.
      if (addedNode.getNetwork != null && addedNode.getNetwork.asInstanceOf[Network.Wrapper].network == otherNetworkAfterReaddress) {
        if (addedNode.getReachability == Visibility.NEIGHBORS)
          connects += ((addedNode, Iterable(oldNode.data)))
        if (oldNode.data.getReachability == Visibility.NEIGHBORS)
          connects += ((oldNode.data, Iterable(addedNode)))

        val oldNodes = nodes
        val newNodes = otherNetworkAfterReaddress.nodes
        val oldVisibleNodes = oldNodes.filter(_.getReachability == Visibility.NETWORK)
        val newVisibleNodes = newNodes.filter(_.getReachability == Visibility.NETWORK)

        newVisibleNodes.foreach(node => connects += ((node, oldNodes)))
        oldVisibleNodes.foreach(node => connects += ((node, newNodes)))

        data ++= otherNetworkAfterReaddress.data
        connectors ++= otherNetworkAfterReaddress.connectors
        globalBuffer += otherNetworkAfterReaddress.globalBuffer
        globalBufferSize += otherNetworkAfterReaddress.globalBufferSize
        otherNetworkAfterReaddress.data.values.foreach(node => {
          node.data match {
            case connector: Connector => connector.distributor = Some(wrapper)
            case _ =>
          }
          node.data.getNetwork = wrapper
        })
        otherNetworkAfterReaddress.data.clear()
        otherNetworkAfterReaddress.connectors.clear()

        Network.Edge(oldNode, node(addedNode))
      }
      else add(oldNode, addedNode)
    }

    for ((node, nodes) <- connects) nodes.foreach(_.asInstanceOf[MutableNode].onConnect(node))

    true
  }

  private def handleSplit(subGraphs: Seq[mutable.Map[String, Network.Vertex]]) =
    if (subGraphs.size > 1) {
      val nodes = subGraphs.map(_.values.map(_.data))
      val visibleNodes = nodes.map(_.filter(_.getReachability == Visibility.NETWORK))

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

      for (indexA <- subGraphs.indices) {
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
    targets.foreach(_.getEnvironment.onMessage(message))
  }

  // ----------------------------------------------------------------------- //

  def addConnector(connector: Connector) {
    if (connector.getLocalBufferSize > 0) {
      assert(!connectors.contains(connector))
      connectors += connector
      globalBuffer += connector.getLocalBuffer
      globalBufferSize += connector.getLocalBufferSize
    }
    connector.distributor = Some(wrapper)
  }

  def removeConnector(connector: Connector) {
    if (connector.getLocalBufferSize > 0) {
      assert(connectors.contains(connector))
      val bufferSize = connector.getLocalBufferSize
      connector.setLocalBufferSize(0)
      connector.setLocalBufferSize(bufferSize)
      connectors -= connector
      globalBuffer -= connector.getLocalBuffer
      globalBufferSize -= connector.getLocalBufferSize
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
          if (connector.getLocalBuffer > 0) {
            if (connector.getLocalBuffer < remaining) {
              remaining -= connector.getLocalBuffer
              connector.getLocalBuffer = 0
            }
            else {
              connector.getLocalBuffer -= remaining
              remaining = 0
            }
          }
        }
        -remaining
      }
      else /* if (delta > 0) */ {
        var remaining = delta
        for (connector <- connectors if remaining > 0) {
          if (connector.getLocalBuffer < connector.getLocalBufferSize) {
            val space = connector.getLocalBufferSize - connector.getLocalBuffer
            if (space < remaining) {
              remaining -= space
              connector.getLocalBuffer = connector.getLocalBufferSize
            }
            else {
              connector.getLocalBuffer += remaining
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
  override def joinOrCreateNetwork(world: IBlockAccess, pos: BlockPos): Unit = {
    val tileEntity = world.getTileEntity(pos)
    if (tileEntity != null && !tileEntity.isInvalid && tileEntity.getWorld != null && !tileEntity.getWorld.isRemote) {
      for (side <- EnumFacing.values) {
        val npos = tileEntity.getPos.offset(side)
        if (tileEntity.getWorld.isBlockLoaded(npos)) {
          val localNode = getNetworkNode(tileEntity, side)
          val neighborTileEntity = tileEntity.getWorld.getTileEntity(npos)
          val neighborNode = getNetworkNode(neighborTileEntity, side.getOpposite)
          localNode match {
            case Some(node: MutableNode) =>
              neighborNode match {
                case Some(neighbor: MutableNode) if neighbor != node && neighbor.getNetwork != null =>
                  val canConnectColor = canConnectBasedOnColor(tileEntity, neighborTileEntity)
                  val canConnectIM = canConnectFromSideIM(tileEntity, side) && canConnectFromSideIM(neighborTileEntity, side.getOpposite)
                  if (canConnectColor && canConnectIM) neighbor.connect(node)
                  else node.disconnect(neighbor)
                case _ =>
              }
              if (node.getNetwork == null) {
                joinNewNetwork(node)
              }
            case _ =>
          }
        }
      }
    }
  }

  override def joinOrCreateNetwork(tileEntity: TileEntity): Unit = {
    if (tileEntity != null) {
      val world = tileEntity.getWorld
      val pos = tileEntity.getPos
      if (world != null && pos != null) {
        joinOrCreateNetwork(world, pos)
      }
    }
  }

  def joinNewNetwork(node: ImmutableNode): Unit = node match {
    case mutableNode: MutableNode if mutableNode.getNetwork == null =>
      new Network(mutableNode)
    case _ =>
  }

  def getNetworkNode(tileEntity: TileEntity, side: EnumFacing): Option[ImmutableNode] = {
    if (tileEntity != null) {
      if (tileEntity.hasCapability(Capabilities.SidedEnvironmentCapability, side)) {
        val host = tileEntity.getCapability(Capabilities.SidedEnvironmentCapability, side)
        if (host != null) return Option(host.sidedNode(side))
      }

      if (tileEntity.hasCapability(Capabilities.EnvironmentCapability, side)) {
        val host = tileEntity.getCapability(Capabilities.EnvironmentCapability, side)
        if (host != null) return Option(host.getNode)
      }
    }

    None
  }

  private def getConnectionColor(tileEntity: TileEntity): Int = {
    if (tileEntity != null) {
      if (tileEntity.hasCapability(Capabilities.ColoredCapability, null)) {
        val colored = tileEntity.getCapability(Capabilities.ColoredCapability, null)
        if (colored != null && colored.controlsConnectivity) return colored.getColor
      }
    }

    DyeUtils.rgbValues(EnumDyeColor.SILVER)
  }

  private def canConnectBasedOnColor(te1: TileEntity, te2: TileEntity) = {
    val (c1, c2) = (getConnectionColor(te1), getConnectionColor(te2))
    c1 == c2 || c1 == DyeUtils.rgbValues(EnumDyeColor.SILVER) || c2 == DyeUtils.rgbValues(EnumDyeColor.SILVER)
  }

  private def canConnectFromSideIM(tileEntity: TileEntity, side: EnumFacing) =
    tileEntity match {
      case im: tileentity.traits.ImmibisMicroblock => im.ImmibisMicroblocks_isSideOpen(side.ordinal)
      case _ => true
    }

  // ----------------------------------------------------------------------- //

  override def joinWirelessNetwork(endpoint: WirelessEndpoint) {
    WirelessNetwork.add(endpoint)
  }

  override def updateWirelessNetwork(endpoint: WirelessEndpoint) {
    WirelessNetwork.update(endpoint)
  }

  override def leaveWirelessNetwork(endpoint: WirelessEndpoint) {
    WirelessNetwork.remove(endpoint)
  }

  override def leaveWirelessNetwork(endpoint: WirelessEndpoint, dimension: Int) {
    WirelessNetwork.remove(endpoint, dimension)
  }

  // ----------------------------------------------------------------------- //

  override def sendWirelessPacket(source: WirelessEndpoint, strength: Double, packet: network.Packet) {
    for (endpoint <- WirelessNetwork.computeReachableFrom(source, strength)) {
      endpoint.receivePacket(packet, source)
    }
  }

  // ----------------------------------------------------------------------- //

  def newNode(host: Environment, reachability: Visibility) = new NodeBuilder(host, reachability)

  override def newPacket(source: String, destination: String, port: Int, data: Array[AnyRef]) = {
    val packet = new Packet(source, destination, port, data)
    // We do the size check here instead of in the constructor of the packet
    // itself to avoid errors when loading packets.
    if (packet.getSize > Settings.get.maxNetworkPacketSize) {
      throw new IllegalArgumentException("packet too big (max " + Settings.get.maxNetworkPacketSize + ")")
    }
    packet
  }

  override def newPacket(nbt: NBTTagCompound) = {
    val source = nbt.getString("source")
    val destination =
      if (nbt.hasKey("dest")) null
      else nbt.getString("dest")
    val port = nbt.getInteger("port")
    val ttl = nbt.getInteger("ttl")
    val data = (for (i <- 0 until nbt.getInteger("dataLength")) yield {
      if (nbt.hasKey("data" + i)) {
        nbt.getTag("data" + i) match {
          case tag: NBTTagByte => Boolean.box(tag.getByte == 1)
          case tag: NBTTagInt => Int.box(tag.getInt)
          case tag: NBTTagDouble => Double.box(tag.getDouble)
          case tag: NBTTagString => tag.getString: AnyRef
          case tag: NBTTagByteArray => tag.getByteArray
        }
      }
      else null
    }).toArray
    new Packet(source, destination, port, data, ttl)
  }

  var isServer = SideTracker.isServer _

  class NodeBuilder(val _host: Environment, val _reachability: Visibility) extends api.detail.Builder.NodeBuilder {
    def withComponent(name: String, visibility: Visibility) = new Network.ComponentBuilder(_host, _reachability, name, visibility)

    def withComponent(name: String) = withComponent(name, _reachability)

    def withConnector(bufferSize: Double) = new Network.ConnectorBuilder(_host, _reachability, bufferSize)

    def withConnector() = withConnector(0)

    def create() = if (isServer()) new MutableNode with NodeVarargPart {
      val host = _host
      val reachability = _reachability
    }
    else null
  }

  class ComponentBuilder(val _host: Environment, val _reachability: Visibility, val _name: String, val _visibility: Visibility) extends api.detail.Builder.ComponentBuilder {
    def withConnector(bufferSize: Double) = new Network.ComponentConnectorBuilder(_host, _reachability, _name, _visibility, bufferSize)

    def withConnector() = withConnector(0)

    def create() = if (isServer()) new Component with NodeVarargPart {
      val getEnvironment = _host
      val getReachability = _reachability
      val getName = _name
      setVisibility(_visibility)
    }
    else null
  }

  class ConnectorBuilder(val _host: Environment, val _reachability: Visibility, val _bufferSize: Double) extends api.detail.Builder.ConnectorBuilder {
    def withComponent(name: String, visibility: Visibility) = new Network.ComponentConnectorBuilder(_host, _reachability, name, visibility, _bufferSize)

    def withComponent(name: String) = withComponent(name, _reachability)

    def create() = if (isServer()) new Connector with NodeVarargPart {
      val getEnvironment = _host
      val getReachability = _reachability
      getLocalBufferSize = _bufferSize
    }
    else null
  }

  class ComponentConnectorBuilder(val _host: Environment, val _reachability: Visibility, val _name: String, val _visibility: Visibility, val _bufferSize: Double) extends api.detail.Builder.ComponentConnectorBuilder {
    def create() = if (isServer()) new ComponentConnector with NodeVarargPart {
      val getEnvironment = _host
      val getReachability = _reachability
      val getName = _name
      getLocalBufferSize = _bufferSize
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

    override def toString = s"$data [${edges.length}]"
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
          addressed += node.data.getAddress -> node
          queue ++= node.edges.map(_.other(node)).filter(n => !seen.contains(n) && !queue.contains(n))
        }
        Some(addressed)
      }
    }) filter (_.nonEmpty) map (_.get)
  }

  // ----------------------------------------------------------------------- //

  private class Message(val getSource: ImmutableNode, val getName: String, val getData: Array[AnyRef]) extends api.network.Message {
    var isCanceled = false

    def cancel() = isCanceled = true
  }

  // ----------------------------------------------------------------------- //

  class Packet(var getSource: String, var getDestination: String, var getPort: Int, var getData: Array[AnyRef], var getTTL: Int = 5) extends api.network.Packet {
    val getSize = Option(getData).fold(0)(values => {
      if (values.length > Settings.get.maxNetworkPacketParts) {
        throw new IllegalArgumentException("packet has too many parts")
      }
      values.length * 2 + values.foldLeft(0)((acc, arg) => {
        acc + (arg match {
          case null | Unit | None => 4
          case _: java.lang.Boolean => 4
          case _: java.lang.Byte => 4
          case _: java.lang.Short => 4
          case _: java.lang.Integer => 4
          case _: java.lang.Float => 8
          case _: java.lang.Double => 8
          case value: java.lang.String => value.length max 1
          case value: Array[Byte] => value.length max 1
          case value => throw new IllegalArgumentException(s"unsupported data type: $value (${value.getClass.getCanonicalName})")
        })
      })
    })

    override def getHop() = new Packet(getSource, getDestination, getPort, getData, getTTL - 1)

    override def save(nbt: NBTTagCompound) {
      nbt.setString("source", getSource)
      if (getDestination != null && !getDestination.isEmpty) {
        nbt.setString("dest", getDestination)
      }
      nbt.setInteger("port", getPort)
      nbt.setInteger("ttl", getTTL)
      nbt.setInteger("dataLength", getData.length)
      for (i <- getData.indices) getData(i) match {
        case null | Unit | None =>
        case value: java.lang.Boolean => nbt.setBoolean("data" + i, value)
        case value: java.lang.Integer => nbt.setInteger("data" + i, value)
        case value: java.lang.Double => nbt.setDouble("data" + i, value)
        case value: java.lang.String => nbt.setString("data" + i, value)
        case value: Array[Byte] => nbt.setByteArray("data" + i, value)
        case value => OpenComputers.log.warn("Unexpected type while saving network packet: " + value.getClass.getName)
      }
    }

    override def toString = s"{source = $getSource, destination = $getDestination, port = $getPort, data = [${getData.mkString(", ")}}]}"
  }

  // ----------------------------------------------------------------------- //

  private[network] class Wrapper(val network: Network) extends api.network.Network with Distributor {
    def connect(nodeA: ImmutableNode, nodeB: ImmutableNode) =
      network.connect(nodeA.asInstanceOf[MutableNode], nodeB.asInstanceOf[MutableNode])

    def disconnect(nodeA: ImmutableNode, nodeB: ImmutableNode) =
      network.disconnect(nodeA.asInstanceOf[MutableNode], nodeB.asInstanceOf[MutableNode])

    def remove(node: ImmutableNode) = network.remove(node.asInstanceOf[MutableNode])

    def node(address: String) = network.node(address)

    def nodes = network.nodes.asJava

    def nodes(reference: ImmutableNode) = network.reachableNodes(reference).asJava

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

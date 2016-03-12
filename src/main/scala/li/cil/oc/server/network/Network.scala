package li.cil.oc.server.network

/* FMP
import codechicken.lib.vec.Cuboid6
import codechicken.multipart.JNormalOcclusion
import codechicken.multipart.NormalOcclusionTest
import codechicken.multipart.TFacePart
import codechicken.multipart.TileMultipart
import li.cil.oc.common.block.Cable
import li.cil.oc.integration.fmp.CablePart
*/

import li.cil.oc.OpenComputers
import li.cil.oc.Settings
import li.cil.oc.api
import li.cil.oc.api.network
import li.cil.oc.api.network._
import li.cil.oc.api.network.{Node => ImmutableNode}
import li.cil.oc.common.tileentity
import li.cil.oc.integration.Mods
import li.cil.oc.server.network.{Node => MutableNode}
import li.cil.oc.util.SideTracker
import net.minecraft.item.EnumDyeColor
import net.minecraft.nbt._
import net.minecraft.tileentity.TileEntity
import net.minecraft.util.EnumFacing

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
    node.data.network = wrapper
  })

  // Called by nodes when they want to change address from loading.
  def remap(remappedNode: MutableNode, newAddress: String) {
    data.get(remappedNode.address) match {
      case Some(node) =>
        val neighbors = node.edges.map(_.other(node))
        node.data.remove()
        node.data.address = newAddress
        while (data.contains(node.data.address)) {
          node.data.address = java.util.UUID.randomUUID().toString
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
          case Visibility.Network => subGraphs.flatMap(_.values.map(_.data))
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
      case Some(n) if n.data == node => n.edges.map(_.other(n).data)
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

  private def contains(node: MutableNode) = node.network == wrapper && data.contains(node.address)

  private def node(node: ImmutableNode) = data(node.address)

  private def addNew(node: MutableNode) = {
    val newNode = new Network.Vertex(node)
    if (node.address == null || data.contains(node.address))
      node.address = java.util.UUID.randomUUID().toString
    data += node.address -> newNode
    node match {
      case connector: Connector => addConnector(connector)
      case _ =>
    }
    node.network = wrapper
    newNode
  }

  private def add(oldNode: Network.Vertex, addedNode: MutableNode): Boolean = {
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

          // This may lead to splits, which is the whole reason we have to
          // check the network of the other nodes after the readdressing.
          node.remove()

          do {
            node.address = java.util.UUID.randomUUID().toString
          } while (data.contains(node.address) || otherNetwork.data.contains(node.address))

          if (neighbors.isEmpty) {
            assert(otherNetwork.data.size == 1)
            Network.joinNewNetwork(node)
          } else {
            neighbors.foreach(_.connect(node))
          }
        })

        duplicates.head.data.network.asInstanceOf[Network.Wrapper].network
      }

      // The address change can theoretically cause the node to be kicked from
      // its old network (via onConnect callbacks), so we make sure it's still
      // in the same network. If it isn't we start over.
      if (addedNode.network != null && addedNode.network.asInstanceOf[Network.Wrapper].network == otherNetworkAfterReaddress) {
        if (addedNode.reachability == Visibility.Neighbors)
          connects += ((addedNode, Iterable(oldNode.data)))
        if (oldNode.data.reachability == Visibility.Neighbors)
          connects += ((oldNode.data, Iterable(addedNode)))

        val oldNodes = nodes
        val newNodes = otherNetworkAfterReaddress.nodes
        val oldVisibleNodes = oldNodes.filter(_.reachability == Visibility.Network)
        val newVisibleNodes = newNodes.filter(_.reachability == Visibility.Network)

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
          node.data.network = wrapper
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
        -remaining
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
  override def joinOrCreateNetwork(tileEntity: TileEntity): Unit =
    if (!tileEntity.isInvalid && !tileEntity.getWorld.isRemote) {
      for (side <- EnumFacing.values) {
        val npos = tileEntity.getPos.offset(side)
        if (tileEntity.getWorld.isBlockLoaded(npos)) {
          val localNode = getNetworkNode(tileEntity, side)
          val neighborTileEntity = tileEntity.getWorld.getTileEntity(npos)
          val neighborNode = getNetworkNode(neighborTileEntity, side.getOpposite)
          localNode match {
            case Some(node: MutableNode) =>
              neighborNode match {
                case Some(neighbor: MutableNode) if neighbor != node && neighbor.network != null =>
                  val canConnectColor = canConnectBasedOnColor(tileEntity, neighborTileEntity)
                  val canConnectFMP = !Mods.ForgeMultipart.isAvailable ||
                    (canConnectFromSideFMP(tileEntity, side) && canConnectFromSideFMP(neighborTileEntity, side.getOpposite))
                  val canConnectIM = canConnectFromSideIM(tileEntity, side) && canConnectFromSideIM(neighborTileEntity, side.getOpposite)
                  if (canConnectColor && canConnectFMP && canConnectIM) neighbor.connect(node)
                  else node.disconnect(neighbor)
                case _ =>
              }
              if (node.network == null) {
                joinNewNetwork(node)
              }
            case _ =>
          }
        }
      }
    }

  def joinNewNetwork(node: ImmutableNode): Unit = node match {
    case mutableNode: MutableNode if mutableNode.network == null =>
      new Network(mutableNode)
    case _ =>
  }

  def getNetworkNode(tileEntity: TileEntity, side: EnumFacing) =
    tileEntity match {
      case host: SidedEnvironment => Option(host.sidedNode(side))
      case host: Environment with SidedComponent =>
        if (host.canConnectNode(side)) Option(host.node)
        else None
      case host: Environment => Option(host.node)
      case host if Mods.ForgeMultipart.isAvailable => getMultiPartNode(host)
      case _ => None
    }

  private def getMultiPartNode(tileEntity: TileEntity) = None

  /* TODO FMP
    tileEntity match {
      case host: TileMultipart => host.partList.find(_.isInstanceOf[CablePart]) match {
        case Some(part: CablePart) => Some(part.node)
        case _ => None
      }
      case _ => None
    }
  */

  private def cableColor(tileEntity: TileEntity) =
    tileEntity match {
      case cable: tileentity.Cable => cable.color
      case _ =>
        if (Mods.ForgeMultipart.isAvailable) cableColorFMP(tileEntity)
        else EnumDyeColor.SILVER
    }

  private def cableColorFMP(tileEntity: TileEntity) = EnumDyeColor.SILVER

  /* TODO FMP
    tileEntity match {
      case host: TileMultipart => (host.partList collect {
        case cable: CablePart => cable.color
      }).headOption.getOrElse(Color.LightGray)
      case _ => Color.LightGray
    }
  */

  private def canConnectBasedOnColor(te1: TileEntity, te2: TileEntity) = {
    val (c1, c2) = (cableColor(te1), cableColor(te2))
    c1 == c2 || c1 == EnumDyeColor.SILVER || c2 == EnumDyeColor.SILVER
  }

  private def canConnectFromSideFMP(tileEntity: TileEntity, side: EnumFacing) = true

  /* TODO FMP
    tileEntity match {
      case host: TileMultipart =>
        host.partList.forall {
          case part: JNormalOcclusion if !part.isInstanceOf[CablePart] =>
            val ownBounds = Iterable(new Cuboid6(Cable.cachedBounds(side.flag)))
            val otherBounds = part.getOcclusionBoxes
            NormalOcclusionTest(ownBounds, otherBounds)
          case part: TFacePart => !part.solid(side.ordinal) || (part.getSlotMask & codechicken.multipart.PartMap.face(side.ordinal).mask) == 0
          case _ => true
        }
      case _ => true
    }
  */

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
    if (packet.size > Settings.get.maxNetworkPacketSize) {
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

  class NodeBuilder(val _host: Environment, val _reachability: Visibility) extends api.detail.Builder.NodeBuilder {
    def withComponent(name: String, visibility: Visibility) = new Network.ComponentBuilder(_host, _reachability, name, visibility)

    def withComponent(name: String) = withComponent(name, _reachability)

    def withConnector(bufferSize: Double) = new Network.ConnectorBuilder(_host, _reachability, bufferSize)

    def withConnector() = withConnector(0)

    def create() = if (SideTracker.isServer) new MutableNode with NodeVarargPart {
      val host = _host
      val reachability = _reachability
    }
    else null
  }

  class ComponentBuilder(val _host: Environment, val _reachability: Visibility, val _name: String, val _visibility: Visibility) extends api.detail.Builder.ComponentBuilder {
    def withConnector(bufferSize: Double) = new Network.ComponentConnectorBuilder(_host, _reachability, _name, _visibility, bufferSize)

    def withConnector() = withConnector(0)

    def create() = if (SideTracker.isServer) new Component with NodeVarargPart {
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

    def create() = if (SideTracker.isServer) new Connector with NodeVarargPart {
      val host = _host
      val reachability = _reachability
      localBufferSize = _bufferSize
    }
    else null
  }

  class ComponentConnectorBuilder(val _host: Environment, val _reachability: Visibility, val _name: String, val _visibility: Visibility, val _bufferSize: Double) extends api.detail.Builder.ComponentConnectorBuilder {
    def create() = if (SideTracker.isServer) new ComponentConnector with NodeVarargPart {
      val host = _host
      val reachability = _reachability
      val name = _name
      localBufferSize = _bufferSize
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

  class Packet(var source: String, var destination: String, var port: Int, var data: Array[AnyRef], var ttl: Int = 5) extends api.network.Packet {
    val size = Option(data).fold(0)(values => {
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
          case _ => throw new IllegalArgumentException("unsupported data type")
        })
      })
    })

    override def hop() = new Packet(source, destination, port, data, ttl - 1)

    override def save(nbt: NBTTagCompound) {
      nbt.setString("source", source)
      if (destination != null && !destination.isEmpty) {
        nbt.setString("dest", destination)
      }
      nbt.setInteger("port", port)
      nbt.setInteger("ttl", ttl)
      val dataArray = data.toArray
      nbt.setInteger("dataLength", dataArray.length)
      for (i <- dataArray.indices) dataArray(i) match {
        case null | Unit | None =>
        case value: java.lang.Boolean => nbt.setBoolean("data" + i, value)
        case value: java.lang.Integer => nbt.setInteger("data" + i, value)
        case value: java.lang.Double => nbt.setDouble("data" + i, value)
        case value: java.lang.String => nbt.setString("data" + i, value)
        case value: Array[Byte] => nbt.setByteArray("data" + i, value)
        case value => OpenComputers.log.warn("Unexpected type while saving network packet: " + value.getClass.getName)
      }
    }

    override def toString = s"{source = $source, destination = $destination, port = $port, data = [${data.mkString(", ")}}]}"
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
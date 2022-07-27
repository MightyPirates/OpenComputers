package li.cil.oc.common.tileentity

import java.util

import li.cil.oc.Settings
import li.cil.oc.api
import li.cil.oc.api.Driver
import li.cil.oc.api.component.RackMountable
import li.cil.oc.api.internal
import li.cil.oc.api.network.Analyzable
import li.cil.oc.api.network.Connector
import li.cil.oc.api.network.EnvironmentHost
import li.cil.oc.api.network.Message
import li.cil.oc.api.network.Node
import li.cil.oc.api.network.Packet
import li.cil.oc.api.network.Visibility
import li.cil.oc.api.util.StateAware
import li.cil.oc.common.Slot
import li.cil.oc.common.tileentity.traits.RedstoneChangedEventArgs
import li.cil.oc.integration.opencomputers.DriverRedstoneCard
import li.cil.oc.server.{PacketSender => ServerPacketSender}
import li.cil.oc.util.ExtendedInventory._
import li.cil.oc.util.ExtendedNBT._
import li.cil.oc.util.RotationHelper
import net.minecraft.entity.player.PlayerEntity
import net.minecraft.inventory.IInventory
import net.minecraft.item.ItemStack
import net.minecraft.nbt.CompoundNBT
import net.minecraft.nbt.IntArrayNBT
import net.minecraft.tileentity.TileEntity
import net.minecraft.tileentity.TileEntityType
import net.minecraft.util.Direction
import net.minecraftforge.common.util.Constants.NBT
import net.minecraftforge.api.distmarker.Dist
import net.minecraftforge.api.distmarker.OnlyIn

class Rack(selfType: TileEntityType[_ <: Rack]) extends TileEntity(selfType) with traits.PowerAcceptor with traits.Hub with traits.PowerBalancer
  with traits.ComponentInventory with traits.Rotatable with traits.BundledRedstoneAware with Analyzable with internal.Rack with traits.StateAware {

  var isRelayEnabled = false
  val lastData = new Array[CompoundNBT](getContainerSize)
  val hasChanged: Array[Boolean] = Array.fill(getContainerSize)(true)

  // Map node connections for each installed mountable. Each mountable may
  // have up to four outgoing connections, with the first one always being
  // the "primary" connection, i.e. being a direct connection allowing
  // component access (i.e. actually connecting to that side of the rack).
  // The other nodes are "secondary" connections and merely transfer network
  // messages.
  // mountable -> connectable -> side
  val nodeMapping: Array[Array[Option[Direction]]] = Array.fill(getContainerSize)(Array.fill[Option[Direction]](4)(None))
  val snifferNodes: Array[Array[Node]] = Array.fill(getContainerSize)(Array.fill(3)(api.Network.newNode(this, Visibility.Neighbors).create()))

  def connect(slot: Int, connectableIndex: Int, side: Option[Direction]): Unit = {
    val newSide = side match {
      case Some(direction) if direction != Direction.SOUTH => Option(direction)
      case _ => None
    }

    val oldSide = nodeMapping(slot)(connectableIndex)
    if (oldSide == newSide) return

    // Cut connection / remove sniffer node.
    val mountable = getMountable(slot)
    if (mountable != null && oldSide.isDefined) {
      if (connectableIndex == 0) {
        val node = mountable.node
        val plug = sidedNode(toGlobal(oldSide.get))
        if (node != null && plug != null) {
          node.disconnect(plug)
        }
      }
      else {
        snifferNodes(slot)(connectableIndex).remove()
      }
    }

    nodeMapping(slot)(connectableIndex) = newSide

    // Establish connection / add sniffer node.
    if (mountable != null && newSide.isDefined) {
      if (connectableIndex == 0) {
        val node = mountable.node
        val plug = sidedNode(toGlobal(newSide.get))
        if (node != null && plug != null) {
          node.connect(plug)
        }
      }
      else if (connectableIndex < mountable.getConnectableCount) {
        val connectable = mountable.getConnectableAt(connectableIndex)
        if (connectable != null && connectable.node != null) {
          if (connectable.node.network == null) {
            api.Network.joinNewNetwork(connectable.node)
          }
          connectable.node.connect(snifferNodes(slot)(connectableIndex))
        }
      }
    }
  }

  private def reconnect(plugSide: Direction): Unit = {
    for (slot <- 0 until getContainerSize) {
      val mapping = nodeMapping(slot)
      mapping(0) match {
        case Some(side) if toGlobal(side) == plugSide =>
          val mountable = getMountable(slot)
          val busNode = sidedNode(plugSide)
          if (busNode != null && mountable != null && mountable.node != null && busNode != mountable.node) {
            api.Network.joinNewNetwork(mountable.node)
            busNode.connect(mountable.node)
          }
        case _ => // Not connected to this side.
      }
      for (connectableIndex <- 0 until 3) {
        mapping(connectableIndex) match {
          case Some(side) if toGlobal(side) == plugSide =>
            val mountable = getMountable(slot)
            if (mountable != null && connectableIndex < mountable.getConnectableCount) {
              val connectable = mountable.getConnectableAt(connectableIndex)
              if (connectable != null && connectable.node != null) {
                if (connectable.node.network == null) {
                  api.Network.joinNewNetwork(connectable.node)
                }
                connectable.node.connect(snifferNodes(slot)(connectableIndex))
              }
            }
          case _ => // Not connected to this side.
        }
      }
    }
  }

  protected def sendPacketToMountables(sourceSide: Option[Direction], packet: Packet): Unit = {
    // When a message arrives on a bus, also send it to all secondary nodes
    // connected to it. Only deliver it to that very node, if it's not the
    // sender, to avoid loops.
    for (slot <- 0 until getContainerSize) {
      val mapping = nodeMapping(slot)
      for (connectableIndex <- 0 until 3) {
        mapping(connectableIndex + 1) match {
          case Some(side) if sourceSide.contains(toGlobal(side)) =>
            val mountable = getMountable(slot)
            if (mountable != null && connectableIndex < mountable.getConnectableCount) {
              val connectable = mountable.getConnectableAt(connectableIndex)
              if (connectable != null) {
                connectable.receivePacket(packet)
              }
            }
          case _ => // Not connected to a bus.
        }
      }
    }
  }

  // ----------------------------------------------------------------------- //
  // Hub

  override def tryEnqueuePacket(sourceSide: Option[Direction], packet: Packet): Boolean = {
    sendPacketToMountables(sourceSide, packet)
    if (isRelayEnabled)
      super.tryEnqueuePacket(sourceSide, packet)
    else
      true
  }

  override protected def relayPacket(sourceSide: Option[Direction], packet: Packet): Unit = {
    if (isRelayEnabled)
      super.relayPacket(sourceSide, packet)
  }

  override protected def onPlugConnect(plug: Plug, node: Node): Unit = {
    super.onPlugConnect(plug, node)
    connectComponents()
    reconnect(plug.side)
  }

  protected override def createNode(plug: Plug): Node = api.Network.newNode(plug, Visibility.Network)
    .withConnector(Settings.get.bufferDistributor)
    .create()

  // ----------------------------------------------------------------------- //
  // Environment

  override def dispose(): Unit = {
    super.dispose()
    disconnectComponents()
  }

  override def onMessage(message: Message): Unit = {
    super.onMessage(message)
    if (message.name == "network.message") message.data match {
      case Array(packet: Packet) => relayIfMessageFromConnectable(message, packet)
      case _ =>
    }
  }

  private def relayIfMessageFromConnectable(message: Message, packet: Packet): Unit = {
    for (slot <- 0 until getContainerSize) {
      val mountable = getMountable(slot)
      if (mountable != null) {
        val mapping = nodeMapping(slot)
        for (connectableIndex <- 0 until 3) {
          mapping(connectableIndex + 1) match {
            case Some(side) =>
              if (connectableIndex < mountable.getConnectableCount) {
                val connectable = mountable.getConnectableAt(connectableIndex)
                if (connectable != null && connectable.node == message.source) {
                  sidedNode(toGlobal(side)).sendToReachable("network.message", packet)
                  relayToConnectablesOnSide(message, packet, side)
                  return
                }
              }
            case _ => // Not connected to a bus.
          }
        }
      }
    }
  }

  private def relayToConnectablesOnSide(message: Message, packet: Packet, sourceSide: Direction): Unit = {
    for (slot <- 0 until getContainerSize) {
      val mountable = getMountable(slot)
      if (mountable != null) {
        val mapping = nodeMapping(slot)
        for (connectableIndex <- 0 until 3) {
          mapping(connectableIndex + 1) match {
            case Some(side) if side == sourceSide =>
              if (connectableIndex < mountable.getConnectableCount) {
                val connectable = mountable.getConnectableAt(connectableIndex)
                if (connectable != null && connectable.node != message.source) {
                  snifferNodes(slot)(connectableIndex).sendToNeighbors("network.message", packet)
                }
              }
            case _ => // Not connected to a bus.
          }
        }
      }
    }
  }

  // ----------------------------------------------------------------------- //
  // SidedEnvironment

  override def canConnect(side: Direction): Boolean = side != facing

  override def sidedNode(side: Direction): Node = if (side != facing) super.sidedNode(side) else null

  // ----------------------------------------------------------------------- //
  // power.Common

  @OnlyIn(Dist.CLIENT)
  override protected def hasConnector(side: Direction): Boolean = side != facing

  override protected def connector(side: Direction) = Option(if (side != facing) sidedNode(side).asInstanceOf[Connector] else null)

  override def energyThroughput: Double = Settings.get.serverRackRate

  // ----------------------------------------------------------------------- //
  // Analyzable

  override def onAnalyze(player: PlayerEntity, side: Direction, hitX: Float, hitY: Float, hitZ: Float): Array[Node] = {
    slotAt(side, hitX, hitY, hitZ) match {
      case Some(slot) => components(slot) match {
        case Some(analyzable: Analyzable) => analyzable.onAnalyze(player, side, hitX, hitY, hitZ)
        case _ => null
      }
      case _ => Array(sidedNode(side))
    }
  }

  // ----------------------------------------------------------------------- //
  // internal.Rack

  override def indexOfMountable(mountable: RackMountable): Int = components.indexWhere(_.contains(mountable))

  override def getMountable(slot: Int): RackMountable = components(slot) match {
    case Some(mountable: RackMountable) => mountable
    case _ => null
  }

  override def getMountableData(slot: Int): CompoundNBT = lastData(slot)

  override def markChanged(slot: Int): Unit = {
    hasChanged.synchronized(hasChanged(slot) = true)
    setOutputEnabled(hasRedstoneCard)
  }

  // ----------------------------------------------------------------------- //
  // StateAware

  override def getCurrentState: util.EnumSet[StateAware.State] = {
    val result = util.EnumSet.noneOf(classOf[api.util.StateAware.State])
    components.collect {
      case Some(mountable: RackMountable) => result.addAll(mountable.getCurrentState)
    }
    result
  }

  // ----------------------------------------------------------------------- //
  // Rotatable

  override protected def onRotationChanged() {
    super.onRotationChanged()
    checkRedstoneInputChanged()
  }

  // ----------------------------------------------------------------------- //
  // RedstoneAware

  override protected def onRedstoneInputChanged(args: RedstoneChangedEventArgs) {
    super.onRedstoneInputChanged(args)
    components.collect {
      case Some(mountable: RackMountable) if mountable.node != null =>
        val toLocalArgs = RedstoneChangedEventArgs(toLocal(args.side), args.oldValue, args.newValue, args.color)
        mountable.node.sendToNeighbors("redstone.changed", toLocalArgs)
    }
  }

  // ----------------------------------------------------------------------- //
  // IInventory

  override def getContainerSize = 4

  override def getMaxStackSize = 1

  override def canPlaceItem(slot: Int, stack: ItemStack): Boolean = (slot, Option(Driver.driverFor(stack, getClass))) match {
    case (_, Some(driver)) => driver.slot(stack) == Slot.RackMountable
    case _ => false
  }

  override def setChanged() {
    super.setChanged()
    if (isServer) {
      setOutputEnabled(hasRedstoneCard)
      ServerPacketSender.sendRackInventory(this)
    }
    else {
      getLevel.sendBlockUpdated(getBlockPos, getLevel.getBlockState(getBlockPos), getLevel.getBlockState(getBlockPos), 3)
    }
  }

  // ----------------------------------------------------------------------- //
  // ComponentInventory

  override protected def onItemAdded(slot: Int, stack: ItemStack): Unit = {
    if (isServer) {
      for (connectable <- 0 until 4) {
        nodeMapping(slot)(connectable) = None
      }
      lastData(slot) = null
      hasChanged(slot) = true
    }
    super.onItemAdded(slot, stack)
  }

  override protected def onItemRemoved(slot: Int, stack: ItemStack): Unit = {
    if (isServer) {
      for (connectable <- 0 until 4) {
        nodeMapping(slot)(connectable) = None
      }
      lastData(slot) = null
    }
    super.onItemRemoved(slot, stack)
  }

  override protected def connectItemNode(node: Node): Unit = {
    // By default create a new network for mountables. They have to
    // be wired up manually (mapping is reset in onItemAdded).
    api.Network.joinNewNetwork(node)
  }

  // ----------------------------------------------------------------------- //
  // TileEntity

  override def updateEntity() {
    super.updateEntity()
    if (isServer && isConnected) {
      lazy val connectors = Direction.values.map(sidedNode).collect {
        case connector: Connector => connector
      }
      components.zipWithIndex.collect {
        case (Some(mountable: RackMountable), slot) =>
          if (hasChanged(slot)) {
            hasChanged(slot) = false
            lastData(slot) = mountable.getData
            ServerPacketSender.sendRackMountableData(this, slot)
            getLevel.updateNeighborsAt(getBlockPos, getBlockState.getBlock)
            // These are working state dependent, so recompute them.
            setOutputEnabled(hasRedstoneCard)
          }

          // Power mountables without requiring them to be connected to the outside.
          mountable.node match {
            case connector: Connector =>
              var remaining = Settings.get.serverRackRate
              for (outside <- connectors if remaining > 0) {
                val received = remaining + outside.changeBuffer(-remaining)
                val rejected = connector.changeBuffer(received)
                outside.changeBuffer(rejected)
                remaining -= received - rejected
              }
            case _ => // Nothing using energy.
          }
      }

      updateComponents()
    }
  }

  // ----------------------------------------------------------------------- //

  private final val IsRelayEnabledTag = Settings.namespace + "isRelayEnabled"
  private final val NodeMappingTag = Settings.namespace + "nodeMapping"
  private final val LastDataTag = Settings.namespace + "lastData"
  private final val RackDataTag = Settings.namespace + "rackData"

  override def loadForServer(nbt: CompoundNBT): Unit = {
    super.loadForServer(nbt)

    isRelayEnabled = nbt.getBoolean(IsRelayEnabledTag)
    nbt.getList(NodeMappingTag, NBT.TAG_INT_ARRAY).map((buses: IntArrayNBT) =>
      buses.getAsIntArray.map(id => if (id < 0 || id == Direction.SOUTH.ordinal()) None else Option(Direction.from3DDataValue(id)))).
      copyToArray(nodeMapping)

    // Kickstart initialization.
    _isOutputEnabled = hasRedstoneCard
  }

  override def saveForServer(nbt: CompoundNBT): Unit = {
    super.saveForServer(nbt)

    nbt.putBoolean(IsRelayEnabledTag, isRelayEnabled)
    nbt.setNewTagList(NodeMappingTag, nodeMapping.map(buses =>
      toNbt(buses.map(side => side.fold(-1)(_.ordinal())))))
  }

  @OnlyIn(Dist.CLIENT) override
  def loadForClient(nbt: CompoundNBT): Unit = {
    super.loadForClient(nbt)

    val data = nbt.getList(LastDataTag, NBT.TAG_COMPOUND).
      toTagArray[CompoundNBT]
    data.copyToArray(lastData)
    loadData(nbt.getCompound(RackDataTag))
    connectComponents()
  }

  override def saveForClient(nbt: CompoundNBT): Unit = {
    super.saveForClient(nbt)

    val data = lastData.map(tag => if (tag == null) new CompoundNBT() else tag)
    nbt.setNewTagList(LastDataTag, data)
    nbt.setNewCompoundTag(RackDataTag, saveData)
  }

  // ----------------------------------------------------------------------- //

  def slotAt(side: Direction, hitX: Float, hitY: Float, hitZ: Float): Option[Int] = {
    if (side == facing) {
      val globalY = (hitY * 16).toInt // [0, 15]
      val l = 2
      val h = 14
      val slot = ((15 - globalY) - l) * getContainerSize / (h - l)
      Some(math.max(0, math.min(getContainerSize - 1, slot)))
    }
    else None
  }

  def isWorking(mountable: RackMountable): Boolean = mountable.getCurrentState.contains(api.util.StateAware.State.IsWorking)

  def hasRedstoneCard: Boolean = components.exists {
    case Some(mountable: EnvironmentHost with RackMountable with IInventory) if isWorking(mountable) =>
      mountable.exists(stack => DriverRedstoneCard.worksWith(stack, mountable.getClass))
    case _ => false
  }
}

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
import li.cil.oc.common.Slot
import li.cil.oc.common.tileentity.capabilities.RotatableImpl
import li.cil.oc.integration.opencomputers.DriverRedstoneCard
import li.cil.oc.server.{PacketSender => ServerPacketSender}
import li.cil.oc.util.ExtendedInventory._
import li.cil.oc.util.ExtendedNBT._
import net.minecraft.entity.player.EntityPlayer
import net.minecraft.inventory.IInventory
import net.minecraft.item.ItemStack
import net.minecraft.nbt.NBTTagCompound
import net.minecraft.nbt.NBTTagIntArray
import net.minecraft.util.EnumFacing
import net.minecraftforge.common.util.Constants.NBT
import net.minecraftforge.fml.relauncher.Side
import net.minecraftforge.fml.relauncher.SideOnly

class Rack extends traits.PowerAcceptor with traits.Hub with traits.PowerBalancer with traits.ComponentInventory with RotatableImpl with traits.BundledRedstoneAware with Analyzable with internal.Rack with traits.StateAware {
  var isRelayEnabled = true
  val lastData = new Array[NBTTagCompound](getSizeInventory)
  val hasChanged = Array.fill(getSizeInventory)(true)

  // Map node connections for each installed mountable. Each mountable may
  // have up to four outgoing connections, with the first one always being
  // the "primary" connection, i.e. being a direct connection allowing
  // component access (i.e. actually connecting to that side of the rack).
  // The other nodes are "secondary" connections and merely transfer network
  // messages.
  // mountable -> connectable -> side
  val nodeMapping = Array.fill(getSizeInventory)(Array.fill[Option[EnumFacing]](4)(None))
  val snifferNodes = Array.fill(getSizeInventory)(Array.fill(3)(api.Network.newNode(this, Visibility.NEIGHBORS).create()))

  def connect(slot: Int, connectableIndex: Int, side: Option[EnumFacing]): Unit = {
    val newSide = side match {
      case Some(direction) if direction != EnumFacing.SOUTH => Option(direction)
      case _ => None
    }

    val oldSide = nodeMapping(slot)(connectableIndex)
    if (oldSide == newSide) return

    // Cut connection / remove sniffer node.
    val mountable = getMountable(slot)
    if (mountable != null && oldSide.isDefined) {
      if (connectableIndex == 0) {
        val node = mountable.getNode
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
        val node = mountable.getNode
        val plug = sidedNode(toGlobal(newSide.get))
        if (node != null && plug != null) {
          node.connect(plug)
        }
      }
      else if (connectableIndex < mountable.getConnectableCount) {
        val connectable = mountable.getConnectableAt(connectableIndex)
        if (connectable != null && connectable.getNode != null) {
          if (connectable.getNode.getNetwork == null) {
            api.Network.joinNewNetwork(connectable.getNode)
          }
          connectable.getNode.connect(snifferNodes(slot)(connectableIndex))
        }
      }
    }
  }

  private def reconnect(plugSide: EnumFacing): Unit = {
    for (slot <- 0 until getSizeInventory) {
      val mapping = nodeMapping(slot)
      mapping(0) match {
        case Some(side) if toGlobal(side) == plugSide =>
          val mountable = getMountable(slot)
          val busNode = sidedNode(plugSide)
          if (busNode != null && mountable != null && mountable.getNode != null && busNode != mountable.getNode) {
            api.Network.joinNewNetwork(mountable.getNode)
            busNode.connect(mountable.getNode)
          }
        case _ => // Not connected to this side.
      }
      for (connectableIndex <- 0 until 3) {
        mapping(connectableIndex) match {
          case Some(side) if toGlobal(side) == plugSide =>
            val mountable = getMountable(slot)
            if (mountable != null && connectableIndex < mountable.getConnectableCount) {
              val connectable = mountable.getConnectableAt(connectableIndex)
              if (connectable != null && connectable.getNode != null) {
                if (connectable.getNode.getNetwork == null) {
                  api.Network.joinNewNetwork(connectable.getNode)
                }
                connectable.getNode.connect(snifferNodes(slot)(connectableIndex))
              }
            }
          case _ => // Not connected to this side.
        }
      }
    }
  }

  // ----------------------------------------------------------------------- //
  // Hub

  override protected def relayPacket(sourceSide: Option[EnumFacing], packet: Packet): Unit = {
    if (isRelayEnabled) super.relayPacket(sourceSide, packet)

    // When a message arrives on a bus, also send it to all secondary nodes
    // connected to it. Only deliver it to that very node, if it's not the
    // sender, to avoid loops.
    for (slot <- 0 until getSizeInventory) {
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

  override protected def onPlugConnect(plug: Plug, node: Node): Unit = {
    super.onPlugConnect(plug, node)
    connectComponents()
    reconnect(plug.side)
  }

  protected override def createNode(plug: Plug): Node = api.Network.newNode(plug, Visibility.NETWORK)
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
    if (message.getName == "network.message") message.getData match {
      case Array(packet: Packet) => relayIfMessageFromConnectable(message, packet)
      case _ =>
    }
  }

  private def relayIfMessageFromConnectable(message: Message, packet: Packet): Unit = {
    for (slot <- 0 until getSizeInventory) {
      val mountable = getMountable(slot)
      if (mountable != null) {
        val mapping = nodeMapping(slot)
        for (connectableIndex <- 0 until 3) {
          mapping(connectableIndex + 1) match {
            case Some(side) =>
              if (connectableIndex < mountable.getConnectableCount) {
                val connectable = mountable.getConnectableAt(connectableIndex)
                if (connectable != null && connectable.getNode == message.getSource) {
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

  private def relayToConnectablesOnSide(message: Message, packet: Packet, sourceSide: EnumFacing): Unit = {
    for (slot <- 0 until getSizeInventory) {
      val mountable = getMountable(slot)
      if (mountable != null) {
        val mapping = nodeMapping(slot)
        for (connectableIndex <- 0 until 3) {
          mapping(connectableIndex + 1) match {
            case Some(side) if side == sourceSide =>
              if (connectableIndex < mountable.getConnectableCount) {
                val connectable = mountable.getConnectableAt(connectableIndex)
                if (connectable != null && connectable.getNode != message.getSource) {
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

  override def canConnect(side: EnumFacing) = side != getFacing

  override def sidedNode(side: EnumFacing): Node = if (side != getFacing) super.sidedNode(side) else null

  // ----------------------------------------------------------------------- //
  // power.Common

  @SideOnly(Side.CLIENT)
  override protected def hasConnector(side: EnumFacing) = side != getFacing

  override protected def connector(side: EnumFacing) = Option(if (side != getFacing) sidedNode(side).asInstanceOf[Connector] else null)

  override def energyThroughput = Settings.get.serverRackRate

  // ----------------------------------------------------------------------- //
  // Analyzable

  override def onAnalyze(player: EntityPlayer, side: EnumFacing, hitX: Float, hitY: Float, hitZ: Float): Array[Node] = {
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

  override def getMountableData(slot: Int): NBTTagCompound = lastData(slot)

  override def markChanged(slot: Int): Unit = {
    hasChanged.synchronized(hasChanged(slot) = true)
    isOutputEnabled = hasRedstoneCard
  }

  // ----------------------------------------------------------------------- //
  // StateAware

  override def getCurrentState = {
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
  // RedstoneAwareImpl

  override protected def onRedstoneInputChanged(side: EnumFacing, oldMaxValue: Int, newMaxValue: Int) {
    super.onRedstoneInputChanged(side, oldMaxValue, newMaxValue)
    components.collect {
      case Some(mountable: RackMountable) if mountable.getNode != null =>
        mountable.getNode.sendToNeighbors("redstone.changed", toLocal(side), int2Integer(oldMaxValue), int2Integer(newMaxValue))
    }
  }

  // ----------------------------------------------------------------------- //
  // IInventory

  override def getSizeInventory = 4

  override def getInventoryStackLimit = 1

  override def isItemValidForSlot(slot: Int, stack: ItemStack): Boolean = (slot, Option(Driver.driverFor(stack, getClass))) match {
    case (_, Some(driver)) => driver.slot(stack) == Slot.RackMountable
    case _ => false
  }

  override def markDirty() {
    super.markDirty()
    if (isServer) {
      isOutputEnabled = hasRedstoneCard
      ServerPacketSender.sendRackInventory(this)
    }
    else {
      getWorld.notifyBlockUpdate(getPos, getWorld.getBlockState(getPos), getWorld.getBlockState(getPos), 3)
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
      lazy val connectors = EnumFacing.VALUES.map(sidedNode).collect {
        case connector: Connector => connector
      }
      components.zipWithIndex.collect {
        case (Some(mountable: RackMountable), slot) =>
          if (hasChanged(slot)) {
            hasChanged(slot) = false
            lastData(slot) = mountable.getData
            ServerPacketSender.sendRackMountableData(this, slot)
            getWorld.notifyNeighborsOfStateChange(getPos, getBlockType, false)
            // These are working state dependent, so recompute them.
            isOutputEnabled = hasRedstoneCard
          }

          // Power mountables without requiring them to be connected to the outside.
          mountable.getNode match {
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

  override def readFromNBTForServer(nbt: NBTTagCompound): Unit = {
    super.readFromNBTForServer(nbt)

    isRelayEnabled = nbt.getBoolean(IsRelayEnabledTag)
    nbt.getTagList(NodeMappingTag, NBT.TAG_INT_ARRAY).map((buses: NBTTagIntArray) =>
      buses.getIntArray.map(id => if (id < 0 || id == EnumFacing.SOUTH.ordinal()) None else Option(EnumFacing.getFront(id)))).
      copyToArray(nodeMapping)

    // Kickstart initialization.
    _isOutputEnabled = hasRedstoneCard
  }

  override def writeToNBTForServer(nbt: NBTTagCompound): Unit = {
    super.writeToNBTForServer(nbt)

    nbt.setBoolean(IsRelayEnabledTag, isRelayEnabled)
    nbt.setNewTagList(NodeMappingTag, nodeMapping.map(buses =>
      toNbt(buses.map(side => side.fold(-1)(_.ordinal())))))
  }

  @SideOnly(Side.CLIENT) override
  def readFromNBTForClient(nbt: NBTTagCompound): Unit = {
    super.readFromNBTForClient(nbt)

    val data = nbt.getTagList(LastDataTag, NBT.TAG_COMPOUND).
      toArray[NBTTagCompound]
    data.copyToArray(lastData)
    load(nbt.getCompoundTag(RackDataTag))
    connectComponents()
  }

  override def writeToNBTForClient(nbt: NBTTagCompound): Unit = {
    super.writeToNBTForClient(nbt)

    val data = lastData.map(tag => if (tag == null) new NBTTagCompound() else tag)
    nbt.setNewTagList(LastDataTag, data)
    nbt.setNewCompoundTag(RackDataTag, save)
  }

  // ----------------------------------------------------------------------- //

  def slotAt(side: EnumFacing, hitX: Float, hitY: Float, hitZ: Float) = {
    if (side == getFacing) {
      val globalY = (hitY * 16).toInt // [0, 15]
      val l = 2
      val h = 14
      val slot = ((15 - globalY) - l) * getSizeInventory / (h - l)
      Some(math.max(0, math.min(getSizeInventory - 1, slot)))
    }
    else None
  }

  def isWorking(mountable: RackMountable) = mountable.getCurrentState.contains(api.util.StateAware.State.IsWorking)

  def hasRedstoneCard = components.exists {
    case Some(mountable: EnvironmentHost with RackMountable with IInventory) if isWorking(mountable) =>
      mountable.exists(stack => DriverRedstoneCard.worksWith(stack, mountable.getClass))
    case _ => false
  }
}

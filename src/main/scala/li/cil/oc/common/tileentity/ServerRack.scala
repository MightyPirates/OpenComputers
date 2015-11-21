package li.cil.oc.common.tileentity

import java.util

import com.google.common.base.Strings
import cpw.mods.fml.common.Optional.Method
import cpw.mods.fml.common.eventhandler.SubscribeEvent
import cpw.mods.fml.relauncher.Side
import cpw.mods.fml.relauncher.SideOnly
import li.cil.oc._
import li.cil.oc.api.Network
import li.cil.oc.api.internal
import li.cil.oc.api.network.Analyzable
import li.cil.oc.api.network._
import li.cil.oc.client.Sound
import li.cil.oc.common.Tier
import li.cil.oc.integration.Mods
import li.cil.oc.integration.opencomputers.DriverRedstoneCard
import li.cil.oc.integration.stargatetech2.DriverAbstractBusCard
import li.cil.oc.integration.util.Waila
import li.cil.oc.server.component
import li.cil.oc.server.{PacketSender => ServerPacketSender}
import li.cil.oc.util.ExtendedNBT._
import net.minecraft.entity.player.EntityPlayer
import net.minecraft.item.ItemStack
import net.minecraft.nbt.NBTTagCompound
import net.minecraft.nbt.NBTTagString
import net.minecraftforge.common.util.Constants.NBT
import net.minecraftforge.common.util.ForgeDirection
import net.minecraftforge.event.world.WorldEvent

import scala.collection.mutable

class ServerRack extends traits.PowerAcceptor with traits.Hub with traits.PowerBalancer with traits.Inventory with traits.Rotatable with traits.BundledRedstoneAware with traits.AbstractBusAware with Analyzable with internal.ServerRack with traits.StateAware {
  val servers = Array.fill(getSizeInventory)(None: Option[component.Server])

  val sides = Seq(Option(ForgeDirection.UP), Option(ForgeDirection.EAST), Option(ForgeDirection.WEST), Option(ForgeDirection.DOWN)).
    padTo(servers.length, None).toArray

  val terminals = servers.indices.map(new common.component.Terminal(this, _)).toArray

  var range = 16

  // For client side, where we don't create the component.
  private val _isRunning = new Array[Boolean](getSizeInventory)
  private val _hasErrored = new Array[Boolean](getSizeInventory)

  private var markChunkDirty = false

  var internalSwitch = false

  // For client side rendering.
  var isPresent = Array.fill[Option[String]](getSizeInventory)(None)

  // Used on client side to check whether to render disk activity indicators.
  var lastAccess = Array.fill(4)(0L)

  val builtInSwitchTier = Settings.get.serverRackSwitchTier
  relayDelay = math.max(1, relayBaseDelay - ((builtInSwitchTier + 1) * relayDelayPerUpgrade).toInt)
  relayAmount = math.max(1, relayBaseAmount + (builtInSwitchTier + 1) * relayAmountPerUpgrade)
  maxQueueSize = math.max(1, queueBaseSize + (builtInSwitchTier + 1) * queueSizePerUpgrade)

  override def server(slot: Int) = servers(slot).orNull

  @SideOnly(Side.CLIENT)
  override protected def hasConnector(side: ForgeDirection) = side != facing

  override protected def connector(side: ForgeDirection) = Option(if (side != facing) sidedNode(side).asInstanceOf[Connector] else null)

  override def energyThroughput = Settings.get.serverRackRate

  override def getWorld = world

  // ----------------------------------------------------------------------- //

  override def canConnect(side: ForgeDirection) = side != facing

  override def sidedNode(side: ForgeDirection): Node = if (side != facing) super.sidedNode(side) else null

  @Method(modid = Mods.IDs.StargateTech2)
  override def getInterfaces(side: Int) = if (side != facing.ordinal) {
    super.getInterfaces(side)
  }
  else null

  // ----------------------------------------------------------------------- //

  def isRunning(number: Int) =
    if (isServer) servers(number).fold(false)(_.machine.isRunning)
    else _isRunning(number)

  @SideOnly(Side.CLIENT)
  def setRunning(number: Int, value: Boolean): Unit = {
    _isRunning(number) = value
    if (!value) {
      _hasErrored(number) = false
    }
    world.markBlockForUpdate(x, y, z)
    if (anyRunning) Sound.startLoop(this, "computer_running", 1.5f, 50 + world.rand.nextInt(50))
    else Sound.stopLoop(this)
  }

  def anyRunning = servers.indices.exists(isRunning)

  def hasErrored(number: Int) =
    if (isServer) servers(number).fold(false)(_.machine.lastError != null)
    else _hasErrored(number)

  @SideOnly(Side.CLIENT)
  def setErrored(number: Int, value: Boolean): Unit = {
    _hasErrored(number) = value
  }

  def anyErrored = servers.indices.exists(hasErrored)

  override def currentState = {
    if (anyRunning) util.EnumSet.of(traits.State.IsWorking)
    else util.EnumSet.noneOf(classOf[traits.State])
  }

  // ----------------------------------------------------------------------- //

  def markForSaving() = markChunkDirty = true

  override def installedComponents = servers.flatMap {
    case Some(server) => server.inventory.components collect {
      case Some(component) => component
    }
    case _ => Iterable.empty
  }

  def hasAbstractBusCard = servers exists {
    case Some(server) => server.machine.isRunning && server.inventory.items.exists {
      case Some(stack) => DriverAbstractBusCard.worksWith(stack, server.getClass)
      case _ => false
    }
    case _ => false
  }

  def hasRedstoneCard = servers exists {
    case Some(server) => server.machine.isRunning && server.inventory.items.exists {
      case Some(stack) => DriverRedstoneCard.worksWith(stack, server.getClass)
      case _ => false
    }
    case _ => false
  }

  def reconnectServer(number: Int, server: component.Server) {
    val serverNode = server.machine.node
    for (side <- ForgeDirection.VALID_DIRECTIONS if side != facing) {
      val node = sidedNode(side)
      if (node != null) {
        if (sides(number).contains(toLocal(side))) {
          node.connect(serverNode)
        }
        else {
          node.disconnect(serverNode)
        }
      }
    }
  }

  // ----------------------------------------------------------------------- //

  override protected def distribute() = {
    def node(side: Int) = sides(side) match {
      case None | Some(ForgeDirection.UNKNOWN) => servers(side).fold(null: Connector)(_.machine.node.asInstanceOf[Connector])
      case _ => null
    }
    val nodes = (0 to 3).map(node)
    def network(connector: Connector) = if (connector != null && connector.network != null) connector.network else this
    val (sumBuffer, sumSize) = super.distribute()
    var sumBufferServers, sumSizeServers = 0.0
    network(nodes(0)).synchronized {
      network(nodes(1)).synchronized {
        network(nodes(2)).synchronized {
          network(nodes(3)).synchronized {
            for (node <- nodes if node != null) {
              sumBufferServers += node.globalBuffer
              sumSizeServers += node.globalBufferSize
            }
            if (sumSize + sumSizeServers > 0) {
              val ratio = (sumBuffer + sumBufferServers) / (sumSize + sumSizeServers)
              for (node <- nodes if node != null) {
                node.changeBuffer(node.globalBufferSize * ratio - node.globalBuffer)
              }
            }
          }
        }
      }
    }
    (sumBuffer + sumBufferServers, sumSize + sumSizeServers)
  }

  // ----------------------------------------------------------------------- //

  override protected def relayPacket(sourceSide: Option[ForgeDirection], packet: Packet) {
    if (internalSwitch) {
      for (slot <- servers.indices) {
        val side = sides(slot).map(toGlobal)
        if (side != sourceSide) {
          servers(slot) match {
            case Some(server) => server.machine.node.sendToNeighbors("network.message", packet)
            case _ =>
          }
        }
      }
    }
    else super.relayPacket(sourceSide, packet)
  }

  override protected def onPlugMessage(plug: Plug, message: Message) {
    // This check is a little hacky. Basically what we test here is whether
    // the message was relayed internally, because only internally relayed
    // network messages originate from the actual server nodes themselves.
    // The otherwise come from the network card.
    if (message.name != "network.message" || !(servers collect {
      case Some(server) => server.machine.node
    }).contains(message.source)) super.onPlugMessage(plug, message)
  }

  // ----------------------------------------------------------------------- //

  override def getSizeInventory = 4

  override def getInventoryStackLimit = 1

  override def isItemValidForSlot(i: Int, stack: ItemStack) = {
    val descriptor = api.Items.get(stack)
    descriptor == api.Items.get(Constants.ItemName.ServerTier1) ||
      descriptor == api.Items.get(Constants.ItemName.ServerTier2) ||
      descriptor == api.Items.get(Constants.ItemName.ServerTier3) ||
      descriptor == api.Items.get(Constants.ItemName.ServerCreative)
  }

  // ----------------------------------------------------------------------- //

  override def onAnalyze(player: EntityPlayer, side: Int, hitX: Float, hitY: Float, hitZ: Float) = {
    slotAt(ForgeDirection.getOrientation(side), hitX, hitY, hitZ) match {
      case Some(slot) => servers(slot) match {
        case Some(server) => Array(server.machine.node)
        case _ => null
      }
      case _ => Array(sidedNode(ForgeDirection.getOrientation(side)))
    }
  }

  def slotAt(side: ForgeDirection, hitX: Float, hitY: Float, hitZ: Float) = {
    if (side == facing) {
      val l = 2 / 16.0
      val h = 14 / 16.0
      val slot = (((1 - hitY) - l) / (h - l) * 4).toInt
      Some(math.max(0, math.min(servers.length - 1, slot)))
    }
    else None
  }

  // ----------------------------------------------------------------------- //

  override def canUpdate = isServer

  override def updateEntity() {
    super.updateEntity()
    if (isServer && isConnected) {
      val shouldUpdatePower = world.getTotalWorldTime % Settings.get.tickFrequency == 0
      if (shouldUpdatePower && range > 0 && !Settings.get.ignorePower) {
        val countRunning = servers.count {
          case Some(server) => server.machine.isRunning
          case _ => false
        }
        if (countRunning > 0) {
          var cost = -(countRunning * range * Settings.get.wirelessCostPerRange * Settings.get.tickFrequency)
          for (side <- ForgeDirection.VALID_DIRECTIONS if cost < 0) {
            sidedNode(side) match {
              case connector: Connector => cost = connector.changeBuffer(cost)
              case _ =>
            }
          }
        }
      }

      servers collect {
        case Some(server) =>
          if (shouldUpdatePower && server.tier == Tier.Four) {
            server.machine.node.asInstanceOf[Connector].changeBuffer(Double.PositiveInfinity)
          }
          server.machine.update()
      }

      if (markChunkDirty) {
        markChunkDirty = false
        world.markTileEntityChunkModified(x, y, z, this)
      }

      for (i <- servers.indices) {
        val isRunning = servers(i).fold(false)(_.machine.isRunning)
        val errored = servers(i).fold(false)(_.machine.lastError != null)
        if (_isRunning(i) != isRunning || _hasErrored(i) != errored) {
          _isRunning(i) = isRunning
          _hasErrored(i) = errored
          markDirty()
          ServerPacketSender.sendServerState(this, i)
          world.notifyBlocksOfNeighborChange(x, y, z, block)
        }
      }
      isOutputEnabled = hasRedstoneCard
      isAbstractBusAvailable = hasAbstractBusCard

      servers collect {
        case Some(server) =>
          server.inventory.updateComponents()
          terminals(server.slot).buffer.update()
      }
    }
  }

  // ----------------------------------------------------------------------- //

  override protected def initialize() {
    super.initialize()
    if (isClient) {
      ServerRack.list += this -> Unit
    }
  }

  override def dispose() {
    super.dispose()
    if (isClient) {
      ServerRack.list -= this
    }
    else {
      servers collect {
        case Some(server) => server.machine.stop()
      }
    }
  }

  override def readFromNBTForServer(nbt: NBTTagCompound) {
    super.readFromNBTForServer(nbt)
    for (slot <- 0 until getSizeInventory) {
      if (getStackInSlot(slot) != null) {
        val server = new component.Server(this, slot)
        servers(slot) = Option(server)
      }
    }
    nbt.getTagList(Settings.namespace + "servers", NBT.TAG_COMPOUND).toArray[NBTTagCompound].
      zipWithIndex.foreach {
      case (tag, index) if index < servers.length =>
        servers(index) match {
          case Some(server) =>
            try server.load(tag) catch {
              case t: Throwable => OpenComputers.log.warn("Failed restoring server state. Please report this!", t)
            }
          case _ =>
        }
      case _ =>
    }
    val sidesNbt = nbt.getByteArray(Settings.namespace + "sides").map {
      case side if side >= 0 => Option(ForgeDirection.getOrientation(side))
      case _ => None
    }
    Array.copy(sidesNbt, 0, sides, 0, math.min(sidesNbt.length, sides.length))
    nbt.getTagList(Settings.namespace + "terminals", NBT.TAG_COMPOUND).toArray[NBTTagCompound].
      zipWithIndex.foreach {
      case (tag, index) if index < terminals.length =>
        try terminals(index).load(tag) catch {
          case t: Throwable => OpenComputers.log.warn("Failed restoring terminal state. Please report this!", t)
        }
      case _ =>
    }
    range = nbt.getInteger(Settings.namespace + "range")
    internalSwitch = nbt.getBoolean(Settings.namespace + "internalSwitch")

    // Kickstart initialization to avoid values getting overwritten by
    // readFromNBTForClient if that packet is handled after a manual
    // initialization / state change packet.
    for (i <- servers.indices) {
      val isRunning = servers(i).fold(false)(_.machine.isRunning)
      _isRunning(i) = isRunning
    }
    _isOutputEnabled = hasRedstoneCard
    _isAbstractBusAvailable = hasAbstractBusCard
  }

  // Side check for Waila (and other mods that may call this client side).
  override def writeToNBTForServer(nbt: NBTTagCompound) = if (isServer) {
    if (!Waila.isSavingForTooltip) {
      nbt.setNewTagList(Settings.namespace + "servers", servers map {
        case Some(server) =>
          val serverNbt = new NBTTagCompound()
          try server.save(serverNbt) catch {
            case t: Throwable => OpenComputers.log.warn("Failed saving server state. Please report this!", t)
          }
          serverNbt
        case _ => new NBTTagCompound()
      })
    }
    super.writeToNBTForServer(nbt)
    nbt.setByteArray(Settings.namespace + "sides", sides.map {
      case Some(side) => side.ordinal.toByte
      case _ => -1: Byte
    })
    nbt.setNewTagList(Settings.namespace + "terminals", terminals.map(t => {
      val terminalNbt = new NBTTagCompound()
      try t.save(terminalNbt) catch {
        case t: Throwable => OpenComputers.log.warn("Failed saving terminal state. Please report this!", t)
      }
      terminalNbt
    }))
    nbt.setInteger(Settings.namespace + "range", range)
    nbt.setBoolean(Settings.namespace + "internalSwitch", internalSwitch)
  }

  @SideOnly(Side.CLIENT)
  override def readFromNBTForClient(nbt: NBTTagCompound) {
    super.readFromNBTForClient(nbt)
    val isRunningNbt = nbt.getBooleanArray("isServerRunning")
    Array.copy(isRunningNbt, 0, _isRunning, 0, math.min(isRunningNbt.length, _isRunning.length))
    val hasErroredNbt = nbt.getBooleanArray("hasServerErrored")
    Array.copy(hasErroredNbt, 0, _hasErrored, 0, math.min(hasErroredNbt.length, _hasErrored.length))
    val isPresentNbt = nbt.getTagList("isPresent", NBT.TAG_STRING).map((tag: NBTTagString) => {
      val value = tag.func_150285_a_()
      if (Strings.isNullOrEmpty(value)) None else Some(value)
    }).toArray
    Array.copy(isPresentNbt, 0, isPresent, 0, math.min(isPresentNbt.length, isPresent.length))
    val sidesNbt = nbt.getByteArray("sides").map {
      case side if side >= 0 => Option(ForgeDirection.getOrientation(side))
      case _ => None
    }
    Array.copy(sidesNbt, 0, sides, 0, math.min(sidesNbt.length, sides.length))
    nbt.getTagList("terminals", NBT.TAG_COMPOUND).toArray[NBTTagCompound].
      zipWithIndex.foreach {
      case (tag, index) if index < terminals.length => terminals(index).readFromNBTForClient(tag)
      case _ =>
    }
    range = nbt.getInteger("range")
    if (anyRunning) Sound.startLoop(this, "computer_running", 1.5f, 1000 + world.rand.nextInt(2000))
  }

  override def writeToNBTForClient(nbt: NBTTagCompound) {
    super.writeToNBTForClient(nbt)
    nbt.setBooleanArray("isServerRunning", _isRunning)
    nbt.setBooleanArray("hasServerErrored", _hasErrored)
    nbt.setNewTagList("isPresent", servers.map(value => new NBTTagString(value match {
      case Some(server) if server.machine != null && server.machine.node != null && server.machine.node.address != null => server.machine.node.address
      case _ => ""
    })))
    nbt.setByteArray("sides", sides.map {
      case Some(side) => side.ordinal.toByte
      case _ => -1: Byte
    })
    nbt.setNewTagList("terminals", terminals.map(t => {
      val terminalNbt = new NBTTagCompound()
      t.writeToNBTForClient(terminalNbt)
      terminalNbt
    }))
    nbt.setInteger("range", range)
  }

  // ----------------------------------------------------------------------- //

  override protected def onPlugConnect(plug: Plug, node: Node) {
    if (node == plug.node) {
      for (number <- servers.indices) {
        val serverSide = sides(number).map(toGlobal)
        servers(number) match {
          case Some(server) =>
            if (serverSide == Option(plug.side)) plug.node.connect(server.machine.node)
            else api.Network.joinNewNetwork(server.machine.node)
            terminals(number).connect(server.machine.node)
          case _ =>
        }
      }
    }
  }

  override protected def createNode(plug: Plug) = api.Network.newNode(plug, Visibility.Network).
    withConnector(Settings.get.bufferDistributor).
    create()

  override protected def onItemAdded(slot: Int, stack: ItemStack) {
    super.onItemAdded(slot, stack)
    if (isServer) {
      val server = new component.Server(this, slot)
      servers(slot) = Some(server)
      reconnectServer(slot, server)
      Network.joinNewNetwork(server.machine.node)
      terminals(slot).connect(server.machine.node)
    }
  }

  override protected def onItemRemoved(slot: Int, stack: ItemStack) {
    super.onItemRemoved(slot, stack)
    if (isServer) {
      servers(slot) match {
        case Some(server) =>
          server.machine.node.remove()
          server.inventory.containerOverride = stack
          server.inventory.save(new NBTTagCompound()) // Only flush components.
          server.inventory.markDirty()
        case _ =>
      }
      servers(slot) = None
      terminals(slot).keys.clear()
    }
  }

  override def markDirty() {
    super.markDirty()
    if (isServer) {
      isOutputEnabled = hasRedstoneCard
      isAbstractBusAvailable = hasAbstractBusCard
      ServerPacketSender.sendServerPresence(this)
    }
    else {
      world.markBlockForUpdate(x, y, z)
    }
  }

  override protected def onRotationChanged() {
    super.onRotationChanged()
    checkRedstoneInputChanged()
  }

  override protected def onRedstoneInputChanged(side: ForgeDirection, oldMaxValue: Int, newMaxValue: Int) {
    super.onRedstoneInputChanged(side, oldMaxValue, newMaxValue)
    servers collect {
      case Some(server) => server.machine.node.sendToNeighbors("redstone.changed", toLocal(side), Int.box(oldMaxValue), Int.box(newMaxValue))
    }
  }

  override def rotate(axis: ForgeDirection) = false
}

object ServerRack {
  val list = mutable.WeakHashMap.empty[ServerRack, Unit]

  @SubscribeEvent
  def onWorldUnload(e: WorldEvent.Unload) {
    if (e.world.isRemote) {
      list.clear()
    }
  }
}

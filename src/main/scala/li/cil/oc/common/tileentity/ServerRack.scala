package li.cil.oc.common.tileentity

import java.util.logging.Level

import cpw.mods.fml.common.Optional
import cpw.mods.fml.common.Optional.Method
import cpw.mods.fml.relauncher.{Side, SideOnly}
import li.cil.oc._
import li.cil.oc.api.Network
import li.cil.oc.api.network._
import li.cil.oc.client.Sound
import li.cil.oc.common.Tier
import li.cil.oc.server.{component, driver, PacketSender => ServerPacketSender}
import li.cil.oc.util.ExtendedNBT._
import li.cil.oc.util.mods.{Mods, Waila}
import net.minecraft.entity.player.EntityPlayer
import net.minecraft.item.ItemStack
import net.minecraft.nbt.{NBTTagCompound, NBTTagString}
import net.minecraftforge.common.ForgeDirection
import net.minecraftforge.event.ForgeSubscribe
import net.minecraftforge.event.world.WorldEvent
import stargatetech2.api.bus.IBusDevice

import scala.collection.mutable

// See AbstractBusAware as to why we have to define the IBusDevice here.
@Optional.Interface(iface = "stargatetech2.api.bus.IBusDevice", modid = Mods.IDs.StargateTech2)
class ServerRack extends traits.PowerAcceptor with traits.Hub with traits.PowerBalancer with traits.Inventory with traits.Rotatable with traits.BundledRedstoneAware with traits.AbstractBusAware with Analyzable with IBusDevice {
  val servers = Array.fill(getSizeInventory)(None: Option[component.Server])

  val sides = Seq(ForgeDirection.UP, ForgeDirection.EAST, ForgeDirection.WEST, ForgeDirection.DOWN).
    padTo(servers.length, ForgeDirection.UNKNOWN).toArray

  val terminals = (0 until servers.length).map(new common.component.Terminal(this, _)).toArray

  var range = 16

  // For client side, where we don't create the component.
  private val _isRunning = new Array[Boolean](getSizeInventory)

  private var hasChanged = false

  var internalSwitch = false

  // For client side rendering.
  var isPresent = Array.fill[Option[String]](getSizeInventory)(None)

  @SideOnly(Side.CLIENT)
  override protected def hasConnector(side: ForgeDirection) = side != facing

  override protected def connector(side: ForgeDirection) = Option(if (side != facing) sidedNode(side).asInstanceOf[Connector] else null)

  override def getWorld = world

  // ----------------------------------------------------------------------- //

  override def canConnect(side: ForgeDirection) = side != facing

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
  def setRunning(number: Int, value: Boolean) = {
    _isRunning(number) = value
    world.markBlockForRenderUpdate(x, y, z)
    if (anyRunning) Sound.startLoop(this, "computer_running", 1.5f, 50 + world.rand.nextInt(50))
    else Sound.stopLoop(this)
    this
  }

  def anyRunning = (0 until servers.length).exists(isRunning)

  // ----------------------------------------------------------------------- //

  def markAsChanged() = hasChanged = true

  override def installedComponents = servers.flatMap {
    case Some(server) => server.inventory.components collect {
      case Some(component) => component
    }
    case _ => Iterable.empty
  }

  def hasAbstractBusCard = servers exists {
    case Some(server) => server.machine.isRunning && server.inventory.items.exists {
      case Some(stack) => driver.item.AbstractBusCard.worksWith(stack)
      case _ => false
    }
    case _ => false
  }

  def hasRedstoneCard = servers exists {
    case Some(server) => server.machine.isRunning && server.inventory.items.exists {
      case Some(stack) => driver.item.RedstoneCard.worksWith(stack)
      case _ => false
    }
    case _ => false
  }

  def reconnectServer(number: Int, server: component.Server) {
    val serverSide = sides(number)
    val serverNode = server.machine.node
    for (side <- ForgeDirection.VALID_DIRECTIONS) {
      if (toGlobal(serverSide) == side) sidedNode(side).connect(serverNode)
      else sidedNode(side).disconnect(serverNode)
    }
  }

  // ----------------------------------------------------------------------- //

  override protected def distribute() = {
    def node(side: Int) = if (sides(side) == ForgeDirection.UNKNOWN) servers(side).fold(null: Connector)(_.node.asInstanceOf[Connector]) else null
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

  override protected def relayPacket(sourceSide: ForgeDirection, packet: Packet) {
    if (internalSwitch) {
      for (slot <- 0 until servers.length) {
        val side = toGlobal(sides(slot))
        if (side != sourceSide) {
          servers(slot) match {
            case Some(server) => server.node.sendToNeighbors("network.message", packet)
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
      case Some(server) => server.node
    }).contains(message.source)) super.onPlugMessage(plug, message)
  }

  // ----------------------------------------------------------------------- //

  override def getSizeInventory = 4

  override def getInventoryStackLimit = 1

  override def isItemValidForSlot(i: Int, stack: ItemStack) = {
    val descriptor = api.Items.get(stack)
    descriptor == api.Items.get("server1") ||
      descriptor == api.Items.get("server2") ||
      descriptor == api.Items.get("server3") ||
      descriptor == api.Items.get("serverCreative")
  }

  // ----------------------------------------------------------------------- //

  override def onAnalyze(player: EntityPlayer, side: Int, hitX: Float, hitY: Float, hitZ: Float) = {
    if (side == facing.ordinal) {
      val l = 2 / 16.0
      val h = 14 / 16.0
      val slot = (((1 - hitY) - l) / (h - l) * 4).toInt
      if (slot >= 0 && slot <= 3 && servers(slot).isDefined) {
        val computer = servers(slot).get.machine
        computer.lastError match {
          case value if value != null =>
            player.sendChatToPlayer(Localization.Analyzer.LastError(value))
          case _ =>
        }
        player.sendChatToPlayer(Localization.Analyzer.Components(computer.componentCount, servers(slot).get.maxComponents))
        val list = computer.users
        if (list.size > 0) {
          player.sendChatToPlayer(Localization.Analyzer.Users(list))
        }
        Array(computer.node)
      }
      else null
    }
    else Array(sidedNode(ForgeDirection.getOrientation(side)))
  }

  // ----------------------------------------------------------------------- //

  override def canUpdate = isServer

  override def updateEntity() {
    super.updateEntity()
    if (isServer && isConnected) {
      if (range > 0 && !Settings.get.ignorePower && anyRunning) {
        val running = servers.count {
          case Some(server) => server.machine.isRunning
          case _ => false
        }
        var cost = -(running * range * Settings.get.wirelessCostPerRange)
        for (side <- ForgeDirection.VALID_DIRECTIONS if cost < 0) {
          sidedNode(side) match {
            case connector: Connector => cost = connector.changeBuffer(cost)
            case _ =>
          }
        }
      }

      servers collect {
        case Some(server) =>
          if (server.tier == Tier.Four && world.getTotalWorldTime % Settings.get.tickFrequency == 0) {
            server.node.asInstanceOf[Connector].changeBuffer(Double.PositiveInfinity)
          }
          server.machine.update()
      }

      if (hasChanged) {
        hasChanged = false
        world.markTileEntityChunkModified(x, y, z, this)
      }

      for (i <- 0 until servers.length) {
        val isRunning = servers(i).fold(false)(_.machine.isRunning)
        if (_isRunning(i) != isRunning) {
          _isRunning(i) = isRunning
          ServerPacketSender.sendServerState(this, i)
        }
      }
      isOutputEnabled = hasRedstoneCard
      isAbstractBusAvailable = hasAbstractBusCard

      servers collect {
        case Some(server) => server.inventory.updateComponents()
      }
    }
  }

  // ----------------------------------------------------------------------- //

  override protected def initialize() {
    super.initialize()
    ServerRack.list += this -> Unit
  }

  override protected def dispose() {
    super.dispose()
    ServerRack.list -= this
  }

  override def readFromNBT(nbt: NBTTagCompound) {
    super.readFromNBT(nbt)
    for (slot <- 0 until getSizeInventory) {
      if (getStackInSlot(slot) != null) {
        servers(slot) = Some(new component.Server(this, slot))
      }
    }
    for ((serverNbt, slot) <- nbt.getTagList(Settings.namespace + "servers").iterator[NBTTagCompound].zipWithIndex if slot < servers.length) {
      servers(slot) match {
        case Some(server) => try server.load(serverNbt) catch {
          case t: Throwable => OpenComputers.log.log(Level.WARNING, "Failed restoring server state. Please report this!", t)
        }
        case _ =>
      }
    }
    val sidesNbt = nbt.getByteArray(Settings.namespace + "sides").byteArray.map(ForgeDirection.getOrientation(_))
    Array.copy(sidesNbt, 0, sides, 0, math.min(sidesNbt.length, sides.length))
    val terminalsNbt = nbt.getTagList(Settings.namespace + "terminals").iterator[NBTTagCompound].toArray
    for (i <- 0 until math.min(terminals.length, terminalsNbt.length)) {
      try terminals(i).load(terminalsNbt(i)) catch {
        case t: Throwable => OpenComputers.log.log(Level.WARNING, "Failed restoring terminal state. Please report this!", t)
      }
    }
    range = nbt.getInteger(Settings.namespace + "range")
    internalSwitch = nbt.getBoolean(Settings.namespace + "internalSwitch")
  }

  // Side check for Waila (and other mods that may call this client side).
  override def writeToNBT(nbt: NBTTagCompound) = if (isServer) {
    if (!Mods.Waila.isAvailable || !Waila.isSavingForTooltip) {
      nbt.setNewTagList(Settings.namespace + "servers", servers map {
        case Some(server) =>
          val serverNbt = new NBTTagCompound()
          try server.save(serverNbt) catch {
            case t: Throwable => OpenComputers.log.log(Level.WARNING, "Failed saving server state. Please report this!", t)
          }
          serverNbt
        case _ => new NBTTagCompound()
      })
    }
    super.writeToNBT(nbt)
    nbt.setByteArray(Settings.namespace + "sides", sides.map(_.ordinal.toByte))
    nbt.setNewTagList(Settings.namespace + "terminals", terminals.map(t => {
      val terminalNbt = new NBTTagCompound()
      try t.save(terminalNbt) catch {
        case t: Throwable => OpenComputers.log.log(Level.WARNING, "Failed saving terminal state. Please report this!", t)
      }
      terminalNbt
    }))
    nbt.setInteger(Settings.namespace + "range", range)
    nbt.setBoolean(Settings.namespace + "internalSwitch", internalSwitch)
  }

  @SideOnly(Side.CLIENT)
  override def readFromNBTForClient(nbt: NBTTagCompound) {
    super.readFromNBTForClient(nbt)
    val isRunningNbt = nbt.getByteArray("isServerRunning").byteArray.map(_ == 1)
    Array.copy(isRunningNbt, 0, _isRunning, 0, math.min(isRunningNbt.length, _isRunning.length))
    val isPresentNbt = nbt.getTagList("isPresent").iterator[NBTTagString].map(value => if (value.data == "") None else Some(value.data)).toArray
    Array.copy(isPresentNbt, 0, isPresent, 0, math.min(isPresentNbt.length, isPresent.length))
    val sidesNbt = nbt.getByteArray("sides").byteArray.map(ForgeDirection.getOrientation(_))
    Array.copy(sidesNbt, 0, sides, 0, math.min(sidesNbt.length, sides.length))
    val terminalsNbt = nbt.getTagList("terminals").iterator[NBTTagCompound].toArray
    for (i <- 0 until math.min(terminals.length, terminalsNbt.length)) {
      terminals(i).readFromNBTForClient(terminalsNbt(i))
    }
    range = nbt.getInteger("range")
    if (anyRunning) Sound.startLoop(this, "computer_running", 1.5f, 1000 + world.rand.nextInt(2000))
  }

  override def writeToNBTForClient(nbt: NBTTagCompound) {
    super.writeToNBTForClient(nbt)
    nbt.setByteArray("isServerRunning", _isRunning.map(value => (if (value) 1 else 0).toByte))
    nbt.setNewTagList("isPresent", servers.map(value => new NBTTagString(null, value.fold("")(_.machine.node.address))))
    nbt.setByteArray("sides", sides.map(_.ordinal.toByte))
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
      for (number <- 0 until servers.length) {
        val serverSide = sides(number)
        servers(number) match {
          case Some(server) =>
            if (toGlobal(serverSide) == plug.side) plug.node.connect(server.machine.node)
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
          server.inventory.onInventoryChanged()
        case _ =>
      }
      servers(slot) = None
      terminals(slot).keys.clear()
    }
  }

  override def onInventoryChanged() {
    super.onInventoryChanged()
    if (isServer) {
      isOutputEnabled = hasRedstoneCard
      isAbstractBusAvailable = hasAbstractBusCard
      ServerPacketSender.sendServerPresence(this)
    }
    else {
      world.markBlockForRenderUpdate(x, y, z)
    }
  }

  override protected def onRotationChanged() {
    super.onRotationChanged()
    checkRedstoneInputChanged()
  }

  override protected def onRedstoneInputChanged(side: ForgeDirection) {
    super.onRedstoneInputChanged(side)
    servers collect {
      case Some(server) => server.machine.signal("redstone_changed", server.machine.node.address, Int.box(toLocal(side).ordinal()))
    }
  }

  override def rotate(axis: ForgeDirection) = false
}

object ServerRack {
  val list = mutable.WeakHashMap.empty[ServerRack, Unit]

  @ForgeSubscribe
  def onWorldUnload(e: WorldEvent.Unload) {
    if (e.world.isRemote) {
      list.clear()
    }
  }
}
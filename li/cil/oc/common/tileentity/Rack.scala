package li.cil.oc.common.tileentity

import cpw.mods.fml.common.Optional
import cpw.mods.fml.relauncher.{Side, SideOnly}
import li.cil.oc.api.network.{Connector, Visibility, Node, Analyzable}
import li.cil.oc.common
import li.cil.oc.server.{PacketSender => ServerPacketSender, driver, component}
import li.cil.oc.util.ExtendedNBT._
import li.cil.oc.{api, Items, Settings}
import net.minecraft.entity.player.EntityPlayer
import net.minecraft.item.ItemStack
import net.minecraft.nbt.{NBTTagString, NBTTagCompound}
import net.minecraftforge.common.ForgeDirection
import stargatetech2.api.bus.IBusDevice

// See AbstractBusAware as to why we have to define the IBusDevice here.
@Optional.Interface(iface = "stargatetech2.api.bus.IBusDevice", modid = "StargateTech2")
class Rack extends Hub with PowerBalancer with Inventory with Rotatable with BundledRedstoneAware with AbstractBusAware with IBusDevice with Analyzable {
  val servers = Array.fill(getSizeInventory)(None: Option[component.Server])

  val sides = Array.fill(servers.length)(ForgeDirection.UNKNOWN)

  val terminals = (0 until servers.length).map(new common.component.Terminal(this, _)).toArray

  var range = 16

  // For client side, where we don't create the component.
  private val _isRunning = new Array[Boolean](getSizeInventory)

  private var hasChanged = false

  // For client side rendering.
  var isPresent = Array.fill[Option[String]](getSizeInventory)(None)

  // ----------------------------------------------------------------------- //

  override def canConnect(side: ForgeDirection) = side != facing

  // ----------------------------------------------------------------------- //

  def isRunning(number: Int) =
    if (isServer) servers(number).fold(false)(_.machine.isRunning)
    else _isRunning(number)

  @SideOnly(Side.CLIENT)
  def setRunning(number: Int, value: Boolean) = {
    _isRunning(number) = value
    world.markBlockForRenderUpdate(x, y, z)
    this
  }

  def anyRunning = (0 until servers.length).exists(isRunning)

  // ----------------------------------------------------------------------- //

  def markAsChanged() = hasChanged = true

  def installedComponents = servers.flatMap {
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
      if (toGlobal(serverSide) == side || serverSide == ForgeDirection.UNKNOWN) {
        sidedNode(side).connect(serverNode)
      }
      else {
        sidedNode(side).disconnect(serverNode)
      }
    }
  }

  // ----------------------------------------------------------------------- //

  def getSizeInventory = 4

  def getInvName = Settings.namespace + "container.Rack"

  def getInventoryStackLimit = 1

  def isItemValidForSlot(i: Int, stack: ItemStack) = Items.server.createItemStack().isItemEqual(stack)

  // ----------------------------------------------------------------------- //

  def onAnalyze(stats: NBTTagCompound, player: EntityPlayer, side: Int, hitX: Float, hitY: Float, hitZ: Float) = {
    if (side == facing.ordinal) {
      val l = 2 / 16.0
      val h = 14 / 16.0
      val slot = ((hitY - l) / (h - l) * 4).toInt
      if (slot >= 0 && slot <= 3 && servers(slot).isDefined) {
        servers(slot).get.machine.node
      }
      else null
    }
    else sidedNode(ForgeDirection.getOrientation(side))
  }

  // ----------------------------------------------------------------------- //

  override def updateEntity() {
    if (isServer) {
      if (addedToNetwork) {
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
          case Some(server) => server.machine.update()
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

        updateRedstoneInput()
        servers collect {
          case Some(server) => server.inventory.updateComponents()
        }
      }
    }
    super.updateEntity()
  }

  // ----------------------------------------------------------------------- //

  override def readFromNBT(nbt: NBTTagCompound) {
    super.readFromNBT(nbt)
    for (slot <- 0 until getSizeInventory) {
      if (getStackInSlot(slot) != null) {
        val server = new component.Server(this, slot)
        servers(slot) = Some(server)
      }
    }
    for ((serverNbt, slot) <- nbt.getTagList(Settings.namespace + "servers").iterator[NBTTagCompound].zipWithIndex if slot < servers.length) {
      servers(slot) match {
        case Some(server) => server.load(serverNbt)
        case _ =>
      }
    }
    val sidesNbt = nbt.getByteArray(Settings.namespace + "sides").byteArray.map(ForgeDirection.getOrientation(_))
    Array.copy(sidesNbt, 0, sides, 0, math.min(sidesNbt.length, sides.length))
    val terminalsNbt = nbt.getTagList(Settings.namespace + "terminals").iterator[NBTTagCompound].toArray
    for (i <- 0 until math.min(terminals.length, terminalsNbt.length)) {
      terminals(i).load(terminalsNbt(i))
    }
    range = nbt.getInteger(Settings.namespace + "range")
  }

  override def writeToNBT(nbt: NBTTagCompound) {
    if (!new Exception().getStackTrace.exists(_.getClassName.startsWith("mcp.mobius.waila"))) {
      nbt.setNewTagList(Settings.namespace + "servers", servers map {
        case Some(server) =>
          val serverNbt = new NBTTagCompound()
          server.save(serverNbt)
          serverNbt
        case _ => new NBTTagCompound()
      })
    }
    super.writeToNBT(nbt)
    nbt.setByteArray(Settings.namespace + "sides", sides.map(_.ordinal.toByte))
    nbt.setNewTagList(Settings.namespace + "terminals", terminals.map(t => {
      val terminalNbt = new NBTTagCompound()
      t.save(terminalNbt)
      terminalNbt
    }))
    nbt.setInteger(Settings.namespace + "range", range)
  }

  @SideOnly(Side.CLIENT)
  override def readFromNBTForClient(nbt: NBTTagCompound) {
    super.readFromNBTForClient(nbt)
    val isRunningNbt = nbt.getByteArray("isRunning").byteArray.map(_ == 1)
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
  }

  override def writeToNBTForClient(nbt: NBTTagCompound) {
    super.writeToNBTForClient(nbt)
    nbt.setByteArray("isRunning", _isRunning.map(value => (if (value) 1 else 0).toByte))
    nbt.setNewTagList("isPresent", servers.map(value => new NBTTagString(null, value.fold("")(_.machine.address))))
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
          case Some(server) if toGlobal(serverSide) == plug.side || serverSide == ForgeDirection.UNKNOWN =>
            plug.node.connect(server.machine.node)
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
      case Some(server) => server.machine.signal("redstone_changed", server.machine.address, Int.box(toLocal(side).ordinal()))
    }
  }

  override def rotate(axis: ForgeDirection) = false
}

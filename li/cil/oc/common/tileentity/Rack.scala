package li.cil.oc.common.tileentity

import cpw.mods.fml.common.Optional
import cpw.mods.fml.relauncher.{Side, SideOnly}
import li.cil.oc.api.network.{Node, Analyzable}
import li.cil.oc.server.{PacketSender => ServerPacketSender, driver, component}
import li.cil.oc.util.ExtendedNBT._
import li.cil.oc.{Items, Settings}
import net.minecraft.entity.player.EntityPlayer
import net.minecraft.item.ItemStack
import net.minecraft.nbt.NBTTagCompound
import net.minecraftforge.common.ForgeDirection
import stargatetech2.api.bus.IBusDevice

// See AbstractBusAware as to why we have to define the IBusDevice here.
@Optional.Interface(iface = "stargatetech2.api.bus.IBusDevice", modid = "StargateTech2")
class Rack extends Hub with Inventory with Rotatable with BundledRedstoneAware with AbstractBusAware with IBusDevice with Analyzable {
  val servers = Array.fill(getSizeInventory)(None: Option[component.Server])

  val sides = Array.fill(servers.length)(ForgeDirection.UNKNOWN)

  // For client side, where we don't create the component.
  private val _isRunning = new Array[Boolean](getSizeInventory)

  private var hasChanged = false

  // For client side rendering.
  var isPresent = new Array[Boolean](getSizeInventory)

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
    servers.collect {
      case Some(server) => server.machine.lastError match {
        case Some(value) =>
          // TODO check if already in, expand value string with additional messages
          stats.setString(Settings.namespace + "gui.Analyzer.LastError", value)
        case _ =>
      }
    }
    null
  }

  // ----------------------------------------------------------------------- //

  override def updateEntity() {
    if (isServer) {
      if (addedToNetwork) {
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
    val sidesNbt = nbt.getByteArray(Settings.namespace + "sides").byteArray.map(ForgeDirection.getOrientation(_))
    Array.copy(sidesNbt, 0, sides, 0, math.min(sidesNbt.length, sides.length))
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
  }

  override def writeToNBT(nbt: NBTTagCompound) {
    nbt.setByteArray(Settings.namespace + "sides", sides.map(_.ordinal.toByte))
    nbt.setNewTagList(Settings.namespace + "servers", servers map {
      case Some(server) =>
        val serverNbt = new NBTTagCompound()
        server.save(serverNbt)
        serverNbt
      case _ => new NBTTagCompound()
    })
    super.writeToNBT(nbt)
  }

  @SideOnly(Side.CLIENT)
  override def readFromNBTForClient(nbt: NBTTagCompound) {
    super.readFromNBTForClient(nbt)
    val isRunningNbt = nbt.getByteArray("isRunning").byteArray.map(_ == 1)
    Array.copy(isRunningNbt, 0, _isRunning, 0, math.min(isRunningNbt.length, _isRunning.length))
    val isPresentNbt = nbt.getByteArray("isPresent").byteArray.map(_ == 1)
    Array.copy(isPresentNbt, 0, isPresent, 0, math.min(isPresentNbt.length, isPresent.length))
    val sidesNbt = nbt.getByteArray("sides").byteArray.map(ForgeDirection.getOrientation(_))
    Array.copy(sidesNbt, 0, sides, 0, math.min(sidesNbt.length, sides.length))
  }

  override def writeToNBTForClient(nbt: NBTTagCompound) {
    super.writeToNBTForClient(nbt)
    nbt.setByteArray("isRunning", _isRunning.map(value => (if (value) 1 else 0).toByte))
    nbt.setByteArray("isPresent", servers.map(value => (if (value.isDefined) 1 else 0).toByte))
    nbt.setByteArray("sides", sides.map(_.ordinal.toByte))
  }

  // ----------------------------------------------------------------------- //

  override protected def onPlugConnect(plug: Plug, node: Node) {
    if (node == plug.node) {
      for (number <- 0 until servers.length) {
        val serverSide = sides(number)
        servers(number) match {
          case Some(server) if toGlobal(serverSide) == plug.side || serverSide == ForgeDirection.UNKNOWN =>
            plug.node.connect(server.machine.node)
          case _ =>
        }
      }
    }
  }

  override protected def onItemAdded(slot: Int, stack: ItemStack) {
    super.onItemAdded(slot, stack)
    if (isServer) {
      val server = new component.Server(this, slot)
      servers(slot) = Some(server)
      reconnectServer(slot, server)
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

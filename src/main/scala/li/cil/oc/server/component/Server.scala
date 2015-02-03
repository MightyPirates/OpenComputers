package li.cil.oc.server.component

import li.cil.oc.Settings
import li.cil.oc.api
import li.cil.oc.api.Driver
import li.cil.oc.api.Machine
import li.cil.oc.api.driver.item.Memory
import li.cil.oc.api.driver.item.Processor
import li.cil.oc.api.internal
import li.cil.oc.api.machine.Architecture
import li.cil.oc.api.machine.MachineHost
import li.cil.oc.api.network.Environment
import li.cil.oc.api.network.Message
import li.cil.oc.api.network.Node
import li.cil.oc.api.network.Visibility
import li.cil.oc.common.Slot
import li.cil.oc.common.init.Items
import li.cil.oc.common.inventory.ComponentInventory
import li.cil.oc.common.inventory.ServerInventory
import li.cil.oc.common.item
import li.cil.oc.common.tileentity
import li.cil.oc.util.ExtendedNBT._
import net.minecraft.item.ItemStack
import net.minecraft.nbt.NBTTagCompound

class Server(val rack: tileentity.ServerRack, val slot: Int) extends Environment with MachineHost with internal.Server {
  val machine = Machine.create(this)

  val inventory = new NetworkedInventory()

  // Used to grab messages when not connected to any side in the server rack.
  val node = api.Network.newNode(this, Visibility.Network).create()

  machine.onHostChanged()

  def tier = Items.multi.subItem(rack.getStackInSlot(slot)) match {
    case Some(server: item.Server) => server.tier
    case _ => 0
  }

  // ----------------------------------------------------------------------- //

  override def cpuArchitecture: Class[_ <: Architecture] = {
    for (i <- 0 until inventory.getSizeInventory if inventory.isComponentSlot(i)) Option(inventory.getStackInSlot(i)) match {
      case Some(s) => Option(Driver.driverFor(s, rack.getClass)) match {
        case Some(driver: Processor) if driver.slot(s) == Slot.CPU => return driver.architecture(s)
        case _ =>
      }
      case _ =>
    }
    null
  }

  override def callBudget = inventory.items.foldLeft(0.0)((sum, item) => sum + (item match {
    case Some(stack) => Option(Driver.driverFor(stack, getClass)) match {
      case Some(driver: Processor) if driver.slot(stack) == Slot.CPU => Settings.get.callBudgets(driver.tier(stack))
      case _ => 0
    }
    case _ => 0
  }))

  override def installedMemory = inventory.items.foldLeft(0)((sum, item) => sum + (item match {
    case Some(stack) => Option(Driver.driverFor(stack, rack.getClass)) match {
      case Some(driver: Memory) => driver.amount(stack)
      case _ => 0
    }
    case _ => 0
  }))

  lazy val maxComponents = if (!hasCPU) 0
  else inventory.items.foldLeft(0)((sum, stack) => sum + (stack match {
    case Some(item) => Option(Driver.driverFor(item, rack.getClass)) match {
      case Some(driver: Processor) => driver.supportedComponents(item)
      case _ => 0
    }
    case _ => 0
  }))

  override def componentSlot(address: String) = inventory.components.indexWhere(_.exists(env => env.node != null && env.node.address == address))

  def hasCPU = inventory.items.exists {
    case Some(stack) => Option(Driver.driverFor(stack, rack.getClass)) match {
      case Some(driver) => driver.slot(stack) == Slot.CPU
      case _ => false
    }
    case _ => false
  }

  override def xPosition = rack.x + 0.5

  override def yPosition = rack.y + 0.5

  override def zPosition = rack.z + 0.5

  override def world = rack.world

  override def markChanged() = rack.markChanged()

  // ----------------------------------------------------------------------- //

  override def onConnect(node: Node) {}

  override def onDisconnect(node: Node) {}

  override def onMessage(message: Message) {
    // If we're internal mode and this server is not connected to any side, we
    // must manually propagate network messages to other servers in the rack.
    // Ensure the message originated in our local network, to avoid infinite
    // recursion if two unconnected servers are in one server rack.
    if (rack.internalSwitch && message.name == "network.message" &&
      rack.sides(this.slot) == None && // Only if we're in internal mode.
      message.source != machine.node && // In this case it was relayed from another internal machine.
      node.network.node(message.source.address) != null) {
      for (slot <- 0 until rack.servers.length) {
        rack.servers(slot) match {
          case Some(server) if server != this => server.machine.node.sendToNeighbors(message.name, message.data: _*)
          case _ =>
        }
      }
    }
  }

  override def onMachineConnect(node: Node) {
    if (node == machine.node) {
      node.connect(this.node)
    }
    inventory.onConnect(node)
  }

  override def onMachineDisconnect(node: Node) = inventory.onDisconnect(node)

  def load(nbt: NBTTagCompound) {
    machine.load(nbt.getCompoundTag("machine"))
  }

  def save(nbt: NBTTagCompound) {
    nbt.setNewCompoundTag("machine", machine.save)
    inventory.saveComponents()
    inventory.markDirty()
  }

  // Required due to abstract overrides in component inventory.
  class NetworkedInventory extends ServerInventory with ComponentInventory {
    override def onConnect(node: Node) {
      if (node == this.node) {
        connectComponents()
      }
    }

    override def onDisconnect(node: Node) {
      if (node == this.node) {
        disconnectComponents()
      }
    }

    override def tier = Server.this.tier

    var containerOverride: ItemStack = _

    override def container = if (containerOverride != null) containerOverride else rack.getStackInSlot(slot)

    override def node() = machine.node

    override def onMessage(message: Message) {}

    override def host = rack

    // Resolves conflict between ComponentInventory and ServerInventory.
    override def getInventoryStackLimit = 1
  }

}

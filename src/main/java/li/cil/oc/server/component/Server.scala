package li.cil.oc.server.component

import li.cil.oc.Items
import li.cil.oc.api.driver
import li.cil.oc.api.driver.Slot
import li.cil.oc.api.network.{Message, Node}
import li.cil.oc.common.inventory.ComponentInventory
import li.cil.oc.common.inventory.ServerInventory
import li.cil.oc.common.item
import li.cil.oc.common.tileentity
import li.cil.oc.server.component.machine.Machine
import li.cil.oc.server.driver.Registry
import li.cil.oc.util.ExtendedNBT._
import net.minecraft.item.ItemStack
import net.minecraft.nbt.NBTTagCompound

class Server(val rack: tileentity.Rack, val number: Int) extends Machine.Owner {
  val machine = new Machine(this)

  val inventory = new NetworkedInventory()

  def tier = Items.multi.subItem(rack.getStackInSlot(number)) match {
    case Some(server: item.Server) => server.tier
    case _ => 0
  }

  // ----------------------------------------------------------------------- //

  override def address = machine.node.address

  override def node = machine.node

  override def start() = machine.start()

  override def stop() = machine.stop()

  override def pause(seconds: Double) = machine.pause(seconds)

  override def isPaused = machine.isPaused

  override def isRunning = machine.isRunning

  override def signal(name: String, args: AnyRef*) = machine.signal(name, args: _*)

  override def canInteract(player: String) = machine.canInteract(player)

  // ----------------------------------------------------------------------- //

  override def installedMemory = inventory.items.foldLeft(0)((sum, stack) => sum + (stack match {
    case Some(item) => Registry.itemDriverFor(item) match {
      case Some(driver: driver.Memory) => driver.amount(item)
      case _ => 0
    }
    case _ => 0
  }))

  lazy val maxComponents = inventory.items.foldLeft(0)((sum, stack) => sum + (stack match {
    case Some(item) => Registry.itemDriverFor(item) match {
      case Some(driver: driver.Processor) => driver.supportedComponents(item)
      case _ => 0
    }
    case _ => 0
  }))

  def hasCPU = inventory.items.exists {
    case Some(stack) => Registry.itemDriverFor(stack) match {
      case Some(driver) => driver.slot(stack) == Slot.Processor
      case _ => false
    }
    case _ => false
  }

  override def world = rack.world

  override def markAsChanged() = rack.markAsChanged()

  // ----------------------------------------------------------------------- //

  override def onConnect(node: Node) = inventory.onConnect(node)

  override def onDisconnect(node: Node) = inventory.onDisconnect(node)

  def load(nbt: NBTTagCompound) {
    machine.load(nbt.getCompoundTag("machine"))
  }

  def save(nbt: NBTTagCompound) {
    nbt.setNewCompoundTag("machine", machine.save)
    // Dummy tag compound, we just want to flush the components to the actual
    // tag compound, which is the one of the stack representing us.
    inventory.save(new NBTTagCompound())
    inventory.onInventoryChanged()
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

    override def container = if (containerOverride != null) containerOverride else rack.getStackInSlot(number)

    override def node() = machine.node

    override def onMessage(message: Message) {}

    override def componentContainer = rack

    // Resolves conflict between ComponentInventory and ServerInventory.
    override def getInventoryStackLimit = 1
  }

}

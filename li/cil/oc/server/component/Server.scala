package li.cil.oc.server.component

import li.cil.oc.api
import li.cil.oc.api.driver
import li.cil.oc.api.network.{Message, Node}
import li.cil.oc.common.inventory.ComponentInventory
import li.cil.oc.common.inventory.ServerInventory
import li.cil.oc.common.tileentity
import li.cil.oc.server.driver.Registry
import li.cil.oc.util.ExtendedNBT._
import net.minecraft.item.ItemStack
import net.minecraft.nbt.NBTTagCompound

class Server(val rack: tileentity.Rack, val number: Int) extends Machine.Owner {
  val machine = new Machine(this)

  val inventory = new NetworkedInventory with ComponentInventory {
    var containerOverride: ItemStack = _

    def container = if (containerOverride != null) containerOverride else rack.getStackInSlot(number)

    def node() = machine.node

    def onMessage(message: Message) {}

    def componentContainer = rack

    // Resolves conflict between ComponentInventory and ServerInventory.
    override def getInventoryStackLimit = 1
  }

  // ----------------------------------------------------------------------- //

  def installedMemory = inventory.items.foldLeft(0)((sum, stack) => sum + (stack match {
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

  def world = rack.world

  def markAsChanged() = rack.markAsChanged()

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
  trait NetworkedInventory extends ServerInventory with api.network.Environment {
    def onConnect(node: Node) {}

    def onDisconnect(node: Node) {}
  }

}

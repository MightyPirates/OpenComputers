package li.cil.oc.server.component

import li.cil.oc.api
import li.cil.oc.api.driver
import li.cil.oc.api.network.{Message, Node}
import li.cil.oc.common.inventory.ComponentInventory
import li.cil.oc.common.inventory.ServerInventory
import li.cil.oc.common.tileentity
import li.cil.oc.server.driver.Registry

class Server(val rack: tileentity.Rack, val number: Int) extends Computer.Owner {
  val inventory = new NetworkedInventory with ComponentInventory {
    def container = rack.getStackInSlot(number)

    def node() = rack.servers(number) match {
      case Some(computer) => computer.node
      case _ => null
    }

    def onMessage(message: Message) {}

    def componentContainer = rack

    // Resolves conflict between ComponentInventory and ServerInventory.
    override def getInventoryStackLimit = 1
  }

  def installedMemory = inventory.items.foldLeft(0)((sum, stack) => sum + (stack match {
    case Some(item) => Registry.itemDriverFor(item) match {
      case Some(driver: driver.Memory) => driver.amount(item)
      case _ => 0
    }
    case _ => 0
  }))

  def world = rack.world

  def markAsChanged() = rack.markAsChanged()

  override def onConnect(node: Node) = inventory.onConnect(node)

  override def onDisconnect(node: Node) = inventory.onDisconnect(node)

  // Required due to abstract overrides in component inventory.
  trait NetworkedInventory extends ServerInventory with api.network.Environment {
    def onConnect(node: Node) {}

    def onDisconnect(node: Node) {}
  }

}

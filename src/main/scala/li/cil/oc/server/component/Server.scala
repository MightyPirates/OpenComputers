package li.cil.oc.server.component

import java.lang.Iterable

import li.cil.oc.api
import li.cil.oc.api.Machine
import li.cil.oc.api.internal
import li.cil.oc.api.machine.MachineHost
import li.cil.oc.api.network.Environment
import li.cil.oc.api.network.Message
import li.cil.oc.api.network.Node
import li.cil.oc.api.network.Visibility
import li.cil.oc.common.inventory.ComponentInventory
import li.cil.oc.common.inventory.ServerInventory
import li.cil.oc.common.item
import li.cil.oc.common.item.Delegator
import li.cil.oc.common.tileentity
import li.cil.oc.util.ExtendedNBT._
import net.minecraft.item.ItemStack
import net.minecraft.nbt.NBTTagCompound

import scala.collection.convert.WrapAsJava._

class Server(val rack: tileentity.ServerRack, val slot: Int) extends Environment with MachineHost with internal.Server {
  val machine = Machine.create(this)

  val inventory = new NetworkedInventory()

  // Used to grab messages when not connected to any side in the server rack.
  val node = api.Network.newNode(this, Visibility.Network).create()

  machine.onHostChanged()

  def tier = Delegator.subItem(rack.getStackInSlot(slot)) match {
    case Some(server: item.Server) => server.tier
    case _ => 0
  }

  // ----------------------------------------------------------------------- //

  override def internalComponents(): Iterable[ItemStack] = (0 until inventory.getSizeInventory).collect {
    case i if inventory.isComponentSlot(i) && inventory.getStackInSlot(i) != null => inventory.getStackInSlot(i)
  }

  override def componentSlot(address: String) = inventory.components.indexWhere(_.exists(env => env.node != null && env.node.address == address))

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

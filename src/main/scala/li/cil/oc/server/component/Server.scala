package li.cil.oc.server.component

import java.lang.Iterable
import java.util

import li.cil.oc.api
import li.cil.oc.api.Machine
import li.cil.oc.api.component.RackMountable
import li.cil.oc.api.driver
import li.cil.oc.api.internal
import li.cil.oc.api.internal.StateAware.State
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
import net.minecraft.entity.player.EntityPlayer
import net.minecraft.item.ItemStack
import net.minecraft.nbt.NBTTagCompound

import scala.collection.convert.WrapAsJava._

class Server(val rack: tileentity.Rack, val slot: Int) extends Environment with MachineHost with ServerInventory with ComponentInventory with RackMountable with internal.Server {
  val machine = Machine.create(this)

  // Used to grab messages when not connected to any side in the server rack.
  val node = api.Network.newNode(this, Visibility.Network).create()

  machine.onHostChanged() // TODO ???

  // ----------------------------------------------------------------------- //
  // Environment

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

  override def onMessage(message: Message) {
    // If we're internal mode and this server is not connected to any side, we
    // must manually propagate network messages to other servers in the rack.
    // Ensure the message originated in our local network, to avoid infinite
    // recursion if two unconnected servers are in one server rack.
    //    if (rack.internalSwitch && message.name == "network.message" &&
    //      rack.sides(this.slot).isEmpty && // Only if we're in internal mode.
    //      message.source != machine.node && // In this case it was relayed from another internal machine.
    //      node.network.node(message.source.address) != null) {
    //      for (slot <- rack.servers.indices) {
    //        rack.servers(slot) match {
    //          case Some(server) if server != this => server.machine.node.sendToNeighbors(message.name, message.data: _*)
    //          case _ =>
    //        }
    //      }
    //    }
  }

  override def load(nbt: NBTTagCompound) {
    super.load(nbt)
    machine.load(nbt.getCompoundTag("machine"))
  }

  override def save(nbt: NBTTagCompound) {
    super.save(nbt)
    nbt.setNewCompoundTag("machine", machine.save)
  }

  // ----------------------------------------------------------------------- //
  // MachineHost

  override def internalComponents(): Iterable[ItemStack] = (0 until getSizeInventory).collect {
    case i if getStackInSlot(i) != null && isComponentSlot(i, getStackInSlot(i)) => getStackInSlot(i)
  }

  override def componentSlot(address: String) = components.indexWhere(_.exists(env => env.node != null && env.node.address == address))

  override def onMachineConnect(node: Node) {
    if (node == machine.node) {
      node.connect(this.node)
    }
    onConnect(node)
  }

  override def onMachineDisconnect(node: Node) = onDisconnect(node)

  // ----------------------------------------------------------------------- //
  // EnvironmentHost

  override def xPosition = rack.x + 0.5

  override def yPosition = rack.y + 0.5

  override def zPosition = rack.z + 0.5

  override def world = rack.world

  override def markChanged() = rack.markChanged()

  // ----------------------------------------------------------------------- //
  // ServerInventory

  override def tier = Delegator.subItem(container) match {
    case Some(server: item.Server) => server.tier
    case _ => 0
  }

  // ----------------------------------------------------------------------- //
  // ItemStackInventory

  override def host = rack

  // ----------------------------------------------------------------------- //
  // ComponentInventory

  override def container = rack.getStackInSlot(slot)

  override protected def connectItemNode(node: Node) {
    if (machine.node != null && node != null) {
      api.Network.joinNewNetwork(machine.node)
      machine.node.connect(node)
    }
  }

  // ----------------------------------------------------------------------- //
  // RackMountable

  override def getNodeCount: Int = ???

  override def getNodeAt(index: Int): Node = ???

  override def onActivate(player: EntityPlayer): Unit = ???

  // ----------------------------------------------------------------------- //
  // ManagedEnvironment

  override def canUpdate: Boolean = ???

  override def update(): Unit = ???

  // ----------------------------------------------------------------------- //
  // StateAware

  override def getCurrentState: util.EnumSet[State] = {
    if (machine.isRunning) util.EnumSet.of(internal.StateAware.State.IsWorking)
    else util.EnumSet.noneOf(classOf[internal.StateAware.State])
  }

  // ----------------------------------------------------------------------- //
  // Analyzable

  override def onAnalyze(player: EntityPlayer, side: Int, hitX: Float, hitY: Float, hitZ: Float) = Array(machine.node)
}

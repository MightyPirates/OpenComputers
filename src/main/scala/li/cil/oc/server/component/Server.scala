package li.cil.oc.server.component

import java.lang.Iterable
import java.util

import li.cil.oc.api
import li.cil.oc.api.Machine
import li.cil.oc.api.internal
import li.cil.oc.api.internal.StateAware.State
import li.cil.oc.api.machine.MachineHost
import li.cil.oc.api.network.Analyzable
import li.cil.oc.api.network.Environment
import li.cil.oc.api.network.Message
import li.cil.oc.api.network.Node
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

class Server(val rack: tileentity.Rack, val slot: Int) extends Environment with MachineHost with ServerInventory with ComponentInventory with Analyzable with internal.Server {
  val machine = Machine.create(this)

  val node = machine.node

  var lastAccess = 0L

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

  override def onMachineConnect(node: Node) = onConnect(node)

  override def onMachineDisconnect(node: Node) = onDisconnect(node)

  // ----------------------------------------------------------------------- //
  // EnvironmentHost

  override def xPosition = rack.xPosition

  override def yPosition = rack.yPosition

  override def zPosition = rack.zPosition

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
    if (node != null) {
      api.Network.joinNewNetwork(machine.node)
      machine.node.connect(node)
    }
  }

  // ----------------------------------------------------------------------- //
  // RackMountable

  override def getData: NBTTagCompound = {
    val nbt = new NBTTagCompound()
    nbt.setBoolean("isRunning", machine.isRunning)
    nbt.setLong("lastAccess", lastAccess)
    nbt.setBoolean("hasErrored", machine.lastError() != null)
    nbt
  }

  override def getNodeCount: Int = 1 // TODO network cards and such

  override def getNodeAt(index: Int): Node = {
    if (index == 0) machine.node
    else ???
  }

  override def onActivate(player: EntityPlayer): Unit = {} // TODO server inventory

  // ----------------------------------------------------------------------- //
  // ManagedEnvironment

  override def canUpdate: Boolean = true

  override def update(): Unit = {
    val wasRunning = machine.isRunning
    val hadErrored = machine.lastError != null

    machine.update()

    val isRunning = machine.isRunning
    val hasErrored = machine.lastError != null
    if (isRunning != wasRunning || hasErrored != hadErrored) {
      rack.markChanged(slot)
    }

    updateComponents()
  }

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

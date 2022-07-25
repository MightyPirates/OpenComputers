package li.cil.oc.server.component

import java.lang.Iterable
import java.util

import li.cil.oc.Constants
import li.cil.oc.api.driver.DeviceInfo.DeviceAttribute
import li.cil.oc.api.driver.DeviceInfo.DeviceClass
import li.cil.oc.OpenComputers
import li.cil.oc.api
import li.cil.oc.api.Machine
import li.cil.oc.api.component.RackBusConnectable
import li.cil.oc.api.driver.DeviceInfo
import li.cil.oc.api.internal
import li.cil.oc.api.internal.Rack
import li.cil.oc.api.machine
import li.cil.oc.api.machine.MachineHost
import li.cil.oc.api.network.Analyzable
import li.cil.oc.api.network.Environment
import li.cil.oc.api.network.Message
import li.cil.oc.api.network.Node
import li.cil.oc.common.GuiType
import li.cil.oc.common.InventorySlots
import li.cil.oc.common.Slot
import li.cil.oc.common.Tier
import li.cil.oc.common.inventory.ComponentInventory
import li.cil.oc.common.inventory.ServerInventory
import li.cil.oc.common.item
import li.cil.oc.common.item.Delegator
import li.cil.oc.server.network.Connector
import li.cil.oc.util.BlockPosition
import li.cil.oc.util.ExtendedNBT._
import net.minecraft.entity.player.PlayerEntity
import net.minecraft.item.ItemStack
import net.minecraft.nbt.CompoundNBT
import net.minecraft.util.Direction
import net.minecraft.util.Hand
import net.minecraft.world.World
import net.minecraftforge.common.capabilities.Capability
import net.minecraftforge.common.capabilities.ICapabilityProvider
import net.minecraftforge.common.util.LazyOptional

import scala.collection.convert.WrapAsJava._

class Server(val rack: api.internal.Rack, val slot: Int) extends Environment with MachineHost with ServerInventory with ComponentInventory with Analyzable with internal.Server with ICapabilityProvider with DeviceInfo {
  lazy val machine: api.machine.Machine = Machine.create(this)

  val node: Node = if (!rack.world.isClientSide) machine.node else null

  var wasRunning = false
  var hadErrored = false
  var lastFileSystemAccess = 0L
  var lastNetworkActivity = 0L

  private final lazy val deviceInfo = Map(
    DeviceAttribute.Class -> DeviceClass.System,
    DeviceAttribute.Description -> "Server",
    DeviceAttribute.Vendor -> Constants.DeviceInfo.DefaultVendor,
    DeviceAttribute.Product -> "Blader",
    DeviceAttribute.Capacity -> getContainerSize.toString
  )

  override def getDeviceInfo: util.Map[String, String] = deviceInfo

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
  }

  private final val MachineTag = "machine"

  override def loadData(nbt: CompoundNBT) {
    super.loadData(nbt)
    if (!rack.world.isClientSide) {
      machine.loadData(nbt.getCompound(MachineTag))
    }
  }

  override def saveData(nbt: CompoundNBT) {
    super.saveData(nbt)
    if (!rack.world.isClientSide) {
      nbt.setNewCompoundTag(MachineTag, machine.saveData)
    }
  }

  // ----------------------------------------------------------------------- //
  // MachineHost

  override def internalComponents(): Iterable[ItemStack] = (0 until getContainerSize).collect {
    case i if !getItem(i).isEmpty && isComponentSlot(i, getItem(i)) => getItem(i)
  }

  override def componentSlot(address: String): Int = components.indexWhere(_.exists(env => env.node != null && env.node.address == address))

  override def onMachineConnect(node: Node): Unit = onConnect(node)

  override def onMachineDisconnect(node: Node): Unit = onDisconnect(node)

  // ----------------------------------------------------------------------- //
  // EnvironmentHost

  override def xPosition: Double = rack.xPosition

  override def yPosition: Double = rack.yPosition

  override def zPosition: Double = rack.zPosition

  override def world: World = rack.world

  override def markChanged(): Unit = rack.markChanged()

  // ----------------------------------------------------------------------- //
  // ServerInventory

  override def tier: Int = Delegator.subItem(container) match {
    case Some(server: item.Server) => server.tier
    case _ => 0
  }

  override def stillValid(player: PlayerEntity): Boolean = rack.stillValid(player)

  // ----------------------------------------------------------------------- //
  // ItemStackInventory

  override def host: Rack = rack

  // ----------------------------------------------------------------------- //
  // ComponentInventory

  override def container: ItemStack = rack.getItem(slot)

  override protected def connectItemNode(node: Node) {
    if (node != null) {
      api.Network.joinNewNetwork(machine.node)
      machine.node.connect(node)
    }
  }

  override protected def onItemRemoved(slot: Int, stack: ItemStack): Unit = {
    super.onItemRemoved(slot, stack)
    if (!rack.world.isClientSide) {
      val slotType = InventorySlots.server(tier)(slot).slot
      if (slotType == Slot.CPU) {
        machine.stop()
      }
    }
  }

  // ----------------------------------------------------------------------- //
  // RackMountable

  override def getData: CompoundNBT = {
    val nbt = new CompoundNBT()
    nbt.putBoolean("isRunning", wasRunning)
    nbt.putBoolean("hasErrored", hadErrored)
    nbt.putLong("lastFileSystemAccess", lastFileSystemAccess)
    nbt.putLong("lastNetworkActivity", lastNetworkActivity)
    nbt
  }

  override def getConnectableCount: Int = components.count {
    case Some(_: RackBusConnectable) => true
    case _ => false
  }

  override def getConnectableAt(index: Int): RackBusConnectable = components.collect {
    case Some(busConnectable: RackBusConnectable) => busConnectable
  }.apply(index)

  override def onActivate(player: PlayerEntity, hand: Hand, heldItem: ItemStack, hitX: Float, hitY: Float): Boolean = {
    if (!player.level.isClientSide) {
      if (player.isCrouching) {
        if (!machine.isRunning && stillValid(player)) {
          wasRunning = false
          hadErrored = false
          machine.start()
        }
      }
      else {
        val position = BlockPosition(rack)
        OpenComputers.openGui(player, GuiType.ServerInRack.id, world, position.x, GuiType.embedSlot(position.y, slot), position.z)
      }
    }
    true
  }

  // ----------------------------------------------------------------------- //
  // ManagedEnvironment

  override def canUpdate: Boolean = true

  override def update(): Unit = {
    if (!rack.world.isClientSide) {
      machine.update()

      val isRunning = machine.isRunning
      val hasErrored = machine.lastError != null
      if (isRunning != wasRunning || hasErrored != hadErrored) {
        rack.markChanged(slot)
      }
      wasRunning = isRunning
      hadErrored = hasErrored
      if (tier == Tier.Four) node.asInstanceOf[Connector].changeBuffer(Double.PositiveInfinity)
    }

    updateComponents()
  }

  // ----------------------------------------------------------------------- //
  // StateAware

  override def getCurrentState: util.EnumSet[api.util.StateAware.State] = {
    if (machine.isRunning) util.EnumSet.of(api.util.StateAware.State.IsWorking)
    else util.EnumSet.noneOf(classOf[api.util.StateAware.State])
  }

  // ----------------------------------------------------------------------- //
  // Analyzable

  override def onAnalyze(player: PlayerEntity, side: Direction, hitX: Float, hitY: Float, hitZ: Float) = Array(machine.node)

  // ----------------------------------------------------------------------- //
  // ICapabilityProvider

  override def getCapability[T](capability: Capability[T], facing: Direction): LazyOptional[T] = {
    for (curr <- components) curr match {
      case Some(comp: ICapabilityProvider) => {
        val cap = comp.getCapability(capability, host.toLocal(facing))
        if (cap.isPresent) return cap
      }
      case _ =>
    }
    LazyOptional.empty[T]
  }
}

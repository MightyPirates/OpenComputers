package li.cil.oc.common.tileentity

import java.util

import cpw.mods.fml.common.Optional.Method
import cpw.mods.fml.relauncher.Side
import cpw.mods.fml.relauncher.SideOnly
import li.cil.oc.Settings
import li.cil.oc.api.Driver
import li.cil.oc.api.component.RackMountable
import li.cil.oc.api.internal
import li.cil.oc.api.network.Analyzable
import li.cil.oc.api.network.ComponentHost
import li.cil.oc.api.network.Connector
import li.cil.oc.api.network.ManagedEnvironment
import li.cil.oc.api.network.Node
import li.cil.oc.common.Slot
import li.cil.oc.integration.Mods
import li.cil.oc.integration.opencomputers.DriverRedstoneCard
import li.cil.oc.integration.stargatetech2.DriverAbstractBusCard
import li.cil.oc.server.{PacketSender => ServerPacketSender}
import li.cil.oc.util.ExtendedInventory._
import net.minecraft.entity.player.EntityPlayer
import net.minecraft.inventory.IInventory
import net.minecraft.item.ItemStack
import net.minecraftforge.common.util.ForgeDirection

class Rack extends traits.PowerAcceptor with traits.Hub with traits.PowerBalancer with traits.ComponentInventory with traits.Rotatable with traits.BundledRedstoneAware with traits.AbstractBusAware with Analyzable with internal.ServerRack with traits.StateAware {
  private val lastWorking = new Array[Boolean](getSizeInventory)

  // Map node connections for each installed mountable. Each mountable may
  // have up to four outgoing connections, with the first one always being
  // the "primary" connection, i.e. being a direct connection allowing
  // component access (i.e. actually connecting to that side of the rack).
  // The other nodes are "secondary" connections and merely transfer network
  // messages.
  private val nodeMapping = Array.fill(getSizeInventory)(new Array[(Int, ForgeDirection)](4))

  // ----------------------------------------------------------------------- //
  // Hub

  override def canConnect(side: ForgeDirection) = side != facing

  override def sidedNode(side: ForgeDirection): Node = if (side != facing) super.sidedNode(side) else null

  // ----------------------------------------------------------------------- //
  // power.Common

  @SideOnly(Side.CLIENT)
  override protected def hasConnector(side: ForgeDirection) = side != facing

  override protected def connector(side: ForgeDirection) = Option(if (side != facing) sidedNode(side).asInstanceOf[Connector] else null)

  override def energyThroughput = Settings.get.serverRackRate

  // ----------------------------------------------------------------------- //
  // Analyzable

  override def onAnalyze(player: EntityPlayer, side: Int, hitX: Float, hitY: Float, hitZ: Float): Array[Node] = {
    slotAt(ForgeDirection.getOrientation(side), hitX, hitY, hitZ) match {
      case Some(slot) => components(slot) match {
        case Some(analyzable: Analyzable) => analyzable.onAnalyze(player, side, hitX, hitY, hitZ)
        case _ => null
      }
      case _ => Array(sidedNode(ForgeDirection.getOrientation(side)))
    }
  }

  // ----------------------------------------------------------------------- //
  // AbstractBusAware

  override def installedComponents: Iterable[ManagedEnvironment] = components.collect {
    case Some(mountable: RackMountable with ComponentHost) => mountable.getComponents.collect {
      case managed: ManagedEnvironment => managed
    }
  }.flatten

  @Method(modid = Mods.IDs.StargateTech2)
  override def getInterfaces(side: Int) = if (side != facing.ordinal) {
    super.getInterfaces(side)
  }
  else null

  override def getWorld = world

  // ----------------------------------------------------------------------- //
  // internal.Rack

  override def getMountable(slot: Int): RackMountable = components(slot) match {
    case Some(mountable: RackMountable) => mountable
    case _ => null
  }

  // ----------------------------------------------------------------------- //
  // StateAware

  override def getCurrentState = {
    val result = util.EnumSet.noneOf(classOf[internal.StateAware.State])
    components.collect {
      case Some(mountable: RackMountable) => result.addAll(mountable.getCurrentState)
    }
    result
  }

  // ----------------------------------------------------------------------- //
  // Rotatable

  override protected def onRotationChanged() {
    super.onRotationChanged()
    checkRedstoneInputChanged()
  }

  // ----------------------------------------------------------------------- //
  // RedstoneAware

  override protected def onRedstoneInputChanged(side: ForgeDirection, oldMaxValue: Int, newMaxValue: Int) {
    super.onRedstoneInputChanged(side, oldMaxValue, newMaxValue)
    components.collect {
      case Some(mountable: RackMountable) => mountable.node.sendToNeighbors("redstone.changed", toLocal(side), int2Integer(oldMaxValue), int2Integer(newMaxValue))
    }
  }

  // ----------------------------------------------------------------------- //
  // IInventory

  override def getSizeInventory = 4

  override def getInventoryStackLimit = 1

  override def isItemValidForSlot(slot: Int, stack: ItemStack): Boolean = (slot, Option(Driver.driverFor(stack, getClass))) match {
    case (0, Some(driver)) => driver.slot(stack) == Slot.RackMountable
    case _ => false
  }

  override def markDirty() {
    super.markDirty()
    if (isServer) {
      isOutputEnabled = hasRedstoneCard
      isAbstractBusAvailable = hasAbstractBusCard
      ServerPacketSender.sendServerPresence(this)
    }
    else {
      world.markBlockForUpdate(x, y, z)
    }
  }

  // ----------------------------------------------------------------------- //
  // ComponentInventory

  override protected def onItemAdded(slot: Int, stack: ItemStack): Unit = {
    super.onItemAdded(slot, stack)
  }

  override protected def onItemRemoved(slot: Int, stack: ItemStack): Unit = {
    super.onItemRemoved(slot, stack)
  }

  // ----------------------------------------------------------------------- //

  override def canUpdate = isServer

  override def updateEntity() {
    super.updateEntity()
    if (isServer && isConnected) {
      components.zipWithIndex.collect {
        case (Some(mountable: RackMountable), slot) if isWorking(mountable) != lastWorking(slot) =>
          lastWorking(slot) = isWorking(mountable)
          ServerPacketSender.sendServerState(this, slot)
          world.notifyBlocksOfNeighborChange(x, y, z, block)
          // These are working state dependent, so recompute them.
          isOutputEnabled = hasRedstoneCard
          isAbstractBusAvailable = hasAbstractBusCard
      }

      updateComponents()
    }
  }

  // ----------------------------------------------------------------------- //

  def stacksForSide(side: ForgeDirection): IndexedSeq[ItemStack] =
    if (side == facing) new Array[ItemStack](4)
    else nodeMapping(toLocal(side).ordinal).map(info => getStackInSlot(info._1))

  def nodesForSide(side: ForgeDirection): IndexedSeq[Node] =
    if (side == facing) new Array[Node](4)
    else nodeMapping(toLocal(side).ordinal).map {
      case (slot, nodeNum) => (components(slot), nodeNum)
    }.collect {
      case (Some(mountable: RackMountable), nodeNum) => mountable.getNodeAt(nodeNum)
    }

  def slotAt(side: ForgeDirection, hitX: Float, hitY: Float, hitZ: Float) = {
    if (side == facing) {
      val l = 2 / 16.0
      val h = 14 / 16.0
      val slot = (((1 - hitY) - l) / (h - l) * getSizeInventory).toInt
      Some(math.max(0, math.min(getSizeInventory - 1, slot)))
    }
    else None
  }

  def isWorking(mountable: RackMountable) = mountable.getCurrentState.contains(internal.StateAware.State.IsWorking)

  def hasAbstractBusCard = components.exists {
    case Some(mountable: RackMountable with IInventory) if isWorking(mountable) =>
      mountable.exists(stack => DriverAbstractBusCard.worksWith(stack, mountable.getClass))
    case _ => false
  }

  def hasRedstoneCard = components.exists {
    case Some(mountable: RackMountable with IInventory) if isWorking(mountable) =>
      mountable.exists(stack => DriverRedstoneCard.worksWith(stack, mountable.getClass))
    case _ => false
  }
}

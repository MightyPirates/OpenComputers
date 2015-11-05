package li.cil.oc.common.tileentity

import java.util

import cpw.mods.fml.common.Optional.Method
import cpw.mods.fml.relauncher.Side
import cpw.mods.fml.relauncher.SideOnly
import li.cil.oc.Settings
import li.cil.oc.api
import li.cil.oc.api.Driver
import li.cil.oc.api.StateAware
import li.cil.oc.api.component.RackMountable
import li.cil.oc.api.internal
import li.cil.oc.api.network.Analyzable
import li.cil.oc.api.network.ComponentHost
import li.cil.oc.api.network.Connector
import li.cil.oc.api.network.EnvironmentHost
import li.cil.oc.api.network.ManagedEnvironment
import li.cil.oc.api.network.Node
import li.cil.oc.api.network.Packet
import li.cil.oc.common.Slot
import li.cil.oc.integration.Mods
import li.cil.oc.integration.opencomputers.DriverRedstoneCard
import li.cil.oc.integration.stargatetech2.DriverAbstractBusCard
import li.cil.oc.server.{PacketSender => ServerPacketSender}
import li.cil.oc.util.ExtendedInventory._
import net.minecraft.entity.player.EntityPlayer
import net.minecraft.inventory.IInventory
import net.minecraft.item.ItemStack
import net.minecraft.nbt.NBTTagCompound
import net.minecraftforge.common.util.ForgeDirection

import scala.collection.convert.WrapAsJava._
import scala.collection.convert.WrapAsScala._

class Rack extends traits.PowerAcceptor with traits.Hub with traits.PowerBalancer with traits.ComponentInventory with traits.Rotatable with traits.BundledRedstoneAware with traits.AbstractBusAware with Analyzable with internal.Rack with traits.StateAware {
  var isRelayEnabled = true
  val lastData = new Array[NBTTagCompound](getSizeInventory)
  val hasChanged = new Array[Boolean](getSizeInventory)

  // Map node connections for each installed mountable. Each mountable may
  // have up to four outgoing connections, with the first one always being
  // the "primary" connection, i.e. being a direct connection allowing
  // component access (i.e. actually connecting to that side of the rack).
  // The other nodes are "secondary" connections and merely transfer network
  // messages.
  val nodeMapping = Array.fill(getSizeInventory)(Array.fill[Option[ForgeDirection]](4)(None))

  def connect(mountableIndex: Int, nodeIndex: Int, side: Option[ForgeDirection]): Unit = {
    val newSide = side match {
      case Some(direction) if direction != ForgeDirection.UNKNOWN => Option(direction)
      case _ => None
    }

    val oldSide = nodeMapping(mountableIndex)(nodeIndex)
    if (oldSide == newSide) return

    // If it's the primary node cut the direct connection.
    if (nodeIndex == 0 && oldSide.isDefined) {
      val node = getMountable(mountableIndex).node()
      val plug = sidedNode(oldSide.get)
      if (node != null && plug != null) {
        node.disconnect(plug)
      }
    }

    nodeMapping(mountableIndex)(nodeIndex) = newSide

    // If it's the primary node establish a direct connection.
    if (nodeIndex == 0 && newSide.isDefined) {
      val node = getMountable(mountableIndex).node()
      val plug = sidedNode(newSide.get)
      if (node != null && plug != null) {
        node.connect(plug)
      }
    }
  }

  // ----------------------------------------------------------------------- //
  // Hub

  override protected def relayPacket(sourceSide: Option[ForgeDirection], packet: Packet): Unit = {
    if (isRelayEnabled) super.relayPacket(sourceSide, packet)

    // When a message arrives on a bus, also send it to all secondary nodes
    // connected to it. Only deliver it to that very node, if it's not the
    // sender, to avoid loops.
    for (mountableIndex <- 0 until getSizeInventory) {
      val mapping = nodeMapping(mountableIndex)
      for (nodeIndex <- 0 until 3) {
        mapping(nodeIndex + 1) match {
          case Some(side) if sourceSide.contains(toGlobal(side)) =>
            val mountable = getMountable(mountableIndex)
            if (mountable != null) {
              val connectable = mountable.getConnectableAt(nodeIndex)
              if (connectable != null) {
                connectable.receivePacket(packet)
              }
            }
          case _ => // Not connected to a bus.
        }
      }
    }
  }

  // ----------------------------------------------------------------------- //
  // SidedEnvironment

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

  override def installedComponents: Iterable[ManagedEnvironment] = asJavaIterable(components.collect {
    case Some(mountable: RackMountable with ComponentHost) => iterableAsScalaIterable(mountable.getComponents).collect {
      case managed: ManagedEnvironment => managed
    }
  }.flatten.toIterable)

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

  override def markChanged(slot: Int): Unit = {
    hasChanged.synchronized(hasChanged(slot) = true)
    isOutputEnabled = hasRedstoneCard
    isAbstractBusAvailable = hasAbstractBusCard
  }

  // ----------------------------------------------------------------------- //
  // StateAware

  override def getCurrentState = {
    val result = util.EnumSet.noneOf(classOf[StateAware.State])
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
      case Some(mountable: RackMountable) if mountable.node != null =>
        mountable.node.sendToNeighbors("redstone.changed", toLocal(side), int2Integer(oldMaxValue), int2Integer(newMaxValue))
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
      ServerPacketSender.sendRackInventory(this)
    }
    else {
      world.markBlockForUpdate(x, y, z)
    }
  }

  // ----------------------------------------------------------------------- //
  // TileEntity

  override def canUpdate = isServer

  override def updateEntity() {
    super.updateEntity()
    if (isServer && isConnected) {
      components.zipWithIndex.collect {
        case (Some(mountable: RackMountable), slot) if hasChanged(slot) =>
          lastData(slot) = mountable.getData
          ServerPacketSender.sendRackMountableData(this, slot)
          world.notifyBlocksOfNeighborChange(x, y, z, block)
          // These are working state dependent, so recompute them.
          isOutputEnabled = hasRedstoneCard
          isAbstractBusAvailable = hasAbstractBusCard
      }

      updateComponents()
    }
  }

  // ----------------------------------------------------------------------- //

  override def readFromNBTForServer(nbt: NBTTagCompound): Unit = {
    super.readFromNBTForServer(nbt)
    // TODO read lastState and nodeMapping

    // Kickstart initialization.
    _isOutputEnabled = hasRedstoneCard
    _isAbstractBusAvailable = hasAbstractBusCard
  }

  override def writeToNBTForServer(nbt: NBTTagCompound): Unit = {
    super.writeToNBTForServer(nbt)
    // TODO store lastState and nodeMapping
  }

  @SideOnly(Side.CLIENT) override
  def readFromNBTForClient(nbt: NBTTagCompound): Unit = {
    super.readFromNBTForClient(nbt)
    // TODO read lastState
  }

  override def writeToNBTForClient(nbt: NBTTagCompound): Unit = {
    super.writeToNBTForClient(nbt)
    // TODO store lastState
  }

  // ----------------------------------------------------------------------- //

  def slotAt(side: ForgeDirection, hitX: Float, hitY: Float, hitZ: Float) = {
    if (side == facing) {
      val l = 2 / 16.0
      val h = 14 / 16.0
      val slot = (((1 - hitY) - l) / (h - l) * getSizeInventory).toInt
      Some(math.max(0, math.min(getSizeInventory - 1, slot)))
    }
    else None
  }

  def isWorking(mountable: RackMountable) = mountable.getCurrentState.contains(api.StateAware.State.IsWorking)

  def hasAbstractBusCard = components.exists {
    case Some(mountable: EnvironmentHost with RackMountable with IInventory) if isWorking(mountable) =>
      mountable.exists(stack => DriverAbstractBusCard.worksWith(stack, mountable.getClass))
    case _ => false
  }

  def hasRedstoneCard = components.exists {
    case Some(mountable: EnvironmentHost with RackMountable with IInventory) if isWorking(mountable) =>
      mountable.exists(stack => DriverRedstoneCard.worksWith(stack, mountable.getClass))
    case _ => false
  }
}

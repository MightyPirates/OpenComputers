package li.cil.oc.common.tileentity

import java.util.UUID

import li.cil.oc.api
import li.cil.oc.api.internal
import li.cil.oc.api.internal.MultiTank
import li.cil.oc.api.machine.Arguments
import li.cil.oc.api.machine.Callback
import li.cil.oc.api.machine.Context
import li.cil.oc.api.machine.Machine
import li.cil.oc.api.network._
import li.cil.oc.common.inventory.InventoryProxy
import li.cil.oc.common.tileentity.traits.RedstoneAware
import li.cil.oc.server.agent.Player
import li.cil.oc.server.{PacketSender => ServerPacketSender}
import net.minecraftforge.common.capabilities.Capability
import net.minecraftforge.fluids.capability.CapabilityFluidHandler
import net.minecraftforge.fluids.capability.IFluidHandler
import net.minecraftforge.fml.relauncher.Side
import net.minecraftforge.fml.relauncher.SideOnly
import net.minecraft.entity.Entity
import net.minecraft.entity.player.EntityPlayer
import net.minecraft.inventory.ISidedInventory
import net.minecraft.item.ItemStack
import net.minecraft.nbt.NBTTagCompound
import net.minecraft.util.EnumFacing
import net.minecraft.util.math.AxisAlignedBB
import net.minecraftforge.fluids.Fluid
import net.minecraftforge.fluids.FluidStack
import net.minecraftforge.fluids.IFluidTank
import net.minecraftforge.fluids.capability.IFluidTankProperties

class RobotProxy(val robot: Robot) extends traits.Computer with traits.PowerInformation with traits.RotatableTile with ISidedInventory with IFluidHandler with internal.Robot {
  def this() = this(new Robot())

  // ----------------------------------------------------------------------- //

  override def getCapability[T](capability: Capability[T], facing: EnumFacing): T = {
    if (capability == CapabilityFluidHandler.FLUID_HANDLER_CAPABILITY)
      capability.cast(this.asInstanceOf[T])
    else super.getCapability(capability, facing)
  }

  override val node: Component = api.Network.newNode(this, Visibility.Network).
    withComponent("robot", Visibility.Neighbors).
    create()

  override def machine: Machine = robot.machine

  override def tier: Int = robot.tier

  override def equipmentInventory: InventoryProxy {
    def inventory: Robot

    def getSizeInventory: Int
  } = robot.equipmentInventory

  override def mainInventory: InventoryProxy {
    def offset: Int

    def inventory: Robot

    def getSizeInventory: Int
  } = robot.mainInventory

  override def tank: MultiTank {
    def tankCount: Int

    def getFluidTank(index: Int): ManagedEnvironment with IFluidTank
  } = robot.tank

  override def selectedSlot: Int = robot.selectedSlot

  override def setSelectedSlot(index: Int): Unit = robot.setSelectedSlot(index)

  override def selectedTank: Int = robot.selectedTank

  override def setSelectedTank(index: Int): Unit = robot.setSelectedTank(index)

  override def player: Player = robot.player()

  override def name: String = robot.name

  override def setName(name: String): Unit = robot.setName(name)

  override def ownerName: String = robot.ownerName

  override def ownerUUID: UUID = robot.ownerUUID

  // ----------------------------------------------------------------------- //

  override def connectComponents() {}

  override def disconnectComponents() {}

  override def isRunning: Boolean = robot.isRunning

  override def setRunning(value: Boolean): Unit = robot.setRunning(value)

  override def shouldAnimate(): Boolean = robot.shouldAnimate

  // ----------------------------------------------------------------------- //

  override def componentCount: Int = robot.componentCount

  override def getComponentInSlot(index: Int): ManagedEnvironment = robot.getComponentInSlot(index)

  override def synchronizeSlot(slot: Int): Unit = robot.synchronizeSlot(slot)

  // ----------------------------------------------------------------------- //

  @Callback(doc = """function():boolean -- Starts the robot. Returns true if the state changed.""")
  def start(context: Context, args: Arguments): Array[AnyRef] =
    result(!machine.isPaused && machine.start())

  @Callback(doc = """function():boolean -- Stops the robot. Returns true if the state changed.""")
  def stop(context: Context, args: Arguments): Array[AnyRef] =
    result(machine.stop())

  @Callback(direct = true, doc = """function():boolean -- Returns whether the robot is running.""")
  def isRunning(context: Context, args: Arguments): Array[AnyRef] =
    result(machine.isRunning)

  @Callback(doc = "function(name: string):string -- Sets a new name and returns the old name. Robot must not be running")
  def setName(context: Context, args: Arguments): Array[AnyRef] = {
    val oldName = robot.name
    val newName: String = args.checkString(0)
    if (machine.isRunning) return result(Unit, "is running")
    setName(newName)
    ServerPacketSender.sendRobotNameChange(robot)
    result(oldName)
  }

  @Callback(doc = "function():string -- Returns the robot name.")
  def getName(context: Context, args: Arguments): Array[AnyRef] = result(robot.name)

  override def onMessage(message: Message) {
    super.onMessage(message)
    if (message.name == "network.message" && message.source != this.node) message.data match {
      case Array(packet: Packet) => robot.node.sendToReachable(message.name, packet)
      case _ =>
    }
  }

  // ----------------------------------------------------------------------- //

  override def updateEntity() {
    robot.updateEntity()
  }

  override def validate() {
    super.validate()
    val firstProxy = robot.proxy == null
    robot.proxy = this
    robot.setWorld(getWorld)
    robot.setPos(getPos)
    if (firstProxy) {
      robot.validate()
    }
    if (isServer) {
      // Use the same address we use internally on the outside.
      val nbt = new NBTTagCompound()
      nbt.setString("address", robot.node.address)
      node.load(nbt)
    }
  }

  override def dispose() {
    super.dispose()
    if (robot.proxy == this) {
      robot.dispose()
    }
  }

  override def readFromNBTForServer(nbt: NBTTagCompound) {
    robot.info.load(nbt)
    super.readFromNBTForServer(nbt)
    robot.readFromNBTForServer(nbt)
  }

  override def writeToNBTForServer(nbt: NBTTagCompound) {
    super.writeToNBTForServer(nbt)
    robot.writeToNBTForServer(nbt)
  }

  override def save(nbt: NBTTagCompound): Unit = robot.save(nbt)

  override def load(nbt: NBTTagCompound): Unit = robot.load(nbt)

  @SideOnly(Side.CLIENT)
  override def readFromNBTForClient(nbt: NBTTagCompound): Unit = robot.readFromNBTForClient(nbt)

  override def writeToNBTForClient(nbt: NBTTagCompound): Unit = robot.writeToNBTForClient(nbt)

  override def getMaxRenderDistanceSquared: Double = robot.getMaxRenderDistanceSquared

  override def getRenderBoundingBox: AxisAlignedBB = robot.getRenderBoundingBox

  override def shouldRenderInPass(pass: Int): Boolean = robot.shouldRenderInPass(pass)

  override def markDirty(): Unit = robot.markDirty()

  // ----------------------------------------------------------------------- //

  override def onAnalyze(player: EntityPlayer, side: EnumFacing, hitX: Float, hitY: Float, hitZ: Float): Array[Node] = robot.onAnalyze(player, side, hitX, hitY, hitZ)

  // ----------------------------------------------------------------------- //

  override protected[tileentity] val _input: Array[Int] = robot._input

  override protected[tileentity] val _output: Array[Int] = robot._output

  override protected[tileentity] val _bundledInput: Array[Array[Int]] = robot._bundledInput

  override protected[tileentity] val _rednetInput: Array[Array[Int]] = robot._rednetInput

  override protected[tileentity] val _bundledOutput: Array[Array[Int]] = robot._bundledOutput

  override def isOutputEnabled: Boolean = robot.isOutputEnabled

  override def setOutputEnabled(value: Boolean): RedstoneAware = robot.setOutputEnabled(value)

  override def checkRedstoneInputChanged(): Unit = robot.checkRedstoneInputChanged()

  /* TORO RedLogic
    @Optional.Method(modid = Mods.IDs.RedLogic)
    override def connects(wire: IWire, blockFace: Int, fromDirection: Int) = robot.connects(wire, blockFace, fromDirection)

    @Optional.Method(modid = Mods.IDs.RedLogic)
    override def connectsAroundCorner(wire: IWire, blockFace: Int, fromDirection: Int) = robot.connectsAroundCorner(wire, blockFace, fromDirection)

    @Optional.Method(modid = Mods.IDs.RedLogic)
    override def getBundledCableStrength(blockFace: Int, toDirection: Int) = robot.getBundledCableStrength(blockFace, toDirection)

    @Optional.Method(modid = Mods.IDs.RedLogic)
    override def getEmittedSignalStrength(blockFace: Int, toDirection: Int) = robot.getEmittedSignalStrength(blockFace, toDirection)

    @Optional.Method(modid = Mods.IDs.RedLogic)
    override def onBundledInputChanged() = robot.onBundledInputChanged()

    @Optional.Method(modid = Mods.IDs.RedLogic)
    override def onRedstoneInputChanged() = robot.onRedstoneInputChanged()
  */

  // ----------------------------------------------------------------------- //

  override def pitch: EnumFacing = robot.pitch

  override def pitch_=(value: EnumFacing): Unit = robot.pitch_=(value)

  override def yaw: EnumFacing = robot.yaw

  override def yaw_=(value: EnumFacing): Unit = robot.yaw_=(value)

  override def setFromEntityPitchAndYaw(entity: Entity): Boolean = robot.setFromEntityPitchAndYaw(entity)

  override def setFromFacing(value: EnumFacing): Boolean = robot.setFromFacing(value)

  override def invertRotation(): Boolean = robot.invertRotation()

  override def facing: EnumFacing = robot.facing

  override def rotate(axis: EnumFacing): Boolean = robot.rotate(axis)

  override def toLocal(value: EnumFacing): EnumFacing = robot.toLocal(value)

  override def toGlobal(value: EnumFacing): EnumFacing = robot.toGlobal(value)

  // ----------------------------------------------------------------------- //

  override def getStackInSlot(i: Int): ItemStack = robot.getStackInSlot(i)

  override def decrStackSize(slot: Int, amount: Int): ItemStack = robot.decrStackSize(slot, amount)

  override def setInventorySlotContents(slot: Int, stack: ItemStack): Unit = robot.setInventorySlotContents(slot, stack)

  override def removeStackFromSlot(slot: Int): ItemStack = robot.removeStackFromSlot(slot)

  override def openInventory(player: EntityPlayer): Unit = robot.openInventory(player)

  override def closeInventory(player: EntityPlayer): Unit = robot.closeInventory(player)

  override def hasCustomName: Boolean = robot.hasCustomName

  override def isUsableByPlayer(player: EntityPlayer): Boolean = robot.isUsableByPlayer(player)

  override def dropSlot(slot: Int, count: Int, direction: Option[EnumFacing]): Boolean = robot.dropSlot(slot, count, direction)

  override def dropAllSlots(): Unit = robot.dropAllSlots()

  override def getInventoryStackLimit: Int = robot.getInventoryStackLimit

  override def componentSlot(address: String): Int = robot.componentSlot(address)

  override def getName: String = robot.getName

  override def getSizeInventory: Int = robot.getSizeInventory

  override def isItemValidForSlot(slot: Int, stack: ItemStack): Boolean = robot.isItemValidForSlot(slot, stack)

  // ----------------------------------------------------------------------- //

  override def canExtractItem(slot: Int, stack: ItemStack, side: EnumFacing): Boolean = robot.canExtractItem(slot, stack, side)

  override def canInsertItem(slot: Int, stack: ItemStack, side: EnumFacing): Boolean = robot.canInsertItem(slot, stack, side)

  override def getSlotsForFace(side: EnumFacing): Array[Int] = robot.getSlotsForFace(side)

  // ----------------------------------------------------------------------- //

  override def hasRedstoneCard: Boolean = robot.hasRedstoneCard

  // ----------------------------------------------------------------------- //

  override def globalBuffer: Double = robot.globalBuffer

  override def globalBuffer_=(value: Double): Unit = robot.globalBuffer = value

  override def globalBufferSize: Double = robot.globalBufferSize

  override def globalBufferSize_=(value: Double): Unit = robot.globalBufferSize = value

  // ----------------------------------------------------------------------- //

  override def fill(resource: FluidStack, doFill: Boolean): Int = robot.fill(resource, doFill)

  override def drain(resource: FluidStack, doDrain: Boolean): FluidStack = robot.drain(resource, doDrain)

  override def drain(maxDrain: Int, doDrain: Boolean): FluidStack = robot.drain(maxDrain, doDrain)

  def canFill(fluid: Fluid): Boolean = robot.canFill(fluid)

  def canDrain(fluid: Fluid): Boolean = robot.canDrain(fluid)

  override def getTankProperties: Array[IFluidTankProperties] = robot.getTankProperties
}

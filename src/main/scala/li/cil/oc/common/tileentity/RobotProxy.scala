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
import net.minecraftforge.common.util.LazyOptional
import net.minecraftforge.common.util.NonNullSupplier
import net.minecraftforge.fluids.capability.CapabilityFluidHandler
import net.minecraftforge.fluids.capability.IFluidHandler
import net.minecraftforge.fluids.capability.IFluidHandler.FluidAction
import net.minecraftforge.api.distmarker.Dist
import net.minecraftforge.api.distmarker.OnlyIn
import net.minecraft.entity.Entity
import net.minecraft.entity.player.PlayerEntity
import net.minecraft.fluid.Fluid
import net.minecraft.inventory.ISidedInventory
import net.minecraft.item.ItemStack
import net.minecraft.nbt.CompoundNBT
import net.minecraft.tileentity.TileEntity
import net.minecraft.tileentity.TileEntityType
import net.minecraft.util.Direction
import net.minecraft.util.math.AxisAlignedBB
import net.minecraft.util.text.ITextComponent
import net.minecraftforge.fluids.FluidStack
import net.minecraftforge.fluids.IFluidTank

class RobotProxy(selfType: TileEntityType[_ <: RobotProxy], val robot: Robot) extends TileEntity(selfType)
  with traits.Computer with traits.PowerInformation with traits.RotatableTile with ISidedInventory with IFluidHandler with internal.Robot {

  def this(selfType: TileEntityType[_ <: RobotProxy]) = this(selfType, new Robot())

  // ----------------------------------------------------------------------- //

  private val wrapper = LazyOptional.of(new NonNullSupplier[IFluidHandler] {
    override def get = RobotProxy.this
  })

  override def invalidateCaps() {
    super.invalidateCaps()
    wrapper.invalidate()
  }

  override def getCapability[T](capability: Capability[T], facing: Direction): LazyOptional[T] = {
    if (capability == CapabilityFluidHandler.FLUID_HANDLER_CAPABILITY)
      wrapper.cast[T]
    else super.getCapability(capability, facing)
  }

  override val node: Component = api.Network.newNode(this, Visibility.Network).
    withComponent("robot", Visibility.Neighbors).
    create()

  override def machine: Machine = robot.machine

  override def tier: Int = robot.tier

  override def equipmentInventory: InventoryProxy {
    def inventory: Robot

    def getContainerSize: Int
  } = robot.equipmentInventory

  override def mainInventory: InventoryProxy {
    def offset: Int

    def inventory: Robot

    def getContainerSize: Int
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

  override def clearRemoved() {
    super.clearRemoved()
    val firstProxy = robot.proxy == null
    robot.proxy = this
    robot.setLevelAndPosition(getLevel, getBlockPos)
    if (firstProxy) {
      robot.clearRemoved()
    }
    if (isServer) {
      // Use the same address we use internally on the outside.
      val nbt = new CompoundNBT()
      nbt.putString("address", robot.node.address)
      node.loadData(nbt)
    }
  }

  override def dispose() {
    super.dispose()
    if (robot.proxy == this) {
      robot.dispose()
    }
  }

  override def loadForServer(nbt: CompoundNBT) {
    robot.info.loadData(nbt)
    super.loadForServer(nbt)
    robot.loadForServer(nbt)
  }

  override def saveForServer(nbt: CompoundNBT) {
    super.saveForServer(nbt)
    robot.saveForServer(nbt)
  }

  override def saveData(nbt: CompoundNBT): Unit = robot.saveData(nbt)

  override def loadData(nbt: CompoundNBT): Unit = robot.loadData(nbt)

  @OnlyIn(Dist.CLIENT)
  override def loadForClient(nbt: CompoundNBT): Unit = robot.loadForClient(nbt)

  override def saveForClient(nbt: CompoundNBT): Unit = robot.saveForClient(nbt)

  @OnlyIn(Dist.CLIENT)
  override def getViewDistance: Double = robot.getViewDistance

  override def getRenderBoundingBox: AxisAlignedBB = robot.getRenderBoundingBox

  override def setChanged(): Unit = robot.setChanged()

  // ----------------------------------------------------------------------- //

  override def onAnalyze(player: PlayerEntity, side: Direction, hitX: Float, hitY: Float, hitZ: Float): Array[Node] = robot.onAnalyze(player, side, hitX, hitY, hitZ)

  // ----------------------------------------------------------------------- //

  override protected[tileentity] val _input: Array[Int] = robot._input

  override protected[tileentity] val _output: Array[Int] = robot._output

  override protected[tileentity] val _bundledInput: Array[Array[Int]] = robot._bundledInput

  override protected[tileentity] val _rednetInput: Array[Array[Int]] = robot._rednetInput

  override protected[tileentity] val _bundledOutput: Array[Array[Int]] = robot._bundledOutput

  override def isOutputEnabled: Boolean = robot.isOutputEnabled

  override def setOutputEnabled(value: Boolean): RedstoneAware = robot.setOutputEnabled(value)

  override def checkRedstoneInputChanged(): Unit = robot.checkRedstoneInputChanged()

  // ----------------------------------------------------------------------- //

  override def pitch: Direction = robot.pitch

  override def pitch_=(value: Direction): Unit = robot.pitch_=(value)

  override def yaw: Direction = robot.yaw

  override def yaw_=(value: Direction): Unit = robot.yaw_=(value)

  override def setFromEntityPitchAndYaw(entity: Entity): Boolean = robot.setFromEntityPitchAndYaw(entity)

  override def setFromFacing(value: Direction): Boolean = robot.setFromFacing(value)

  override def invertRotation(): Boolean = robot.invertRotation()

  override def facing: Direction = robot.facing

  override def rotate(axis: Direction): Boolean = robot.rotate(axis)

  override def toLocal(value: Direction): Direction = robot.toLocal(value)

  override def toGlobal(value: Direction): Direction = robot.toGlobal(value)

  // ----------------------------------------------------------------------- //

  override def getItem(i: Int): ItemStack = robot.getItem(i)

  override def removeItem(slot: Int, amount: Int): ItemStack = robot.removeItem(slot, amount)

  override def setItem(slot: Int, stack: ItemStack): Unit = robot.setItem(slot, stack)

  override def removeItemNoUpdate(slot: Int): ItemStack = robot.removeItemNoUpdate(slot)

  override def startOpen(player: PlayerEntity): Unit = robot.startOpen(player)

  override def stopOpen(player: PlayerEntity): Unit = robot.stopOpen(player)

  override def hasCustomName: Boolean = robot.hasCustomName

  override def stillValid(player: PlayerEntity): Boolean = robot.stillValid(player)

  override def dropSlot(slot: Int, count: Int, direction: Option[Direction]): Boolean = robot.dropSlot(slot, count, direction)

  override def dropAllSlots(): Unit = robot.dropAllSlots()

  override def getMaxStackSize: Int = robot.getMaxStackSize

  override def componentSlot(address: String): Int = robot.componentSlot(address)

  override def getName: ITextComponent = robot.getName

  override def getContainerSize: Int = robot.getContainerSize

  override def canPlaceItem(slot: Int, stack: ItemStack): Boolean = robot.canPlaceItem(slot, stack)

  // ----------------------------------------------------------------------- //

  override def canTakeItemThroughFace(slot: Int, stack: ItemStack, side: Direction): Boolean = robot.canTakeItemThroughFace(slot, stack, side)

  override def canPlaceItemThroughFace(slot: Int, stack: ItemStack, side: Direction): Boolean = robot.canPlaceItemThroughFace(slot, stack, side)

  override def getSlotsForFace(side: Direction): Array[Int] = robot.getSlotsForFace(side)

  // ----------------------------------------------------------------------- //

  override def hasRedstoneCard: Boolean = robot.hasRedstoneCard

  // ----------------------------------------------------------------------- //

  override def globalBuffer: Double = robot.globalBuffer

  override def globalBuffer_=(value: Double): Unit = robot.globalBuffer = value

  override def globalBufferSize: Double = robot.globalBufferSize

  override def globalBufferSize_=(value: Double): Unit = robot.globalBufferSize = value

  // ----------------------------------------------------------------------- //

  override def getTanks: Int = robot.getTanks

  override def getFluidInTank(tank: Int): FluidStack = robot.getFluidInTank(tank)

  override def getTankCapacity(tank: Int): Int = robot.getTankCapacity(tank)

  override def isFluidValid(tank: Int, resource: FluidStack): Boolean = robot.isFluidValid(tank, resource)

  override def fill(resource: FluidStack, action: FluidAction): Int = robot.fill(resource, action)

  override def drain(resource: FluidStack, action: FluidAction): FluidStack = robot.drain(resource, action)

  override def drain(maxDrain: Int, action: FluidAction): FluidStack = robot.drain(maxDrain, action)
}

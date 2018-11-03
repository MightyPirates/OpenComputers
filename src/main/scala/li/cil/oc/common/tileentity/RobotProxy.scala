package li.cil.oc.common.tileentity

import li.cil.oc.api
import li.cil.oc.api.internal
import li.cil.oc.api.machine.Arguments
import li.cil.oc.api.machine.Callback
import li.cil.oc.api.machine.Context
import li.cil.oc.api.network._
import li.cil.oc.server.{PacketSender => ServerPacketSender}
import net.minecraftforge.fml.relauncher.Side
import net.minecraftforge.fml.relauncher.SideOnly

/* TODO RedLogic
import mods.immibis.redlogic.api.wiring.IWire
*/

import net.minecraft.entity.Entity
import net.minecraft.entity.player.EntityPlayer
import net.minecraft.inventory.ISidedInventory
import net.minecraft.item.ItemStack
import net.minecraft.nbt.NBTTagCompound
import net.minecraft.util.EnumFacing
import net.minecraftforge.fluids.Fluid
import net.minecraftforge.fluids.FluidStack
import net.minecraftforge.fluids.IFluidHandler

class RobotProxy(val robot: Robot) extends traits.Computer with traits.PowerInformation with traits.RotatableTile with ISidedInventory with IFluidHandler with internal.Robot {
  def this() = this(new Robot())

  // ----------------------------------------------------------------------- //

  override val node = api.Network.newNode(this, Visibility.Network).
    withComponent("robot", Visibility.Neighbors).
    create()

  override def machine = robot.machine

  override def tier = robot.tier

  override def equipmentInventory = robot.equipmentInventory

  override def mainInventory = robot.mainInventory

  override def tank = robot.tank

  override def selectedSlot = robot.selectedSlot

  override def setSelectedSlot(index: Int) = robot.setSelectedSlot(index)

  override def selectedTank = robot.selectedTank

  override def setSelectedTank(index: Int) = robot.setSelectedTank(index)

  override def player = robot.player()

  override def name = robot.name

  override def setName(name: String): Unit = robot.setName(name)

  override def ownerName = robot.ownerName

  override def ownerUUID = robot.ownerUUID

  // ----------------------------------------------------------------------- //

  override def connectComponents() {}

  override def disconnectComponents() {}

  override def isRunning = robot.isRunning

  override def setRunning(value: Boolean) = robot.setRunning(value)

  override def shouldAnimate(): Boolean = robot.shouldAnimate

  // ----------------------------------------------------------------------- //

  override def componentCount = robot.componentCount

  override def getComponentInSlot(index: Int) = robot.getComponentInSlot(index)

  override def synchronizeSlot(slot: Int) = robot.synchronizeSlot(slot)

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
    robot.setWorldObj(worldObj)
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

  override def save(nbt: NBTTagCompound) = robot.save(nbt)

  override def load(nbt: NBTTagCompound) = robot.load(nbt)

  @SideOnly(Side.CLIENT)
  override def readFromNBTForClient(nbt: NBTTagCompound) = robot.readFromNBTForClient(nbt)

  override def writeToNBTForClient(nbt: NBTTagCompound) = robot.writeToNBTForClient(nbt)

  override def getMaxRenderDistanceSquared = robot.getMaxRenderDistanceSquared

  override def getRenderBoundingBox = robot.getRenderBoundingBox

  override def shouldRenderInPass(pass: Int) = robot.shouldRenderInPass(pass)

  override def markDirty() = robot.markDirty()

  // ----------------------------------------------------------------------- //

  override def onAnalyze(player: EntityPlayer, side: EnumFacing, hitX: Float, hitY: Float, hitZ: Float) = robot.onAnalyze(player, side, hitX, hitY, hitZ)

  // ----------------------------------------------------------------------- //

  override protected[tileentity] val _input = robot._input

  override protected[tileentity] val _output = robot._output

  override protected[tileentity] val _bundledInput = robot._bundledInput

  override protected[tileentity] val _rednetInput = robot._rednetInput

  override protected[tileentity] val _bundledOutput = robot._bundledOutput

  override def isOutputEnabled: Boolean = robot.isOutputEnabled

  override def setOutputEnabled(value: Boolean): Unit = robot.setOutputEnabled(value)

  override def checkRedstoneInputChanged() = robot.checkRedstoneInputChanged()

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

  override def pitch = robot.pitch

  override def pitch_=(value: EnumFacing) = robot.pitch_=(value)

  override def yaw = robot.yaw

  override def yaw_=(value: EnumFacing) = robot.yaw_=(value)

  override def setFromEntityPitchAndYaw(entity: Entity) = robot.setFromEntityPitchAndYaw(entity)

  override def setFromFacing(value: EnumFacing) = robot.setFromFacing(value)

  override def invertRotation() = robot.invertRotation()

  override def facing = robot.facing

  override def rotate(axis: EnumFacing) = robot.rotate(axis)

  override def toLocal(value: EnumFacing) = robot.toLocal(value)

  override def toGlobal(value: EnumFacing) = robot.toGlobal(value)

  // ----------------------------------------------------------------------- //

  override def getStackInSlot(i: Int) = robot.getStackInSlot(i)

  override def decrStackSize(slot: Int, amount: Int) = robot.decrStackSize(slot, amount)

  override def setInventorySlotContents(slot: Int, stack: ItemStack) = robot.setInventorySlotContents(slot, stack)

  override def removeStackFromSlot(slot: Int) = robot.removeStackFromSlot(slot)

  override def openInventory(player: EntityPlayer) = robot.openInventory(player)

  override def closeInventory(player: EntityPlayer) = robot.closeInventory(player)

  override def hasCustomName = robot.hasCustomName

  override def isUseableByPlayer(player: EntityPlayer) = robot.isUseableByPlayer(player)

  override def dropSlot(slot: Int, count: Int, direction: Option[EnumFacing]) = robot.dropSlot(slot, count, direction)

  override def dropAllSlots() = robot.dropAllSlots()

  override def getInventoryStackLimit = robot.getInventoryStackLimit

  override def componentSlot(address: String) = robot.componentSlot(address)

  override def getName = robot.getName

  override def getSizeInventory = robot.getSizeInventory

  override def isItemValidForSlot(slot: Int, stack: ItemStack) = robot.isItemValidForSlot(slot, stack)

  // ----------------------------------------------------------------------- //

  override def canExtractItem(slot: Int, stack: ItemStack, side: EnumFacing) = robot.canExtractItem(slot, stack, side)

  override def canInsertItem(slot: Int, stack: ItemStack, side: EnumFacing) = robot.canInsertItem(slot, stack, side)

  override def getSlotsForFace(side: EnumFacing) = robot.getSlotsForFace(side)

  // ----------------------------------------------------------------------- //

  override def hasRedstoneCard = robot.hasRedstoneCard

  // ----------------------------------------------------------------------- //

  override def globalBuffer = robot.globalBuffer

  override def globalBuffer_=(value: Double) = robot.globalBuffer = value

  override def globalBufferSize = robot.globalBufferSize

  override def globalBufferSize_=(value: Double) = robot.globalBufferSize = value

  // ----------------------------------------------------------------------- //

  override def fill(from: EnumFacing, resource: FluidStack, doFill: Boolean) = robot.fill(from, resource, doFill)

  override def drain(from: EnumFacing, resource: FluidStack, doDrain: Boolean) = robot.drain(from, resource, doDrain)

  override def drain(from: EnumFacing, maxDrain: Int, doDrain: Boolean) = robot.drain(from, maxDrain, doDrain)

  override def canFill(from: EnumFacing, fluid: Fluid) = robot.canFill(from, fluid)

  override def canDrain(from: EnumFacing, fluid: Fluid) = robot.canDrain(from, fluid)

  override def getTankInfo(from: EnumFacing) = robot.getTankInfo(from)
}

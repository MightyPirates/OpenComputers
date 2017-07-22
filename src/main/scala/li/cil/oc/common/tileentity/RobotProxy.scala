package li.cil.oc.common.tileentity

import cpw.mods.fml.common.Optional
import cpw.mods.fml.relauncher.Side
import cpw.mods.fml.relauncher.SideOnly
import li.cil.oc.api
import li.cil.oc.api.internal
import li.cil.oc.api.machine.Arguments
import li.cil.oc.api.machine.Callback
import li.cil.oc.api.machine.Context
import li.cil.oc.api.network._
import li.cil.oc.integration.Mods
import mods.immibis.redlogic.api.wiring.IWire
import net.minecraft.entity.Entity
import net.minecraft.entity.player.EntityPlayer
import net.minecraft.inventory.ISidedInventory
import net.minecraft.item.ItemStack
import net.minecraft.nbt.NBTTagCompound
import net.minecraftforge.common.util.ForgeDirection
import net.minecraftforge.fluids.Fluid
import net.minecraftforge.fluids.FluidStack
import net.minecraftforge.fluids.IFluidHandler

class RobotProxy(val robot: Robot) extends traits.Computer with traits.PowerInformation with ISidedInventory with IFluidHandler with internal.Robot {
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
    robot.xCoord = xCoord
    robot.yCoord = yCoord
    robot.zCoord = zCoord
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

  override def onAnalyze(player: EntityPlayer, side: Int, hitX: Float, hitY: Float, hitZ: Float) = robot.onAnalyze(player, side, hitX, hitY, hitZ)

  // ----------------------------------------------------------------------- //

  override protected[tileentity] val _input = robot._input

  override protected[tileentity] val _output = robot._output

  override protected[tileentity] val _bundledInput = robot._bundledInput

  override protected[tileentity] val _rednetInput = robot._rednetInput

  override protected[tileentity] val _bundledOutput = robot._bundledOutput

  override def isOutputEnabled = robot.isOutputEnabled

  override def isOutputEnabled_=(value: Boolean) = robot.isOutputEnabled_=(value)

  override def checkRedstoneInputChanged() = robot.checkRedstoneInputChanged()

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

  // ----------------------------------------------------------------------- //

  override def pitch = robot.pitch

  override def pitch_=(value: ForgeDirection) = robot.pitch_=(value)

  override def yaw = robot.yaw

  override def yaw_=(value: ForgeDirection) = robot.yaw_=(value)

  override def setFromEntityPitchAndYaw(entity: Entity) = robot.setFromEntityPitchAndYaw(entity)

  override def setFromFacing(value: ForgeDirection) = robot.setFromFacing(value)

  override def invertRotation() = robot.invertRotation()

  override def facing = robot.facing

  override def rotate(axis: ForgeDirection) = robot.rotate(axis)

  override def toLocal(value: ForgeDirection) = robot.toLocal(value)

  override def toGlobal(value: ForgeDirection) = robot.toGlobal(value)

  // ----------------------------------------------------------------------- //

  override def getStackInSlot(i: Int) = robot.getStackInSlot(i)

  override def decrStackSize(slot: Int, amount: Int) = robot.decrStackSize(slot, amount)

  override def setInventorySlotContents(slot: Int, stack: ItemStack) = robot.setInventorySlotContents(slot, stack)

  override def getStackInSlotOnClosing(slot: Int) = robot.getStackInSlotOnClosing(slot)

  override def openInventory() = robot.openInventory()

  override def closeInventory() = robot.closeInventory()

  override def hasCustomInventoryName = robot.hasCustomInventoryName

  override def isUseableByPlayer(player: EntityPlayer) = robot.isUseableByPlayer(player)

  override def dropSlot(slot: Int, count: Int, direction: Option[ForgeDirection]) = robot.dropSlot(slot, count, direction)

  override def dropAllSlots() = robot.dropAllSlots()

  override def getInventoryStackLimit = robot.getInventoryStackLimit

  override def componentSlot(address: String) = robot.componentSlot(address)

  override def getInventoryName = robot.getInventoryName

  override def getSizeInventory = robot.getSizeInventory

  override def isItemValidForSlot(slot: Int, stack: ItemStack) = robot.isItemValidForSlot(slot, stack)

  // ----------------------------------------------------------------------- //

  override def canExtractItem(slot: Int, stack: ItemStack, side: Int) = robot.canExtractItem(slot, stack, side)

  override def canInsertItem(slot: Int, stack: ItemStack, side: Int) = robot.canInsertItem(slot, stack, side)

  override def getAccessibleSlotsFromSide(side: Int) = robot.getAccessibleSlotsFromSide(side)

  // ----------------------------------------------------------------------- //

  override def hasRedstoneCard = robot.hasRedstoneCard

  // ----------------------------------------------------------------------- //

  override def globalBuffer = robot.globalBuffer

  override def globalBuffer_=(value: Double) = robot.globalBuffer = value

  override def globalBufferSize = robot.globalBufferSize

  override def globalBufferSize_=(value: Double) = robot.globalBufferSize = value

  // ----------------------------------------------------------------------- //

  override def fill(from: ForgeDirection, resource: FluidStack, doFill: Boolean) = robot.fill(from, resource, doFill)

  override def drain(from: ForgeDirection, resource: FluidStack, doDrain: Boolean) = robot.drain(from, resource, doDrain)

  override def drain(from: ForgeDirection, maxDrain: Int, doDrain: Boolean) = robot.drain(from, maxDrain, doDrain)

  override def canFill(from: ForgeDirection, fluid: Fluid) = robot.canFill(from, fluid)

  override def canDrain(from: ForgeDirection, fluid: Fluid) = robot.canDrain(from, fluid)

  override def getTankInfo(from: ForgeDirection) = robot.getTankInfo(from)
}

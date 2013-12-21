package li.cil.oc.common.tileentity

import cpw.mods.fml.common.Optional
import cpw.mods.fml.relauncher.{Side, SideOnly}
import li.cil.oc.api
import li.cil.oc.api.Network
import li.cil.oc.api.network._
import li.cil.oc.client.gui
import mods.immibis.redlogic.api.wiring.IWire
import net.minecraft.entity.Entity
import net.minecraft.entity.player.EntityPlayer
import net.minecraft.inventory.ISidedInventory
import net.minecraft.item.ItemStack
import net.minecraft.nbt.NBTTagCompound
import net.minecraftforge.common.ForgeDirection

class RobotProxy(val robot: Robot) extends Computer(robot.isClient) with ISidedInventory with Buffer with PowerInformation with RobotContext {
  def this() = this(new Robot(false))

  // ----------------------------------------------------------------------- //

  override val node = api.Network.newNode(this, Visibility.Network).
    withComponent("robot", Visibility.Neighbors).
    create()

  override protected val _computer = null

  override def computer = robot.computer

  // ----------------------------------------------------------------------- //

  // Note: we implement IRobotContext in the TE to allow external components
  //to cast their owner to it (to allow interacting with their owning robot).

  override def isRunning = robot.isRunning

  override def isRunning_=(value: Boolean) = robot.isRunning_=(value)

  def selectedSlot() = robot.selectedSlot

  def player() = robot.player()

  def saveUpgrade() = robot.saveUpgrade()

  // ----------------------------------------------------------------------- //

  @LuaCallback("start")
  def start(context: Context, args: Arguments): Array[AnyRef] =
    result(!computer.isPaused && computer.start())

  @LuaCallback("stop")
  def stop(context: Context, args: Arguments): Array[AnyRef] =
    result(computer.stop())

  @LuaCallback(value = "isRunning", direct = true)
  def isRunning(context: Context, args: Arguments): Array[AnyRef] =
    result(computer.isRunning)

  // ----------------------------------------------------------------------- //

  override def updateEntity() {
    if (node != null && node.network == null) {
      Network.joinOrCreateNetwork(this)
    }
    robot.updateEntity()
  }

  override def validate() {
    super.validate()
    val firstProxy = robot.proxy == null
    robot.proxy = this
    robot.worldObj = worldObj
    robot.xCoord = xCoord
    robot.yCoord = yCoord
    robot.zCoord = zCoord
    if (firstProxy) {
      robot.validate()
    }
  }

  override def invalidate() {
    super.invalidate()
    if (robot.proxy == this) {
      robot.invalidate()
    }
  }

  override def onChunkUnload() {
    super.onChunkUnload()
    if (robot.proxy == this) {
      robot.onChunkUnload()
    }
  }

  override def readFromNBT(nbt: NBTTagCompound) {
    super.readFromNBT(nbt)
    robot.readFromNBT(nbt)
  }

  override def writeToNBT(nbt: NBTTagCompound) {
    super.writeToNBT(nbt)
    robot.writeToNBT(nbt)
  }

  override def save(nbt: NBTTagCompound) = robot.save(nbt)

  override def load(nbt: NBTTagCompound) = robot.load(nbt)

  @SideOnly(Side.CLIENT)
  override def readFromNBTForClient(nbt: NBTTagCompound) = robot.readFromNBTForClient(nbt)

  override def writeToNBTForClient(nbt: NBTTagCompound) = robot.writeToNBTForClient(nbt)

  override def getMaxRenderDistanceSquared = robot.getMaxRenderDistanceSquared

  override def getRenderBoundingBox = robot.getRenderBoundingBox

  override def shouldRenderInPass(pass: Int) = robot.shouldRenderInPass(pass)

  override def onInventoryChanged() = robot.onInventoryChanged()

  override lazy val isClient = robot.isClient

  override lazy val isServer = robot.isServer

  // ----------------------------------------------------------------------- //

  override def onAnalyze(stats: NBTTagCompound, player: EntityPlayer, side: Int, hitX: Float, hitY: Float, hitZ: Float) = {
    robot.onAnalyze(stats, player, side, hitX, hitY, hitZ)
    node
  }

  // ----------------------------------------------------------------------- //

  override def isOutputEnabled = robot.isOutputEnabled

  override def isOutputEnabled_=(value: Boolean) = robot.isOutputEnabled_=(value)

  override def input(side: ForgeDirection) = robot.input(side)

  override def output(side: ForgeDirection) = robot.output(side)

  override def output(side: ForgeDirection, value: Int) = robot.output(side, value)

  override def bundledInput(side: ForgeDirection, color: Int) = robot.bundledInput(side, color)

  override def rednetInput(side: ForgeDirection, color: Int, value: Int) = robot.rednetInput(side, color, value)

  override def bundledOutput(side: ForgeDirection) = robot.bundledOutput(side)

  override def bundledOutput(side: ForgeDirection, color: Int) = robot.bundledOutput(side, color)

  override def bundledOutput(side: ForgeDirection, color: Int, value: Int) = robot.bundledOutput(side, color, value)

  override def checkRedstoneInputChanged() = robot.checkRedstoneInputChanged()

  override def updateRedstoneInput() = robot.updateRedstoneInput()

  @Optional.Method(modid = "RedLogic")
  override def connects(wire: IWire, blockFace: Int, fromDirection: Int) = robot.connects(wire, blockFace, fromDirection)

  @Optional.Method(modid = "RedLogic")
  override def connectsAroundCorner(wire: IWire, blockFace: Int, fromDirection: Int) = robot.connectsAroundCorner(wire, blockFace, fromDirection)

  @Optional.Method(modid = "RedLogic")
  override def getBundledCableStrength(blockFace: Int, toDirection: Int) = robot.getBundledCableStrength(blockFace, toDirection)

  @Optional.Method(modid = "RedLogic")
  override def getEmittedSignalStrength(blockFace: Int, toDirection: Int) = robot.getEmittedSignalStrength(blockFace, toDirection)

  @Optional.Method(modid = "RedLogic")
  override def onBundledInputChanged() = robot.onBundledInputChanged()

  @Optional.Method(modid = "RedLogic")
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

  override def openChest() = robot.openChest()

  override def closeChest() = robot.closeChest()

  override def isInvNameLocalized = robot.isInvNameLocalized

  override def isUseableByPlayer(player: EntityPlayer) = robot.isUseableByPlayer(player)

  override def dropSlot(slot: Int, count: Int, direction: ForgeDirection) = robot.dropSlot(slot, count, direction)

  override def dropAllSlots() = robot.dropAllSlots()

  override def getInventoryStackLimit = robot.getInventoryStackLimit

  override def installedMemory = robot.installedMemory

  def getInvName = robot.getInvName

  def getSizeInventory = robot.getSizeInventory

  def isItemValidForSlot(slot: Int, stack: ItemStack) = robot.isItemValidForSlot(slot, stack)

  // ----------------------------------------------------------------------- //

  def canExtractItem(slot: Int, stack: ItemStack, side: Int) = robot.canExtractItem(slot, stack, side)

  def canInsertItem(slot: Int, stack: ItemStack, side: Int) = robot.canInsertItem(slot, stack, side)

  def getAccessibleSlotsFromSide(side: Int) = robot.getAccessibleSlotsFromSide(side)

  // ----------------------------------------------------------------------- //

  override def markAsChanged() = robot.markAsChanged()

  override def hasRedstoneCard = robot.hasRedstoneCard

  // ----------------------------------------------------------------------- //

  override lazy val buffer = robot.buffer

  override def bufferIsDirty = robot.bufferIsDirty

  override def bufferIsDirty_=(value: Boolean) = robot.bufferIsDirty = value

  override def currentGui = robot.currentGui

  override def currentGui_=(value: Option[gui.Buffer]) = robot.currentGui = value

  def tier = robot.tier

  // ----------------------------------------------------------------------- //

  override def globalBuffer = robot.globalBuffer

  override def globalBuffer_=(value: Double) = robot.globalBuffer = value

  override def globalBufferSize = robot.globalBufferSize

  override def globalBufferSize_=(value: Double) = robot.globalBufferSize = value
}

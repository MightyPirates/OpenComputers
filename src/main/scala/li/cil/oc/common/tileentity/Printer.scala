package li.cil.oc.common.tileentity

import java.util

import cpw.mods.fml.relauncher.Side
import cpw.mods.fml.relauncher.SideOnly
import li.cil.oc.Settings
import li.cil.oc.api
import li.cil.oc.api.machine.Arguments
import li.cil.oc.api.machine.Callback
import li.cil.oc.api.machine.Context
import li.cil.oc.api.network._
import li.cil.oc.common.item.data.PrintData
import li.cil.oc.util.ExtendedAABB._
import li.cil.oc.util.ExtendedNBT._
import li.cil.oc.util.ItemUtils
import net.minecraft.item.ItemStack
import net.minecraft.nbt.NBTTagCompound
import net.minecraft.util.AxisAlignedBB
import net.minecraftforge.common.util.ForgeDirection

class Printer extends traits.Environment with traits.PowerAcceptor with traits.Inventory with traits.Rotatable with SidedEnvironment with traits.StateAware {
  val node = api.Network.newNode(this, Visibility.Network).
    withComponent("printer3d").
    withConnector(Settings.get.bufferConverter).
    create()

  var data = new PrintData()
  var isActive = false
  var output: Option[ItemStack] = None
  var totalRequiredEnergy = 0.0
  var requiredEnergy = 0.0

  val slotPlastic = 0
  val slotInk = 1
  val slotOutput = 2

  // ----------------------------------------------------------------------- //

  @SideOnly(Side.CLIENT)
  override def canConnect(side: ForgeDirection) = side != ForgeDirection.UP

  override def sidedNode(side: ForgeDirection) = if (side != ForgeDirection.UP) node else null

  @SideOnly(Side.CLIENT)
  override protected def hasConnector(side: ForgeDirection) = canConnect(side)

  override protected def connector(side: ForgeDirection) = Option(if (side != ForgeDirection.UP) node else null)

  override protected def energyThroughput = Settings.get.assemblerRate

  override def currentState = {
    if (isAssembling) util.EnumSet.of(traits.State.IsWorking)
    else if (canAssemble) util.EnumSet.of(traits.State.CanWork)
    else util.EnumSet.noneOf(classOf[traits.State])
  }

  // ----------------------------------------------------------------------- //

  def canAssemble = {
    val complexity = data.stateOff.size + data.stateOn.size
    complexity > 0 && complexity <= Settings.get.maxPrintComplexity
  }

  def isAssembling = requiredEnergy > 0

  def progress = (1 - requiredEnergy / totalRequiredEnergy) * 100

  def timeRemaining = (requiredEnergy / Settings.get.assemblerTickAmount / 20).toInt

  // ----------------------------------------------------------------------- //

  @Callback(doc = """function() -- Resets the configuration of the printer and stop printing (current job will finish).""")
  def reset(context: Context, args: Arguments): Array[Object] = {
    data = new PrintData()
    isActive = false // Needs committing.
    null
  }

  @Callback(doc = """function(value:string) -- Set a label for the block being printed.""")
  def setLabel(context: Context, args: Arguments): Array[Object] = {
    data.label = Option(args.optString(0, null))
    isActive = false // Needs committing.
    null
  }

  @Callback(doc = """function():string -- Get the current label for the block being printed.""")
  def getLabel(context: Context, args: Arguments): Array[Object] = {
    result(data.label.orNull)
  }

  @Callback(doc = """function(value:string) -- Set a tooltip for the block being printed.""")
  def setTooltip(context: Context, args: Arguments): Array[Object] = {
    data.tooltip = Option(args.optString(0, null))
    isActive = false // Needs committing.
    null
  }

  @Callback(doc = """function():string -- Get the current tooltip for the block being printed.""")
  def getTooltip(context: Context, args: Arguments): Array[Object] = {
    result(data.tooltip.orNull)
  }

  @Callback(doc = """function(minX:number, minY:number, minZ:number, maxX:number, maxY:number, maxZ:number, texture:string[, state:boolean=false]) -- Adds a shape to the printers configuration, optionally specifying whether it is for the off or on state.""")
  def addShape(context: Context, args: Arguments): Array[Object] = {
    if (data.stateOff.size + data.stateOn.size >= Settings.get.maxPrintComplexity) {
      return result(null, "model too complex")
    }
    val minX = (args.checkInteger(0) max 0 min 16) / 16f
    val minY = (args.checkInteger(1) max 0 min 16) / 16f
    val minZ = (args.checkInteger(2) max 0 min 16) / 16f
    val maxX = (args.checkInteger(3) max 0 min 16) / 16f
    val maxY = (args.checkInteger(4) max 0 min 16) / 16f
    val maxZ = (args.checkInteger(5) max 0 min 16) / 16f
    val texture = args.checkString(6)
    val state = args.optBoolean(7, false)

    if (minX == maxX) throw new IllegalArgumentException("empty block")
    if (minY == maxY) throw new IllegalArgumentException("empty block")
    if (minZ == maxZ) throw new IllegalArgumentException("empty block")

    val list = if (state) data.stateOn else data.stateOff
    list += new PrintData.Shape(AxisAlignedBB.getBoundingBox(
      math.min(minX, maxX),
      math.min(minY, maxY),
      math.min(minZ, maxZ),
      math.max(maxX, minX),
      math.max(maxY, minY),
      math.max(maxZ, minZ)), texture)
    isActive = false // Needs committing.

    result(true)
  }

  @Callback(doc = """function():number -- Get the number of shapes in the current configuration.""")
  def getShapeCount(context: Context, args: Arguments): Array[Object] = result(data.stateOff.size + data.stateOn.size)

  @Callback(doc = """function():number -- Get the maximum allowed number of shapes.""")
  def getMaxShapeCount(context: Context, args: Arguments): Array[Object] = result(Settings.get.maxPrintComplexity)

  @Callback(doc = """function():boolean -- Commit and begin printing the current configuration.""")
  def commit(context: Context, args: Arguments): Array[Object] = {
    if (!canAssemble) {
      return result(null, "model invalid")
    }
    isActive = true
    result(true)
  }

  @Callback(doc = """function(): string, number or boolean -- The current state of the printer, `busy' or `idle', followed by the progress or model validity, respectively.""")
  def status(context: Context, args: Arguments): Array[Object] = {
    if (isAssembling) result("busy", progress)
    else if (canAssemble) result("idle", true)
    else result("idle", false)
  }

  // ----------------------------------------------------------------------- //

  override def canUpdate = isServer

  // ----------------------------------------------------------------------- //

  override def updateEntity() {
    super.updateEntity()

    if (world.getTotalWorldTime % Settings.get.tickFrequency == 0) {
      if (isActive && output.isEmpty) {
        val totalVolume = data.stateOn.foldLeft(0)((acc, shape) => acc + shape.bounds.volume) + data.stateOff.foldLeft(0)((acc, shape) => acc + shape.bounds.volume)
        val totalSurface = data.stateOn.foldLeft(0)((acc, shape) => acc + shape.bounds.surface) + data.stateOff.foldLeft(0)((acc, shape) => acc + shape.bounds.surface)
        val totalShapes = data.stateOn.size + data.stateOff.size
        // TODO Consume plastic (totalVolume) and ink (totalSurface).
        totalRequiredEnergy = totalShapes * Settings.get.printShapeCost
        requiredEnergy = totalRequiredEnergy
        output = Option(data.createItemStack())
        //        ServerPacketSender.sendRobotAssembling(this, assembling = true)
      }

      if (output.isDefined) {
        val want = math.max(1, math.min(requiredEnergy, Settings.get.assemblerTickAmount * Settings.get.tickFrequency))
        val success = Settings.get.ignorePower || node.tryChangeBuffer(-want)
        if (success) {
          requiredEnergy -= want
        }
        if (requiredEnergy <= 0) {
          val result = getStackInSlot(slotOutput)
          if (result == null) {
            setInventorySlotContents(slotOutput, output.get)
          }
          else if (output.get.isItemEqual(result) && ItemStack.areItemStackTagsEqual(output.get, result) && result.stackSize < result.getMaxStackSize) {
            result.stackSize += 1
            markDirty()
          }
          else {
            return
          }
          requiredEnergy = 0
          output = None
        }
        //      ServerPacketSender.sendRobotAssembling(this, success && output.isDefined)
      }
    }
  }

  override def readFromNBTForServer(nbt: NBTTagCompound) {
    super.readFromNBTForServer(nbt)
    data.load(nbt.getCompoundTag("data"))
    isActive = nbt.getBoolean(Settings.namespace + "active")
    if (nbt.hasKey(Settings.namespace + "output")) {
      output = Option(ItemUtils.loadStack(nbt.getCompoundTag(Settings.namespace + "output")))
    }
    totalRequiredEnergy = nbt.getDouble(Settings.namespace + "total")
    requiredEnergy = nbt.getDouble(Settings.namespace + "remaining")
  }

  override def writeToNBTForServer(nbt: NBTTagCompound) {
    super.writeToNBTForServer(nbt)
    nbt.setNewCompoundTag("data", data.save)
    nbt.setBoolean(Settings.namespace + "active", isActive)
    output.foreach(stack => nbt.setNewCompoundTag(Settings.namespace + "output", stack.writeToNBT))
    nbt.setDouble(Settings.namespace + "total", totalRequiredEnergy)
    nbt.setDouble(Settings.namespace + "remaining", requiredEnergy)
  }

  @SideOnly(Side.CLIENT) override
  def readFromNBTForClient(nbt: NBTTagCompound) {
    super.readFromNBTForClient(nbt)
    requiredEnergy = nbt.getDouble("remaining")
  }

  override def writeToNBTForClient(nbt: NBTTagCompound) {
    super.writeToNBTForClient(nbt)
    nbt.setDouble("remaining", requiredEnergy)
  }

  // ----------------------------------------------------------------------- //

  override def getSizeInventory = 3

  override def getInventoryStackLimit = 64

  override def isItemValidForSlot(slot: Int, stack: ItemStack) =
    if (slot == 0)
      true // TODO Plastic
    else if (slot == 1)
      true // TODO Color
    else false
}

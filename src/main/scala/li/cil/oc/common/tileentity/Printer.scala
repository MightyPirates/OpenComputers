package li.cil.oc.common.tileentity

import java.util

import li.cil.oc.Constants
import li.cil.oc.api.driver.DeviceInfo.DeviceAttribute
import li.cil.oc.api.driver.DeviceInfo.DeviceClass
import li.cil.oc.Settings
import li.cil.oc.api
import li.cil.oc.api.driver.DeviceInfo
import li.cil.oc.api.machine.Arguments
import li.cil.oc.api.machine.Callback
import li.cil.oc.api.machine.Context
import li.cil.oc.api.network._
import li.cil.oc.common.item.data.PrintData
import li.cil.oc.common.tileentity.traits.RotatableImpl
import li.cil.oc.server.{PacketSender => ServerPacketSender}
import li.cil.oc.util.ExtendedNBT._
import net.minecraft.inventory.ISidedInventory
import net.minecraft.item.ItemStack
import net.minecraft.nbt.NBTTagCompound
import net.minecraft.util.EnumFacing
import net.minecraft.util.math.AxisAlignedBB
import net.minecraftforge.fml.relauncher.Side
import net.minecraftforge.fml.relauncher.SideOnly

import scala.collection.convert.WrapAsJava._

class Printer extends traits.Environment with traits.Inventory with RotatableImpl with SidedEnvironment with traits.StateAware with traits.Tickable with ISidedInventory with DeviceInfo {
  val getNode = api.Network.newNode(this, Visibility.NETWORK).
    withComponent("printer3d").
    withConnector(Settings.get.bufferConverter).
    create()

  val maxAmountMaterial = 256000
  var amountMaterial = 0
  val maxAmountInk = 100000
  var amountInk = 0

  var data = new PrintData()
  var isActive = false
  var limit = 0
  var output: Option[ItemStack] = None
  var totalRequiredEnergy = 0.0
  var requiredEnergy = 0.0

  val slotMaterial = 0
  val slotInk = 1
  val slotOutput = 2

  private final lazy val deviceInfo = Map(
    DeviceAttribute.Class -> DeviceClass.Printer,
    DeviceAttribute.Description -> "3D Printer",
    DeviceAttribute.Vendor -> Constants.DeviceInfo.DefaultVendor,
    DeviceAttribute.Product -> "Omni-Materializer T6.1"
  )

  override def getDeviceInfo: util.Map[String, String] = deviceInfo

  // ----------------------------------------------------------------------- //

  @SideOnly(Side.CLIENT)
  override def canConnect(side: EnumFacing) = side != EnumFacing.UP

  override def sidedNode(side: EnumFacing) = if (side != EnumFacing.UP) getNode else null

  override def getCurrentState = {
    if (isPrinting) util.EnumSet.of(api.util.StateAware.State.IsWorking)
    else if (canPrint) util.EnumSet.of(api.util.StateAware.State.CanWork)
    else util.EnumSet.noneOf(classOf[api.util.StateAware.State])
  }

  // ----------------------------------------------------------------------- //

  def canPrint = data.stateOff.nonEmpty && data.stateOff.size <= Settings.get.maxPrintComplexity && data.stateOn.size <= Settings.get.maxPrintComplexity

  def isPrinting = output.isDefined

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
    data.label = Option(args.optString(0, null)).map(_.take(24))
    if (data.label.fold(false)(_.isEmpty)) data.label = None
    isActive = false // Needs committing.
    null
  }

  @Callback(doc = """function():string -- Get the current label for the block being printed.""")
  def getLabel(context: Context, args: Arguments): Array[Object] = {
    result(data.label.orNull)
  }

  @Callback(doc = """function(value:string) -- Set a tooltip for the block being printed.""")
  def setTooltip(context: Context, args: Arguments): Array[Object] = {
    data.tooltip = Option(args.optString(0, null)).map(_.take(128))
    if (data.tooltip.fold(false)(_.isEmpty)) data.tooltip = None
    isActive = false // Needs committing.
    null
  }

  @Callback(doc = """function():string -- Get the current tooltip for the block being printed.""")
  def getTooltip(context: Context, args: Arguments): Array[Object] = {
    result(data.tooltip.orNull)
  }

  @Callback(doc = """function(value:number) -- Set what light level the printed block should have.""")
  def setLightLevel(context: Context, args: Arguments): Array[Object] = {
    data.lightLevel = args.checkInteger(0) max 0 min Settings.get.maxPrintLightLevel
    isActive = false // Needs committing.
    null
  }

  @Callback(doc = """function():number -- Get which light level the printed block should have.""")
  def getLightLevel(context: Context, args: Arguments): Array[Object] = {
    result(data.lightLevel)
  }

  @Callback(doc = """function(value:boolean or number) -- Set whether the printed block should emit redstone when in its active state.""")
  def setRedstoneEmitter(context: Context, args: Arguments): Array[Object] = {
    if (args.isBoolean(0)) data.redstoneLevel = if (args.checkBoolean(0)) 15 else 0
    else data.redstoneLevel = args.checkInteger(0) max 0 min 15
    isActive = false // Needs committing.
    null
  }

  @Callback(doc = """function():boolean, number -- Get whether the printed block should emit redstone when in its active state.""")
  def isRedstoneEmitter(context: Context, args: Arguments): Array[Object] = {
    result(data.emitRedstone, data.redstoneLevel)
  }

  @Callback(doc = """function(value:boolean) -- Set whether the printed block should automatically return to its off state.""")
  def setButtonMode(context: Context, args: Arguments): Array[Object] = {
    data.isButtonMode = args.checkBoolean(0)
    isActive = false // Needs committing.
    null
  }

  @Callback(doc = """function():boolean -- Get whether the printed block should automatically return to its off state.""")
  def isButtonMode(context: Context, args: Arguments): Array[Object] = {
    result(data.isButtonMode)
  }

  @Callback(doc = """function(collideOff:boolean, collideOn:boolean) -- Set whether the printed block should be collidable or not.""")
  def setCollidable(context: Context, args: Arguments): Array[Object] = {
    val (collideOff, collideOn) = (args.checkBoolean(0), args.checkBoolean(1))
    data.noclipOff = !collideOff
    data.noclipOn = !collideOn
    null
  }

  @Callback(doc = """function():boolean, boolean -- Get whether the printed block should be collidable or not.""")
  def isCollidable(context: Context, args: Arguments): Array[Object] = {
    result(!data.noclipOff, !data.noclipOn)
  }

  @Callback(doc = """function(minX:number, minY:number, minZ:number, maxX:number, maxY:number, maxZ:number, texture:string[, state:boolean=false][,tint:number]) -- Adds a shape to the printers configuration, optionally specifying whether it is for the off or on state.""")
  def addShape(context: Context, args: Arguments): Array[Object] = {
    if (data.stateOff.size > Settings.get.maxPrintComplexity || data.stateOn.size > Settings.get.maxPrintComplexity) {
      return result(Unit, "model too complex")
    }
    val minX = (args.checkInteger(0) max 0 min 16) / 16f
    val minY = (args.checkInteger(1) max 0 min 16) / 16f
    val minZ = (16 - (args.checkInteger(2) max 0 min 16)) / 16f
    val maxX = (args.checkInteger(3) max 0 min 16) / 16f
    val maxY = (args.checkInteger(4) max 0 min 16) / 16f
    val maxZ = (16 - (args.checkInteger(5) max 0 min 16)) / 16f
    val texture = args.checkString(6).take(64)
    val state = if (args.isBoolean(7)) args.checkBoolean(7) else false
    val tint = if (args.isInteger(7)) Option(args.checkInteger(7)) else if (args.isInteger(8)) Option(args.checkInteger(8)) else None

    if (minX == maxX) throw new IllegalArgumentException("empty block")
    if (minY == maxY) throw new IllegalArgumentException("empty block")
    if (minZ == maxZ) throw new IllegalArgumentException("empty block")

    val list = if (state) data.stateOn else data.stateOff
    list += new PrintData.Shape(new AxisAlignedBB(
      math.min(minX, maxX),
      math.min(minY, maxY),
      math.min(minZ, maxZ),
      math.max(maxX, minX),
      math.max(maxY, minY),
      math.max(maxZ, minZ)),
      texture, tint)
    isActive = false // Needs committing.

    getWorld.notifyBlockUpdate(getPos, getWorld.getBlockState(getPos), getWorld.getBlockState(getPos), 3)

    result(true)
  }

  @Callback(doc = """function():number -- Get the number of shapes in the current configuration.""")
  def getShapeCount(context: Context, args: Arguments): Array[Object] = result(data.stateOff.size, data.stateOn.size)

  @Callback(doc = """function():number -- Get the maximum allowed number of shapes.""")
  def getMaxShapeCount(context: Context, args: Arguments): Array[Object] = result(Settings.get.maxPrintComplexity)

  @Callback(doc = """function([count:number]):boolean -- Commit and begin printing the current configuration.""")
  def commit(context: Context, args: Arguments): Array[Object] = {
    if (!canPrint) {
      return result(Unit, "model invalid")
    }
    limit = (args.optDouble(0, 1) max 0 min Integer.MAX_VALUE).toInt
    isActive = limit > 0
    result(true)
  }

  @Callback(doc = """function(): string, number or boolean -- The current state of the printer, `busy' or `idle', followed by the progress or model validity, respectively.""")
  def status(context: Context, args: Arguments): Array[Object] = {
    if (isPrinting) result("busy", progress)
    else if (canPrint) result("idle", true)
    else result("idle", false)
  }

  // ----------------------------------------------------------------------- //

  override def updateEntity(): Unit = {
    super.updateEntity()

    if (isClient) {
      return
    }

    def canMergeOutput = {
      val presentStack = getStackInSlot(slotOutput)
      val outputStack = data.createItemStack()
      presentStack == null || (presentStack.isItemEqual(outputStack) && ItemStack.areItemStackTagsEqual(presentStack, outputStack))
    }

    if (isActive && output.isEmpty && canMergeOutput) {
      PrintData.computeCosts(data) match {
        case Some((materialRequired, inkRequired)) =>
          totalRequiredEnergy = Settings.get.printCost
          requiredEnergy = totalRequiredEnergy

          if (amountMaterial >= materialRequired && amountInk >= inkRequired) {
            amountMaterial -= materialRequired
            amountInk -= inkRequired
            limit -= 1
            output = Option(data.createItemStack())
            if (limit < 1) isActive = false
            ServerPacketSender.sendPrinting(this, printing = true)
          }
        case _ =>
          isActive = false
          data = new PrintData()
      }
    }

    if (output.isDefined) {
      val want = math.max(1, math.min(requiredEnergy, Settings.get.printerTickAmount))
      val have = want + (if (Settings.get.ignorePower) 0 else getNode.changeBuffer(-want))
      requiredEnergy -= have
      if (requiredEnergy <= 0) {
        val result = getStackInSlot(slotOutput)
        if (result == null) {
          setInventorySlotContents(slotOutput, output.get)
        }
        else if (result.getCount < result.getMaxStackSize && canMergeOutput /* Should never fail, but just in case... */ ) {
          result.grow(1)
          markDirty()
        }
        else {
          return
        }
        requiredEnergy = 0
        output = None
      }
      ServerPacketSender.sendPrinting(this, have > 0.5 && output.isDefined)
    }

    val inputValue = PrintData.materialValue(getStackInSlot(slotMaterial))
    if (inputValue > 0 && maxAmountMaterial - amountMaterial >= inputValue) {
      val material = decrStackSize(slotMaterial, 1)
      if (material != null) {
        amountMaterial += inputValue
      }
    }

    val inkValue = PrintData.inkValue(getStackInSlot(slotInk))
    if (inkValue > 0 && maxAmountInk - amountInk >= inkValue) {
      val material = decrStackSize(slotInk, 1)
      if (material != null) {
        amountInk += inkValue
        if (material.getItem.hasContainerItem(material)) {
          setInventorySlotContents(slotInk, material.getItem.getContainerItem(material))
        }
      }
    }
  }

  // ----------------------------------------------------------------------- //

  private final val AmountMaterialTag = Settings.namespace + "amountMaterial"
  private final val AmountInkTag = Settings.namespace + "amountInk"
  private final val DataTag = Settings.namespace + "data"
  private final val IsActiveTag = Settings.namespace + "active"
  private final val LimitTag = Settings.namespace + "limit"
  private final val OutputTag = Settings.namespace + "output"
  private final val TotalTag = Settings.namespace + "total"
  private final val RemainingTag = Settings.namespace + "remaining"

  override def readFromNBTForServer(nbt: NBTTagCompound) {
    super.readFromNBTForServer(nbt)
    amountMaterial = nbt.getInteger(AmountMaterialTag)
    amountInk = nbt.getInteger(AmountInkTag)
    data.load(nbt.getCompoundTag(DataTag))
    isActive = nbt.getBoolean(IsActiveTag)
    limit = nbt.getInteger(LimitTag)
    if (nbt.hasKey(OutputTag)) {
      output = Option(new ItemStack(nbt.getCompoundTag(OutputTag)))
    }
    else {
      output = None
    }
    totalRequiredEnergy = nbt.getDouble(TotalTag)
    requiredEnergy = nbt.getDouble(RemainingTag)
  }

  override def writeToNBTForServer(nbt: NBTTagCompound) {
    super.writeToNBTForServer(nbt)
    nbt.setInteger(AmountMaterialTag, amountMaterial)
    nbt.setInteger(AmountInkTag, amountInk)
    nbt.setNewCompoundTag(DataTag, data.save)
    nbt.setBoolean(IsActiveTag, isActive)
    nbt.setInteger(LimitTag, limit)
    output.foreach(stack => nbt.setNewCompoundTag(OutputTag, stack.writeToNBT))
    nbt.setDouble(TotalTag, totalRequiredEnergy)
    nbt.setDouble(RemainingTag, requiredEnergy)
  }

  @SideOnly(Side.CLIENT) override
  def readFromNBTForClient(nbt: NBTTagCompound) {
    super.readFromNBTForClient(nbt)
    data.load(nbt.getCompoundTag(DataTag))
    requiredEnergy = nbt.getDouble(RemainingTag)
  }

  override def writeToNBTForClient(nbt: NBTTagCompound) {
    super.writeToNBTForClient(nbt)
    nbt.setNewCompoundTag(DataTag, data.save)
    nbt.setDouble(RemainingTag, requiredEnergy)
  }

  // ----------------------------------------------------------------------- //

  override def getSizeInventory = 3

  override def isItemValidForSlot(slot: Int, stack: ItemStack) =
    if (slot == slotMaterial)
      PrintData.materialValue(stack) > 0
    else if (slot == slotInk)
      PrintData.inkValue(stack) > 0
    else false

  // ----------------------------------------------------------------------- //

  override def getSlotsForFace(side: EnumFacing): Array[Int] = Array(slotMaterial, slotInk, slotOutput)

  override def canExtractItem(slot: Int, stack: ItemStack, side: EnumFacing): Boolean = !isItemValidForSlot(slot, stack)

  override def canInsertItem(slot: Int, stack: ItemStack, side: EnumFacing): Boolean = slot != slotOutput
}

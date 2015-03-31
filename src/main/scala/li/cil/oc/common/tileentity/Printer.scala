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
import li.cil.oc.server.{PacketSender => ServerPacketSender}
import li.cil.oc.util.ExtendedAABB._
import li.cil.oc.util.ExtendedNBT._
import li.cil.oc.util.ItemUtils
import net.minecraft.inventory.ISidedInventory
import net.minecraft.item.ItemStack
import net.minecraft.nbt.NBTTagCompound
import net.minecraft.util.AxisAlignedBB
import net.minecraftforge.common.util.ForgeDirection

class Printer extends traits.Environment with traits.Inventory with traits.Rotatable with SidedEnvironment with traits.StateAware with ISidedInventory {
  val node = api.Network.newNode(this, Visibility.Network).
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

  val materialPerItem = 2000
  val inkPerCartridge = 50000

  val slotMaterial = 0
  val slotInk = 1
  val slotOutput = 2

  // ----------------------------------------------------------------------- //

  @SideOnly(Side.CLIENT)
  override def canConnect(side: ForgeDirection) = side != ForgeDirection.UP

  override def sidedNode(side: ForgeDirection) = if (side != ForgeDirection.UP) node else null

  override def currentState = {
    if (isPrinting) util.EnumSet.of(traits.State.IsWorking)
    else if (canPrint) util.EnumSet.of(traits.State.CanWork)
    else util.EnumSet.noneOf(classOf[traits.State])
  }

  // ----------------------------------------------------------------------- //

  def canPrint = data.stateOff.size > 0 && data.stateOff.size + data.stateOn.size <= Settings.get.maxPrintComplexity

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

  @Callback(doc = """function(value:boolean) -- Set whether the printed block should emit redstone when in its active state.""")
  def setRedstoneEmitter(context: Context, args: Arguments): Array[Object] = {
    data.emitRedstone = args.checkBoolean(0)
    isActive = false // Needs committing.
    null
  }

  @Callback(doc = """function():boolean -- Get whether the printed block should emit redstone when in its active state.""")
  def isRedstoneEmitter(context: Context, args: Arguments): Array[Object] = {
    result(data.emitRedstone)
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

  @Callback(doc = """function(minX:number, minY:number, minZ:number, maxX:number, maxY:number, maxZ:number, texture:string[, state:boolean=false][,tint:number]) -- Adds a shape to the printers configuration, optionally specifying whether it is for the off or on state.""")
  def addShape(context: Context, args: Arguments): Array[Object] = {
    if (data.stateOff.size + data.stateOn.size >= Settings.get.maxPrintComplexity) {
      return result(null, "model too complex")
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
    list += new PrintData.Shape(AxisAlignedBB.getBoundingBox(
      math.min(minX, maxX),
      math.min(minY, maxY),
      math.min(minZ, maxZ),
      math.max(maxX, minX),
      math.max(maxY, minY),
      math.max(maxZ, minZ)),
      texture, tint)
    isActive = false // Needs committing.

    world.markBlockForUpdate(x, y, z)

    result(true)
  }

  @Callback(doc = """function():number -- Get the number of shapes in the current configuration.""")
  def getShapeCount(context: Context, args: Arguments): Array[Object] = result(data.stateOff.size + data.stateOn.size)

  @Callback(doc = """function():number -- Get the maximum allowed number of shapes.""")
  def getMaxShapeCount(context: Context, args: Arguments): Array[Object] = result(Settings.get.maxPrintComplexity)

  @Callback(doc = """function([count:number]):boolean -- Commit and begin printing the current configuration.""")
  def commit(context: Context, args: Arguments): Array[Object] = {
    if (!canPrint) {
      return result(null, "model invalid")
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

  def computeCosts(data: PrintData) = {
    val totalVolume = data.stateOn.foldLeft(0)((acc, shape) => acc + shape.bounds.volume) + data.stateOff.foldLeft(0)((acc, shape) => acc + shape.bounds.volume)
    val totalSurface = data.stateOn.foldLeft(0)((acc, shape) => acc + shape.bounds.surface) + data.stateOff.foldLeft(0)((acc, shape) => acc + shape.bounds.surface)

    if (totalVolume > 0) {
      val materialRequired = (totalVolume / 2) max 1
      val inkRequired = (totalSurface / 6) max 1

      Option((materialRequired, inkRequired))
    }
    else None
  }

  def materialValue(stack: ItemStack) = {
    if (api.Items.get(stack) == api.Items.get("chamelium"))
      materialPerItem
    else if (api.Items.get(stack) == api.Items.get("print")) {
      val data = new PrintData(stack)
      computeCosts(data) match {
        case Some((materialRequired, inkRequired)) => (materialRequired * Settings.get.printRecycleRate).toInt
        case _ => 0
      }
    }
    else 0
  }

  // ----------------------------------------------------------------------- //

  override def canUpdate = isServer

  override def updateEntity() {
    super.updateEntity()

    def canMergeOutput = {
      val presentStack = getStackInSlot(slotOutput)
      val outputStack = data.createItemStack()
      presentStack == null || (presentStack.isItemEqual(outputStack) && ItemStack.areItemStackTagsEqual(presentStack, outputStack))
    }

    if (isActive && output.isEmpty && canMergeOutput) {
      computeCosts(data) match {
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
      val have = want + (if (Settings.get.ignorePower) 0 else node.changeBuffer(-want))
      requiredEnergy -= have
      if (requiredEnergy <= 0) {
        val result = getStackInSlot(slotOutput)
        if (result == null) {
          setInventorySlotContents(slotOutput, output.get)
        }
        else if (result.stackSize < result.getMaxStackSize && canMergeOutput /* Should never fail, but just in case... */ ) {
          result.stackSize += 1
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

    val inputValue = materialValue(getStackInSlot(slotMaterial))
    if (inputValue > 0 && maxAmountMaterial - amountMaterial >= inputValue) {
      val material = decrStackSize(slotMaterial, 1)
      if (material != null) {
        amountMaterial += inputValue
      }
    }

    if (maxAmountInk - amountInk >= inkPerCartridge) {
      if (api.Items.get(getStackInSlot(slotInk)) == api.Items.get("inkCartridge")) {
        setInventorySlotContents(slotInk, api.Items.get("inkCartridgeEmpty").createItemStack(1))
        amountInk += inkPerCartridge
      }
    }
  }

  override def readFromNBTForServer(nbt: NBTTagCompound) {
    super.readFromNBTForServer(nbt)
    amountMaterial = nbt.getInteger(Settings.namespace + "amountMaterial")
    amountInk = nbt.getInteger(Settings.namespace + "amountInk")
    data.load(nbt.getCompoundTag(Settings.namespace + "data"))
    isActive = nbt.getBoolean(Settings.namespace + "active")
    limit = nbt.getInteger(Settings.namespace + "limit")
    if (nbt.hasKey(Settings.namespace + "output")) {
      output = Option(ItemUtils.loadStack(nbt.getCompoundTag(Settings.namespace + "output")))
    }
    totalRequiredEnergy = nbt.getDouble(Settings.namespace + "total")
    requiredEnergy = nbt.getDouble(Settings.namespace + "remaining")
  }

  override def writeToNBTForServer(nbt: NBTTagCompound) {
    super.writeToNBTForServer(nbt)
    nbt.setInteger(Settings.namespace + "amountMaterial", amountMaterial)
    nbt.setInteger(Settings.namespace + "amountInk", amountInk)
    nbt.setNewCompoundTag(Settings.namespace + "data", data.save)
    nbt.setBoolean(Settings.namespace + "active", isActive)
    nbt.setInteger(Settings.namespace + "limit", limit)
    output.foreach(stack => nbt.setNewCompoundTag(Settings.namespace + "output", stack.writeToNBT))
    nbt.setDouble(Settings.namespace + "total", totalRequiredEnergy)
    nbt.setDouble(Settings.namespace + "remaining", requiredEnergy)
  }

  @SideOnly(Side.CLIENT) override
  def readFromNBTForClient(nbt: NBTTagCompound) {
    super.readFromNBTForClient(nbt)
    data.load(nbt.getCompoundTag(Settings.namespace + "data"))
    requiredEnergy = nbt.getDouble("remaining")
  }

  override def writeToNBTForClient(nbt: NBTTagCompound) {
    super.writeToNBTForClient(nbt)
    nbt.setNewCompoundTag(Settings.namespace + "data", data.save)
    nbt.setDouble("remaining", requiredEnergy)
  }

  // ----------------------------------------------------------------------- //

  override def getSizeInventory = 3

  override def getInventoryStackLimit = 64

  override def isItemValidForSlot(slot: Int, stack: ItemStack) =
    if (slot == slotMaterial)
      materialValue(stack) > 0
    else if (slot == slotInk)
      api.Items.get(stack) == api.Items.get("inkCartridge")
    else false

  // ----------------------------------------------------------------------- //

  override def getAccessibleSlotsFromSide(side: Int): Array[Int] = Array(slotMaterial, slotInk, slotOutput)

  override def canExtractItem(slot: Int, stack: ItemStack, side: Int): Boolean = !isItemValidForSlot(slot, stack)

  override def canInsertItem(slot: Int, stack: ItemStack, side: Int): Boolean = slot != slotOutput
}

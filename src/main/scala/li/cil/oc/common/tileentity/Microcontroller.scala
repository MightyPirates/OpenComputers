package li.cil.oc.common.tileentity

import java.util

import cpw.mods.fml.relauncher.Side
import cpw.mods.fml.relauncher.SideOnly
import li.cil.oc.Constants
import li.cil.oc.api.driver.DeviceInfo.DeviceAttribute
import li.cil.oc.api.driver.DeviceInfo.DeviceClass
import li.cil.oc.Settings
import li.cil.oc.api
import li.cil.oc.api.driver.DeviceInfo
import li.cil.oc.api.internal
import li.cil.oc.api.machine.Arguments
import li.cil.oc.api.machine.Callback
import li.cil.oc.api.machine.Context
import li.cil.oc.api.network._
import li.cil.oc.common.Tier
import li.cil.oc.common.item.data.MicrocontrollerData
import li.cil.oc.util.ExtendedArguments._
import li.cil.oc.util.ExtendedNBT._
import net.minecraft.entity.player.EntityPlayer
import net.minecraft.inventory.ISidedInventory
import net.minecraft.item.ItemStack
import net.minecraft.nbt.NBTTagCompound
import net.minecraftforge.common.util.Constants.NBT
import net.minecraftforge.common.util.ForgeDirection

import scala.collection.convert.WrapAsJava._

class Microcontroller extends traits.PowerAcceptor with traits.Hub with traits.Computer with ISidedInventory with internal.Microcontroller with DeviceInfo {
  val info = new MicrocontrollerData()

  override def node = null

  val outputSides = Array.fill(6)(true)

  val snooperNode = api.Network.newNode(this, Visibility.Network).
    withComponent("microcontroller").
    withConnector(Settings.get.bufferMicrocontroller).
    create()

  val componentNodes = Array.fill(6)(api.Network.newNode(this, Visibility.Network).
    withComponent("microcontroller").
    create())

  if (machine != null) {
    machine.node.asInstanceOf[Connector].setLocalBufferSize(0)
    machine.setCostPerTick(Settings.get.microcontrollerCost)
  }

  override def tier = info.tier

  override protected def runSound = None // Microcontrollers are silent.

  private final lazy val deviceInfo = Map(
    DeviceAttribute.Class -> DeviceClass.System,
    DeviceAttribute.Description -> "Microcontroller",
    DeviceAttribute.Vendor -> Constants.DeviceInfo.DefaultVendor,
    DeviceAttribute.Product -> "Cubicle",
    DeviceAttribute.Capacity -> getSizeInventory.toString
  )

  override def getDeviceInfo: util.Map[String, String] = deviceInfo

  // ----------------------------------------------------------------------- //

  @SideOnly(Side.CLIENT)
  override def canConnect(side: ForgeDirection) = side != facing

  override def sidedNode(side: ForgeDirection): Node = if (side != facing) super.sidedNode(side) else null

  @SideOnly(Side.CLIENT)
  override protected def hasConnector(side: ForgeDirection) = side != facing

  override protected def connector(side: ForgeDirection) = Option(if (side != facing) snooperNode else null)

  override def energyThroughput = Settings.get.caseRate(Tier.One)

  override def getWorld = world

  // ----------------------------------------------------------------------- //

  override def onAnalyze(player: EntityPlayer, side: Int, hitX: Float, hitY: Float, hitZ: Float): Array[Node] = {
    super.onAnalyze(player, side, hitX, hitY, hitZ)
    if (ForgeDirection.getOrientation(side) != facing)
      Array(componentNodes(side))
    else
      Array(machine.node)
  }

  // ----------------------------------------------------------------------- //

  override def internalComponents(): java.lang.Iterable[ItemStack] = asJavaIterable(info.components)

  override def componentSlot(address: String) = components.indexWhere(_.exists(env => env.node != null && env.node.address == address))

  // ----------------------------------------------------------------------- //

  @Callback(doc = """function():boolean -- Starts the microcontroller. Returns true if the state changed.""")
  def start(context: Context, args: Arguments): Array[AnyRef] =
    result(!machine.isPaused && machine.start())

  @Callback(doc = """function():boolean -- Stops the microcontroller. Returns true if the state changed.""")
  def stop(context: Context, args: Arguments): Array[AnyRef] =
    result(machine.stop())

  @Callback(direct = true, doc = """function():boolean -- Returns whether the microcontroller is running.""")
  def isRunning(context: Context, args: Arguments): Array[AnyRef] =
    result(machine.isRunning)

  @Callback(direct = true, doc = """function():string -- Returns the reason the microcontroller crashed, if applicable.""")
  def lastError(context: Context, args: Arguments): Array[AnyRef] =
    result(machine.lastError)

  @Callback(direct = true, doc = """function(side:number):boolean -- Get whether network messages are sent via the specified side.""")
  def isSideOpen(context: Context, args: Arguments): Array[AnyRef] = {
    val side = args.checkSideExcept(0, facing)
    result(outputSides(side.ordinal()))
  }

  @Callback(doc = """function(side:number, open:boolean):boolean -- Set whether network messages are sent via the specified side.""")
  def setSideOpen(context: Context, args: Arguments): Array[AnyRef] = {
    val side = args.checkSideExcept(0, facing)
    val oldValue = outputSides(side.ordinal())
    outputSides(side.ordinal()) = args.checkBoolean(1)
    result(oldValue)
  }

  // ----------------------------------------------------------------------- //

  override def canUpdate = isServer

  override def updateEntity() {
    super.updateEntity()

    // Pump energy into the internal network.
    if (world.getTotalWorldTime % Settings.get.tickFrequency == 0) {
      for (side <- ForgeDirection.VALID_DIRECTIONS if side != facing) {
        sidedNode(side) match {
          case connector: Connector =>
            val demand = snooperNode.globalBufferSize - snooperNode.globalBuffer
            val available = demand + connector.changeBuffer(-demand)
            snooperNode.changeBuffer(available)
          case _ =>
        }
      }
    }
  }

  // ----------------------------------------------------------------------- //

  override protected def connectItemNode(node: Node) {
    if (machine != null && machine.node != null && node != null) {
      api.Network.joinNewNetwork(machine.node)
      machine.node.connect(node)
    }
  }

  // ----------------------------------------------------------------------- //

  override protected def createNode(plug: Plug): Node = api.Network.newNode(plug, Visibility.Network).
    withConnector().
    create()

  override protected def onPlugConnect(plug: Plug, node: Node): Unit = {
    super.onPlugConnect(plug, node)
    if (node == plug.node) {
      api.Network.joinNewNetwork(machine.node)
      machine.node.connect(snooperNode)
      connectComponents()
    }
    if (plug.isPrimary)
      plug.node.connect(componentNodes(plug.side.ordinal()))
    else
      componentNodes(plug.side.ordinal).remove()
  }

  override protected def onPlugDisconnect(plug: Plug, node: Node) {
    super.onPlugDisconnect(plug, node)
    if (plug.isPrimary && node != plug.node)
      plug.node.connect(componentNodes(plug.side.ordinal()))
    else
      componentNodes(plug.side.ordinal).remove()
    if (node == plug.node)
      disconnectComponents()
  }

  override protected def onPlugMessage(plug: Plug, message: Message): Unit = {
    if (message.name == "network.message" && message.source.network != snooperNode.network) {
      snooperNode.sendToReachable(message.name, message.data: _*)
    }
  }

  override def onMessage(message: Message): Unit = {
    if (message.name == "network.message" && message.source.network == snooperNode.network) {
      for (side <- ForgeDirection.VALID_DIRECTIONS if outputSides(side.ordinal) && side != facing) {
        sidedNode(side).sendToReachable(message.name, message.data: _*)
      }
    }
  }

  // ----------------------------------------------------------------------- //

  override def readFromNBTForServer(nbt: NBTTagCompound) {
    // Load info before inventory and such, to avoid initializing components
    // to empty inventory.
    info.load(nbt.getCompoundTag(Settings.namespace + "info"))
    nbt.getBooleanArray(Settings.namespace + "outputs")
    nbt.getTagList(Settings.namespace + "componentNodes", NBT.TAG_COMPOUND).toArray[NBTTagCompound].
      zipWithIndex.foreach {
      case (tag, index) => componentNodes(index).load(tag)
    }
    snooperNode.load(nbt.getCompoundTag(Settings.namespace + "snooper"))
    super.readFromNBTForServer(nbt)
    api.Network.joinNewNetwork(machine.node)
    machine.node.connect(snooperNode)
  }

  override def writeToNBTForServer(nbt: NBTTagCompound) {
    super.writeToNBTForServer(nbt)
    nbt.setNewCompoundTag(Settings.namespace + "info", info.save)
    nbt.setBooleanArray(Settings.namespace + "outputs", outputSides)
    nbt.setNewTagList(Settings.namespace + "componentNodes", componentNodes.map {
      case node: Node =>
        val tag = new NBTTagCompound()
        node.save(tag)
        tag
      case _ => new NBTTagCompound()
    })
    nbt.setNewCompoundTag(Settings.namespace + "snooper", snooperNode.save)
  }

  // ----------------------------------------------------------------------- //

  @SideOnly(Side.CLIENT) override
  def readFromNBTForClient(nbt: NBTTagCompound) {
    info.load(nbt.getCompoundTag("info"))
    super.readFromNBTForClient(nbt)
  }

  override def writeToNBTForClient(nbt: NBTTagCompound) {
    super.writeToNBTForClient(nbt)
    nbt.setNewCompoundTag("info", info.save)
  }

  override def items = info.components.map(Option(_))

  override def updateItems(slot: Int, stack: ItemStack): Unit = info.components(slot) = stack

  override def getSizeInventory = info.components.length

  override def isItemValidForSlot(slot: Int, stack: ItemStack) = false

  // Nope.
  override def setInventorySlotContents(slot: Int, stack: ItemStack) {}

  // Nope.
  override def decrStackSize(slot: Int, amount: Int) = null

  // Nope.
  override def getStackInSlotOnClosing(slot: Int) = null

  // Nope.
  override def getAccessibleSlotsFromSide(side: Int): Array[Int] = Array()

  override def canExtractItem(slot: Int, stack: ItemStack, side: Int) = false

  override def canInsertItem(slot: Int, stack: ItemStack, side: Int) = false

  // For hotswapping EEPROMs.
  def changeEEPROM(newEeprom: ItemStack) = {
    val oldEepromIndex = info.components.indexWhere(api.Items.get(_) == api.Items.get(Constants.ItemName.EEPROM))
    if (oldEepromIndex >= 0) {
      val oldEeprom = info.components(oldEepromIndex)
      super.setInventorySlotContents(oldEepromIndex, newEeprom)
      Some(oldEeprom)
    }
    else {
      assert(info.components(getSizeInventory - 1) == null)
      super.setInventorySlotContents(getSizeInventory - 1, newEeprom)
      None
    }
  }
}

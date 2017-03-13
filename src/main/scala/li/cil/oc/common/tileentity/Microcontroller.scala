package li.cil.oc.common.tileentity

import java.util

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
import net.minecraft.item.ItemStack
import net.minecraft.nbt.NBTTagCompound
import net.minecraft.util.EnumFacing
import net.minecraftforge.common.util.Constants.NBT
import net.minecraftforge.fml.relauncher.Side
import net.minecraftforge.fml.relauncher.SideOnly

import scala.collection.convert.WrapAsJava._

class Microcontroller extends traits.PowerAcceptor with traits.NetworkBridge with traits.Computer with internal.Microcontroller with DeviceInfo {
  val info = new MicrocontrollerData()

  override def getNode = null

  val outputSides = Array.fill(6)(true)

  val snooperNode = api.Network.newNode(this, Visibility.NETWORK).
    withComponent("microcontroller").
    withConnector(Settings.get.bufferMicrocontroller).
    create()

  val componentNodes = Array.fill(6)(api.Network.newNode(this, Visibility.NETWORK).
    withComponent("microcontroller").
    create())

  if (machine != null) {
    machine.node.asInstanceOf[EnergyNode].setEnergyCapacity(0)
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
  override def canConnect(side: EnumFacing) = side != getFacing

  override def sidedNode(side: EnumFacing): Node = if (side != getFacing) super.sidedNode(side) else null

  @SideOnly(Side.CLIENT)
  override protected def hasConnector(side: EnumFacing) = side != getFacing

  override protected def connector(side: EnumFacing) = Option(if (side != getFacing) snooperNode else null)

  override def energyThroughput = Settings.get.caseRate(Tier.One)

  // ----------------------------------------------------------------------- //

  override def onAnalyze(player: EntityPlayer, side: EnumFacing, hitX: Float, hitY: Float, hitZ: Float): Array[Node] = {
    super.onAnalyze(player, side, hitX, hitY, hitZ)
    if (side != getFacing)
      Array(componentNodes(side.getIndex))
    else
      Array(machine.node)
  }

  // ----------------------------------------------------------------------- //

  override def internalComponents(): java.lang.Iterable[ItemStack] = asJavaIterable(info.components)

  override def componentSlot(address: String) = components.indexWhere(_.exists(env => env.getNode != null && env.getNode.getAddress == address))

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
    val side = args.checkSideExcept(0, getFacing)
    result(outputSides(side.ordinal()))
  }

  @Callback(doc = """function(side:number, open:boolean):boolean -- Set whether network messages are sent via the specified side.""")
  def setSideOpen(context: Context, args: Arguments): Array[AnyRef] = {
    val side = args.checkSideExcept(0, getFacing)
    val oldValue = outputSides(side.ordinal())
    outputSides(side.ordinal()) = args.checkBoolean(1)
    result(oldValue)
  }

  // ----------------------------------------------------------------------- //

  override def updateEntity() {
    super.updateEntity()

    // Pump energy into the internal network.
    if (isServer && getWorld.getTotalWorldTime % Settings.get.tickFrequency == 0) {
      for (side <- EnumFacing.values if side != getFacing) {
        sidedNode(side) match {
          case connector: EnergyNode =>
            val demand = snooperNode.getGlobalBufferSize - snooperNode.getGlobalBuffer
            val available = demand + connector.changeEnergy(-demand)
            snooperNode.changeEnergy(available)
          case _ =>
        }
      }
    }
  }

  // ----------------------------------------------------------------------- //

  override protected def connectItemNode(node: Node) {
    if (machine.node != null && node != null) {
      api.Network.joinNewNetwork(machine.node)
      machine.node.connect(node)
    }
  }

  // ----------------------------------------------------------------------- //

  override protected def createNode(plug: Plug): Node = api.Network.newNode(plug, Visibility.NETWORK).
    withConnector().
    create()

  override protected def onPlugConnect(plug: Plug, node: Node): Unit = {
    super.onPlugConnect(plug, node)
    if (node == plug.getNode) {
      api.Network.joinNewNetwork(machine.node)
      machine.node.connect(snooperNode)
    }
    if (plug.isPrimary)
      plug.getNode.connect(componentNodes(plug.side.ordinal()))
    else
      componentNodes(plug.side.ordinal).remove()
    connectComponents()
  }

  override protected def onPlugDisconnect(plug: Plug, node: Node) {
    super.onPlugDisconnect(plug, node)
    if (plug.isPrimary && node != plug.getNode)
      plug.getNode.connect(componentNodes(plug.side.ordinal()))
    else
      componentNodes(plug.side.ordinal).remove()
  }

  override protected def onPlugMessage(plug: Plug, message: Message): Unit = {
    if (message.getName == "network.message" && message.getSource.getNetwork != snooperNode.getNetwork) {
      snooperNode.sendToReachable(message.getName, message.getData: _*)
    }
  }

  override def onMessage(message: Message): Unit = {
    if (message.getName == "network.message" && message.getSource.getNetwork == snooperNode.getNetwork) {
      for (side <- EnumFacing.values if outputSides(side.ordinal) && side != getFacing) {
        sidedNode(side).sendToReachable(message.getName, message.getData: _*)
      }
    }
  }

  // ----------------------------------------------------------------------- //

  private final val InfoTag = Settings.namespace + "info"
  private final val OutputsTag = Settings.namespace + "outputs"
  private final val ComponentNodesTag = Settings.namespace + "componentNodes"
  private final val SnooperTag = Settings.namespace + "snooper"

  override def readFromNBTForServer(nbt: NBTTagCompound) {
    // Load info before inventory and such, to avoid initializing components
    // to empty inventory.
    info.load(nbt.getCompoundTag(InfoTag))
    nbt.getBooleanArray(OutputsTag)
    nbt.getTagList(ComponentNodesTag, NBT.TAG_COMPOUND).toArray[NBTTagCompound].
      zipWithIndex.foreach {
      case (tag, index) => componentNodes(index).load(tag)
    }
    snooperNode.load(nbt.getCompoundTag(SnooperTag))
    super.readFromNBTForServer(nbt)
    api.Network.joinNewNetwork(machine.node)
    machine.node.connect(snooperNode)
  }

  override def writeToNBTForServer(nbt: NBTTagCompound) {
    super.writeToNBTForServer(nbt)
    nbt.setNewCompoundTag(InfoTag, info.save)
    nbt.setBooleanArray(OutputsTag, outputSides)
    nbt.setNewTagList(ComponentNodesTag, componentNodes.map {
      case node: Node =>
        val tag = new NBTTagCompound()
        node.save(tag)
        tag
      case _ => new NBTTagCompound()
    })
    nbt.setNewCompoundTag(SnooperTag, snooperNode.save)
  }

  @SideOnly(Side.CLIENT) override
  def readFromNBTForClient(nbt: NBTTagCompound) {
    info.load(nbt.getCompoundTag(InfoTag))
    super.readFromNBTForClient(nbt)
  }

  override def writeToNBTForClient(nbt: NBTTagCompound) {
    super.writeToNBTForClient(nbt)
    nbt.setNewCompoundTag(InfoTag, info.save)
  }

  // ----------------------------------------------------------------------- //

  override def items = info.components.map(Option(_))

  override def updateItems(slot: Int, stack: ItemStack): Unit = info.components(slot) = stack

  override def getSizeInventory = info.components.length

  override def isItemValidForSlot(slot: Int, stack: ItemStack) = false

  // Nope.
  override def setInventorySlotContents(slot: Int, stack: ItemStack) {}

  // Nope.
  override def decrStackSize(slot: Int, amount: Int) = null

  // Nope.
  override def removeStackFromSlot(slot: Int) = null

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

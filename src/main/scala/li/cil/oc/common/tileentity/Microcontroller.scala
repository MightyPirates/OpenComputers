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
import li.cil.oc.util.StackOption
import li.cil.oc.util.StackOption._
import net.minecraft.entity.player.PlayerEntity
import net.minecraft.inventory.ISidedInventory
import net.minecraft.item.ItemStack
import net.minecraft.nbt.CompoundNBT
import net.minecraft.tileentity.TileEntity
import net.minecraft.util.Direction
import net.minecraftforge.common.util.Constants.NBT
import net.minecraftforge.api.distmarker.Dist
import net.minecraftforge.api.distmarker.OnlyIn

import scala.collection.convert.WrapAsJava._

class Microcontroller extends TileEntity(null) with traits.PowerAcceptor with traits.Hub with traits.Computer with ISidedInventory with internal.Microcontroller with DeviceInfo {
  val info = new MicrocontrollerData()

  override def node = null

  val outputSides: Array[Boolean] = Array.fill(6)(true)

  val snooperNode: ComponentConnector = api.Network.newNode(this, Visibility.Network).
    withComponent("microcontroller").
    withConnector(Settings.get.bufferMicrocontroller).
    create()

  val componentNodes: Array[Component] = Array.fill(6)(api.Network.newNode(this, Visibility.Network).
    withComponent("microcontroller").
    create())

  if (machine != null) {
    machine.node.asInstanceOf[Connector].setLocalBufferSize(0)
    machine.setCostPerTick(Settings.get.microcontrollerCost)
  }

  override def tier: Int = info.tier

  override protected def runSound = None // Microcontrollers are silent.

  private final lazy val deviceInfo = Map(
    DeviceAttribute.Class -> DeviceClass.System,
    DeviceAttribute.Description -> "Microcontroller",
    DeviceAttribute.Vendor -> Constants.DeviceInfo.DefaultVendor,
    DeviceAttribute.Product -> "Cubicle",
    DeviceAttribute.Capacity -> getContainerSize.toString
  )

  override def getDeviceInfo: util.Map[String, String] = deviceInfo

  // ----------------------------------------------------------------------- //

  @OnlyIn(Dist.CLIENT)
  override def canConnect(side: Direction): Boolean = side != facing

  override def sidedNode(side: Direction): Node = if (side != facing) super.sidedNode(side) else null

  @OnlyIn(Dist.CLIENT)
  override protected def hasConnector(side: Direction): Boolean = side != facing

  override protected def connector(side: Direction) = Option(if (side != facing) snooperNode else null)

  override def energyThroughput: Double = Settings.get.caseRate(Tier.One)

  // ----------------------------------------------------------------------- //

  override def onAnalyze(player: PlayerEntity, side: Direction, hitX: Float, hitY: Float, hitZ: Float): Array[Node] = {
    super.onAnalyze(player, side, hitX, hitY, hitZ)
    if (side != facing)
      Array(componentNodes(side.get3DDataValue))
    else
      Array(machine.node)
  }

  // ----------------------------------------------------------------------- //

  override def internalComponents(): java.lang.Iterable[ItemStack] = asJavaIterable(info.components)

  override def componentSlot(address: String): Int = components.indexWhere(_.exists(env => env.node != null && env.node.address == address))

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

  override def updateEntity() {
    super.updateEntity()

    // Pump energy into the internal network.
    if (isServer && getLevel.getGameTime % Settings.get.tickFrequency == 0) {
      for (side <- Direction.values if side != facing) {
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
      for (side <- Direction.values if outputSides(side.ordinal) && side != facing) {
        sidedNode(side).sendToReachable(message.name, message.data: _*)
      }
    }
  }

  // ----------------------------------------------------------------------- //

  private final val InfoTag = Settings.namespace + "info"
  private final val OutputsTag = Settings.namespace + "outputs"
  private final val ComponentNodesTag = Settings.namespace + "componentNodes"
  private final val SnooperTag = Settings.namespace + "snooper"

  override def loadForServer(nbt: CompoundNBT) {
    // Load info before inventory and such, to avoid initializing components
    // to empty inventory.
    info.loadData(nbt.getCompound(InfoTag))
    nbt.getBooleanArray(OutputsTag)
    nbt.getList(ComponentNodesTag, NBT.TAG_COMPOUND).toTagArray[CompoundNBT].
      zipWithIndex.foreach {
      case (tag, index) => componentNodes(index).loadData(tag)
    }
    snooperNode.loadData(nbt.getCompound(SnooperTag))
    super.loadForServer(nbt)
    api.Network.joinNewNetwork(machine.node)
    machine.node.connect(snooperNode)
  }

  override def saveForServer(nbt: CompoundNBT) {
    super.saveForServer(nbt)
    nbt.setNewCompoundTag(InfoTag, info.saveData)
    nbt.setBooleanArray(OutputsTag, outputSides)
    nbt.setNewTagList(ComponentNodesTag, componentNodes.map {
      case node: Node =>
        val tag = new CompoundNBT()
        node.saveData(tag)
        tag
      case _ => new CompoundNBT()
    })
    nbt.setNewCompoundTag(SnooperTag, snooperNode.saveData)
  }

  @OnlyIn(Dist.CLIENT) override
  def loadForClient(nbt: CompoundNBT) {
    info.loadData(nbt.getCompound(InfoTag))
    super.loadForClient(nbt)
  }

  override def saveForClient(nbt: CompoundNBT) {
    super.saveForClient(nbt)
    nbt.setNewCompoundTag(InfoTag, info.saveData)
  }

  // ----------------------------------------------------------------------- //

  override def items: Array[ItemStack] = info.components

  override def updateItems(slot: Int, stack: ItemStack): Unit = info.components(slot) = stack

  override def getContainerSize: Int = info.components.length

  override def canPlaceItem(slot: Int, stack: ItemStack) = false

  // Nope.
  override def setItem(slot: Int, stack: ItemStack) {}

  // Nope.
  override def removeItem(slot: Int, amount: Int) = ItemStack.EMPTY

  // Nope.
  override def removeItemNoUpdate(slot: Int) = ItemStack.EMPTY

  // Nope.
  override def canTakeItemThroughFace(slot: Int, stack: ItemStack, side: Direction) = false

  override def canPlaceItemThroughFace(slot: Int, stack: ItemStack, side: Direction) = false

  override def getSlotsForFace(side: Direction): Array[Int] = Array()

  // For hotswapping EEPROMs.
  def changeEEPROM(newEeprom: ItemStack): StackOption = {
    val oldEepromIndex = info.components.indexWhere(api.Items.get(_) == api.Items.get(Constants.ItemName.EEPROM))
    if (oldEepromIndex >= 0) {
      val oldEeprom = info.components(oldEepromIndex)
      super.setItem(oldEepromIndex, newEeprom)
      SomeStack(oldEeprom)
    }
    else {
      assert(info.components(getContainerSize - 1).isEmpty)
      super.setItem(getContainerSize - 1, newEeprom)
      EmptyStack
    }
  }
}

package li.cil.oc.common.tileentity

import cpw.mods.fml.relauncher.{SideOnly, Side}
import li.cil.oc.Config
import li.cil.oc.api
import li.cil.oc.api.driver.Slot
import li.cil.oc.api.network._
import li.cil.oc.client.{PacketSender => ClientPacketSender}
import li.cil.oc.common
import li.cil.oc.server.component
import li.cil.oc.server.component.GraphicsCard
import li.cil.oc.server.component.robot.Player
import li.cil.oc.server.driver.Registry
import li.cil.oc.util.ExtendedNBT._
import net.minecraft.client.Minecraft
import net.minecraft.item.ItemStack
import net.minecraft.nbt.NBTTagCompound
import net.minecraftforge.common.ForgeDirection
import scala.Some

class Robot(isRemote: Boolean) extends Computer(isRemote) with Buffer with PowerInformation {
  def this() = this(false)

  // ----------------------------------------------------------------------- //

  override val node = api.Network.newNode(this, Visibility.Network).
    withComponent("computer", Visibility.Neighbors).
    create()

  override val buffer = new common.component.Buffer(this) {
    override def maxResolution = (48, 14)
  }
  override val computer = if (isRemote) null else new component.Robot(this)
  val (battery, distributor, gpu, keyboard) = if (isServer) {
    val battery = api.Network.newNode(this, Visibility.Network).withConnector(10000).create()
    val distributor = new component.PowerDistributor(this)
    val gpu = new GraphicsCard.Tier1 {
      override val maxResolution = (48, 14)
    }
    val keyboard = new component.Keyboard(this)
    (battery, distributor, gpu, keyboard)
  }
  else (null, null, null, null)

  private lazy val player_ = new Player(this)

  def player(facing: ForgeDirection = facing, side: ForgeDirection = facing) = {
    assert(isServer)
    player_.updatePositionAndRotation(facing, side)
    player_
  }

  var selectedSlot = 0

  // ----------------------------------------------------------------------- //

  def tier = 0

  //def bounds =

  override def installedMemory = 64 * 1024

  def actualSlot(n: Int) = n + 3

  // ----------------------------------------------------------------------- //

  @LuaCallback("start")
  def start(context: Context, args: Arguments): Array[AnyRef] =
    Array(Boolean.box(computer.start()))

  @LuaCallback("stop")
  def stop(context: Context, args: Arguments): Array[AnyRef] =
    Array(Boolean.box(computer.stop()))

  @LuaCallback(value = "isRunning", direct = true)
  def isRunning(context: Context, args: Arguments): Array[AnyRef] =
    Array(Boolean.box(computer.isRunning))

  @LuaCallback(value = "isRobot", direct = true)
  def isRobot(context: Context, args: Arguments): Array[AnyRef] =
    Array(java.lang.Boolean.TRUE)

  // ----------------------------------------------------------------------- //

  override def updateEntity() {
    super.updateEntity()
    if (isServer) {
      distributor.changeBuffer(10) // just for testing
      distributor.update()
      gpu.update()
    }
  }

  override def validate() {
    super.validate()
    if (isServer) {
      items(0) match {
        case Some(item) => player_.getAttributeMap.applyAttributeModifiers(item.getAttributeModifiers)
        case _ =>
      }
    }
    else {
      ClientPacketSender.sendRotatableStateRequest(this)
      ClientPacketSender.sendScreenBufferRequest(this)
      ClientPacketSender.sendRobotSelectedSlotRequest(this)
    }
  }

  override def invalidate() {
    super.invalidate()
    if (currentGui.isDefined) {
      Minecraft.getMinecraft.displayGuiScreen(null)
    }
  }

  // ----------------------------------------------------------------------- //

  override def readFromNBT(nbt: NBTTagCompound) {
    super.readFromNBT(nbt)
    if (isServer) {
      battery.load(nbt.getCompoundTag(Config.namespace + "battery"))
      buffer.load(nbt.getCompoundTag(Config.namespace + "buffer"))
      distributor.load(nbt.getCompoundTag(Config.namespace + "distributor"))
      gpu.load(nbt.getCompoundTag(Config.namespace + "gpu"))
      keyboard.load(nbt.getCompoundTag(Config.namespace + "keyboard"))
    }
    selectedSlot = nbt.getInteger(Config.namespace + "selectedSlot")
  }

  override def writeToNBT(nbt: NBTTagCompound) {
    super.writeToNBT(nbt)
    if (isServer) {
      nbt.setNewCompoundTag(Config.namespace + "battery", battery.save)
      nbt.setNewCompoundTag(Config.namespace + "buffer", buffer.save)
      nbt.setNewCompoundTag(Config.namespace + "distributor", distributor.save)
      nbt.setNewCompoundTag(Config.namespace + "gpu", gpu.save)
      nbt.setNewCompoundTag(Config.namespace + "keyboard", keyboard.save)
    }
    nbt.setInteger(Config.namespace + "selectedSlot", selectedSlot)
  }

  // ----------------------------------------------------------------------- //

  override def onMessage(message: Message) {
    if (message.source.network == node.network) {
      //computer.node.network.sendToReachable(message.source, message.name, message.data: _*)
    }
    else {
      assert(message.source.network == computer.node.network)
      //node.network.sendToReachable(message.source, message.name, message.data: _*)
    }
  }

  override def onConnect(node: Node) {
    if (node == this.node) {
      api.Network.joinNewNetwork(computer.node)

      computer.node.connect(buffer.node)
      computer.node.connect(distributor.node)
      computer.node.connect(gpu.node)
      distributor.node.connect(battery)
      buffer.node.connect(keyboard.node)
    }
    super.onConnect(node)
  }

  override def onDisconnect(node: Node) {
    super.onDisconnect(node)
    if (node == this.node) {
      battery.remove()
      buffer.node.remove()
      computer.node.remove()
      distributor.node.remove()
      gpu.node.remove()
      keyboard.node.remove()
    }
  }

  // ----------------------------------------------------------------------- //

  override protected def connectItemNode(node: Node) {
    computer.node.connect(node)
  }

  @SideOnly(Side.CLIENT)
  override protected def markForRenderUpdate() {
    super.markForRenderUpdate()
    currentGui.foreach(_.recompileDisplayLists())
  }

  // ----------------------------------------------------------------------- //

  def getInvName = Config.namespace + "container.Robot"

  def getSizeInventory = 19

  override def getInventoryStackLimit = 64

  def isItemValidForSlot(slot: Int, item: ItemStack) = (slot, Registry.driverFor(item)) match {
    case (0, _) => true // Allow anything in the tool slot.
    case (1, Some(driver)) => driver.slot(item) == Slot.Card
    case (2, Some(driver)) => driver.slot(item) == Slot.HardDiskDrive
    case (i, _) if 3 until getSizeInventory contains i => true // Normal inventory.
    case _ => false // Invalid slot.
  }

  override protected def onItemRemoved(slot: Int, item: ItemStack) {
    super.onItemRemoved(slot, item)
    if (slot == 0) {
      player_.getAttributeMap.removeAttributeModifiers(item.getAttributeModifiers)
    }
  }

  override protected def onItemAdded(slot: Int, item: ItemStack) {
    if (slot == 0) {
      player_.getAttributeMap.applyAttributeModifiers(item.getAttributeModifiers)
    }
    else if (slot == 1 || slot == 2) {
      super.onItemAdded(slot, item)
    }
  }
}

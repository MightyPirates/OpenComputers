package li.cil.oc.common.tileentity

import li.cil.oc.Config
import li.cil.oc.api
import li.cil.oc.api.driver.Slot
import li.cil.oc.api.network._
import li.cil.oc.client.{PacketSender => ClientPacketSender, gui}
import li.cil.oc.common.component.Buffer
import li.cil.oc.server
import li.cil.oc.server.component
import li.cil.oc.server.component.GraphicsCard
import li.cil.oc.server.driver.Registry
import li.cil.oc.util.ExtendedNBT._
import net.minecraft.client.Minecraft
import net.minecraft.item.ItemStack
import net.minecraft.nbt.NBTTagCompound
import scala.Some

class Robot(isRemote: Boolean) extends Computer(isRemote) with Buffer.Environment with PowerInformation {
  def this() = this(false)

  // ----------------------------------------------------------------------- //

  var currentGui: Option[gui.Robot] = None

  override val node = api.Network.newNode(this, Visibility.None).create()

  override val buffer = new Buffer(this) {
    override def maxResolution = (44, 14)
  }
  val (battery, distributor, gpu, keyboard) = if (isServer) {
    val battery = api.Network.newNode(this, Visibility.Network).withConnector(10000).create()
    val distributor = new component.PowerDistributor(this)
    val gpu = new GraphicsCard.Tier1 {
      override val maxResolution = (44, 14)
    }
    val keyboard = new component.Keyboard(this)
    (battery, distributor, gpu, keyboard)
  }
  else (null, null, null, null)

  // ----------------------------------------------------------------------- //

  def tier = 0

  //def bounds =

  override def installedMemory = 64 * 1024

  @LuaCallback("attack")
  def attack(context: Context, args: Arguments): Array[AnyRef] = {
    // Attack with equipped tool.
    val side = args.checkInteger(0)
    null
  }

  @LuaCallback("use")
  def use(context: Context, args: Arguments): Array[AnyRef] = {
    // Use equipped tool (e.g. dig, chop, till).
    val side = args.checkInteger(0)
    val sneaky = args.checkBoolean(1)
    null
  }

  // ----------------------------------------------------------------------- //

  @LuaCallback("check")
  def check(context: Context, args: Arguments): Array[AnyRef] = {
    // Test for blocks or entities.
    null
  }

  @LuaCallback("collect")
  def collect(context: Context, args: Arguments): Array[AnyRef] = {
    // Pick up items lying around.
    null
  }

  @LuaCallback("compare")
  def compare(context: Context, args: Arguments): Array[AnyRef] = {
    // Compare block to item selected in inventory.
    null
  }

  @LuaCallback("drop")
  def drop(context: Context, args: Arguments): Array[AnyRef] = {
    // Drop items from inventory.
    null
  }

  @LuaCallback("move")
  def move(context: Context, args: Arguments): Array[AnyRef] = {
    // Try to move in the specified direction.
    null
  }

  @LuaCallback("place")
  def place(context: Context, args: Arguments): Array[AnyRef] = {
    // Place block item selected in inventory.
    null
  }

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
    if (isClient) {
      ClientPacketSender.sendRotatableStateRequest(this)
      ClientPacketSender.sendScreenBufferRequest(this)
    }
  }

  override def invalidate() {
    super.invalidate()
    if (currentGui.isDefined) {
      Minecraft.getMinecraft.displayGuiScreen(null)
    }
  }

  // ----------------------------------------------------------------------- //

  //  override def onMessage(message: Message) {
  //    if (message.source.network == node.network) {
  //      computer.node.network.sendToReachable(message.source, message.name, message.data: _*)
  //    }
  //    else {
  //      node.network.sendToReachable(message.source, message.name, message.data: _*)
  //    }
  //  }

  override def onConnect(node: Node) {
    if (node == this.node) {
      server.network.Network.create(computer.node)

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

  override protected def connectItemNode(node: Node) {
    computer.node.connect(node)
  }

  // ----------------------------------------------------------------------- //

  override def readFromNBT(nbt: NBTTagCompound) {
    super.readFromNBT(nbt)
    if (isServer) {
      battery.load(nbt.getCompoundTag(Config.namespace + "battery"))
      buffer.load(nbt.getCompoundTag(Config.namespace + "buffer"))
      gpu.load(nbt.getCompoundTag(Config.namespace + "gpu"))
      keyboard.load(nbt.getCompoundTag(Config.namespace + "keyboard"))
    }
  }

  override def writeToNBT(nbt: NBTTagCompound) {
    super.writeToNBT(nbt)
    if (isServer) {
      nbt.setNewCompoundTag(Config.namespace + "battery", battery.save)
      nbt.setNewCompoundTag(Config.namespace + "buffer", buffer.save)
      nbt.setNewCompoundTag(Config.namespace + "gpu", gpu.save)
      nbt.setNewCompoundTag(Config.namespace + "keyboard", keyboard.save)
    }
  }

  // ----------------------------------------------------------------------- //

  override protected def markForRenderUpdate() {
    super.markForRenderUpdate()
    currentGui.foreach(_.recompileDisplayLists())
  }

  // ----------------------------------------------------------------------- //

  def getInvName = Config.namespace + "container.Robot"

  def getSizeInventory = 12

  def isItemValidForSlot(slot: Int, item: ItemStack) = (slot, Registry.driverFor(item)) match {
    case (0, Some(driver)) => driver.slot(item) == Slot.Tool
    case (1, Some(driver)) => driver.slot(item) == Slot.Card
    case (2, Some(driver)) => driver.slot(item) == Slot.HardDiskDrive
    case (3 | 4 | 5 | 6 | 7 | 8 | 9 | 10 | 11, _) => true // Normal inventory.
    case _ => false // Invalid slot.
  }
}

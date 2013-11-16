package li.cil.oc.common.tileentity

import li.cil.oc.Config
import li.cil.oc.api.driver.Slot
import li.cil.oc.api.network._
import li.cil.oc.client.{PacketSender => ClientPacketSender, gui}
import li.cil.oc.common.component.Buffer
import li.cil.oc.server.component
import li.cil.oc.server.component.GraphicsCard
import li.cil.oc.server.driver.Registry
import net.minecraft.client.Minecraft
import net.minecraft.item.ItemStack
import net.minecraft.nbt.NBTTagCompound
import scala.Some

class Robot(isRemote: Boolean) extends Computer(isRemote) with Buffer.Environment {
  def this() = this(false)

  // ----------------------------------------------------------------------- //

  val gpu = new GraphicsCard.Tier1 {
    override val maxResolution = (44, 14)
  }
  val keyboard = new component.Keyboard(this)

  var currentGui: Option[gui.Robot] = None

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
    gpu.update()
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

  override def onConnect(node: Node) {
    super.onConnect(node)
    if (node == this.node) {
      node.connect(gpu.node)
      node.connect(buffer.node)
      node.connect(keyboard.node)
    }
  }

  override def onDisconnect(node: Node) {
    super.onDisconnect(node)
    if (node == this.node) {
      gpu.node.remove()
      buffer.node.remove()
      keyboard.node.remove()
    }
  }

  // ----------------------------------------------------------------------- //

  override def readFromNBT(nbt: NBTTagCompound) {
    super.readFromNBT(nbt)
    if (isServer) {
      buffer.node.load(nbt.getCompoundTag(Config.namespace + "buffer"))
      buffer.load(nbt.getCompoundTag(Config.namespace + "buffer"))
      gpu.load(nbt.getCompoundTag(Config.namespace + "gpu"))
      keyboard.node.load(nbt.getCompoundTag(Config.namespace + "keyboard"))
    }
  }

  override def writeToNBT(nbt: NBTTagCompound) {
    super.writeToNBT(nbt)
    if (isServer) {
      val bufferNbt = new NBTTagCompound()
      buffer.node.save(bufferNbt)
      buffer.save(bufferNbt)
      nbt.setCompoundTag(Config.namespace + "buffer", bufferNbt)

      val gpuNbt = new NBTTagCompound()
      gpu.save(gpuNbt)
      nbt.setCompoundTag(Config.namespace + "gpu", gpuNbt)

      val keyboardNbt = new NBTTagCompound()
      keyboard.node.save(keyboardNbt)
      nbt.setCompoundTag(Config.namespace + "keyboard", keyboardNbt)
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
    case (1 | 2, Some(driver)) => driver.slot(item) == Slot.Card
    case (3 | 4 | 5 | 6 | 7 | 8 | 9 | 10 | 11, _) => true // Normal inventory.
    case _ => false // Invalid slot.
  }
}

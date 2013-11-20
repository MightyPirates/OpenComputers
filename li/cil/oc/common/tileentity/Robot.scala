package li.cil.oc.common.tileentity

import cpw.mods.fml.relauncher.{SideOnly, Side}
import li.cil.oc.api.driver.Slot
import li.cil.oc.api.network._
import li.cil.oc.client.{PacketSender => ClientPacketSender}
import li.cil.oc.server.component.GraphicsCard
import li.cil.oc.server.component.robot.Player
import li.cil.oc.server.driver.Registry
import li.cil.oc.server.{PacketSender => ServerPacketSender, component}
import li.cil.oc.util.ExtendedNBT._
import li.cil.oc.{Blocks, Config, api, common}
import net.minecraft.client.Minecraft
import net.minecraft.item.ItemStack
import net.minecraft.nbt.NBTTagCompound
import net.minecraftforge.common.ForgeDirection
import scala.Some
import scala.collection.convert.WrapAsScala._

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

  var selectedSlot = 0

  private lazy val player_ = new Player(this)

  var animationTicksLeft = 0

  var animationTicksTotal = 0

  var moveDirection = ForgeDirection.UNKNOWN

  var turnOldFacing = ForgeDirection.UNKNOWN

  // ----------------------------------------------------------------------- //

  def player(facing: ForgeDirection = facing, side: ForgeDirection = facing) = {
    assert(isServer)
    player_.updatePositionAndRotation(facing, side)
    player_
  }

  def actualSlot(n: Int) = n + 3

  def move(direction: ForgeDirection) = {
    val (ox, oy, oz) = (x, y, z)
    val (nx, ny, nz) = (x + direction.offsetX, y + direction.offsetY, z + direction.offsetZ)
    // Setting this will make the tile entity created via the following call
    // to setBlock to re-use our "real" instance as the inner object, instead
    // of creating a new one.
    Blocks.robot.moving.set(Some(this))
    // Do *not* immediately send the change to clients to allow checking if it
    // worked before the client is notified so that we can use the same trick on
    // the client by sending a corresponding packet. This also saves us from
    // having to send the complete state again (e.g. screen buffer) each move.
    val blockId = world.getBlockId(nx, ny, nz)
    val metadata = world.getBlockMetadata(nx, ny, nz)
    val created = world.setBlock(nx, ny, nz, getBlockType.blockID, getBlockMetadata, 1)
    if (created) {
      assert(world.getBlockTileEntity(nx, ny, nz) == this)
      assert(x == nx && y == ny && z == nz)
      world.setBlock(ox, oy, oz, Blocks.robotAfterimage.parent.blockID, Blocks.robotAfterimage.blockId, 1)
      assert(Blocks.blockSpecial.subBlock(world, ox, oy, oz).exists(_ == Blocks.robotAfterimage))
      if (isServer) {
        ServerPacketSender.sendRobotMove(this, ox, oy, oz, direction)
        for (neighbor <- node.neighbors) {
          node.disconnect(neighbor)
        }
        api.Network.joinOrCreateNetwork(world, nx, ny, nz)
      }
      else {
        // On the client this is called from the packet handler code, leading
        // to the entity being added directly to the list of loaded tile
        // entities, without any additional checks - so we get a duplicate.
        val duplicate = world.loadedTileEntityList.remove(world.loadedTileEntityList.size - 1)
        assert(duplicate == this)
        if (blockId > 0) {
          world.playAuxSFX(2001, nx, ny, nz, blockId + (metadata << 12))
        }
        world.markBlockForUpdate(ox, oy, oz)
        world.markBlockForUpdate(nx, ny, nz)
      }
      assert(!isInvalid)
    }
    Blocks.robot.moving.set(None)
    if (created) {
      animateMove(direction, Config.moveDelay)
      checkRedstoneInputChanged()
    }
    created
  }

  def isAnimatingMove = animationTicksLeft > 0 && moveDirection != ForgeDirection.UNKNOWN

  def isAnimatingTurn = animationTicksLeft > 0 && turnOldFacing != ForgeDirection.UNKNOWN

  def animateMove(direction: ForgeDirection, duration: Double) {
    animationTicksTotal = (duration * 20).toInt
    animationTicksLeft = animationTicksTotal
    moveDirection = direction
    turnOldFacing = ForgeDirection.UNKNOWN
  }

  def animateTurn(oldFacing: ForgeDirection, duration: Double) {
    animationTicksTotal = (duration * 20).toInt
    animationTicksLeft = animationTicksTotal
    moveDirection = ForgeDirection.UNKNOWN
    turnOldFacing = oldFacing
  }

  // ----------------------------------------------------------------------- //

  override def installedMemory = 64 * 1024

  def tier = 0

  // ----------------------------------------------------------------------- //

  @LuaCallback("start")
  def start(context: Context, args: Arguments): Array[AnyRef] =
    result(computer.start())

  @LuaCallback("stop")
  def stop(context: Context, args: Arguments): Array[AnyRef] =
    result(computer.stop())

  @LuaCallback(value = "isRunning", direct = true)
  def isRunning(context: Context, args: Arguments): Array[AnyRef] =
    result(computer.isRunning)

  // ----------------------------------------------------------------------- //

  override def updateEntity() {
    if (animationTicksLeft > 0) {
      animationTicksLeft -= 1
      if (animationTicksLeft == 0) {
        val (ox, oy, oz) = (x - moveDirection.offsetX, y - moveDirection.offsetY, z - moveDirection.offsetZ)
        if (Blocks.blockSpecial.subBlock(world, ox, oy, oz).exists(_ == Blocks.robotAfterimage)) {
          world.setBlockToAir(ox, oy, oz)
        }
      }
      if (isClient) {
        world.markBlockForRenderUpdate(x, y, z)
      }
    }
    super.updateEntity()
    if (isServer) {
      distributor.changeBuffer(10) // just for testing
      distributor.update()
      gpu.update()
    }
  }

  override def validate() {
    if (Blocks.robot.moving.get.isEmpty) {
      super.validate()
      if (isServer) {
        items(0) match {
          case Some(item) => player_.getAttributeMap.applyAttributeModifiers(item.getAttributeModifiers)
          case _ =>
        }
      }
      else {
        ClientPacketSender.sendScreenBufferRequest(this)
        ClientPacketSender.sendRobotStateRequest(this)
      }
    }
  }

  override def invalidate() {
    if (Blocks.robot.moving.get.isEmpty) {
      super.invalidate()
      if (currentGui.isDefined) {
        Minecraft.getMinecraft.displayGuiScreen(null)
      }
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
    animationTicksTotal = nbt.getInteger(Config.namespace + "animationTicksTotal")
    animationTicksLeft = nbt.getInteger(Config.namespace + "animationTicksLeft")
    moveDirection = ForgeDirection.getOrientation(nbt.getInteger(Config.namespace + "moveDirection"))
    turnOldFacing = ForgeDirection.getOrientation(nbt.getInteger(Config.namespace + "turnOldFacing"))
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
    if (isAnimatingMove || isAnimatingTurn) {
      nbt.setInteger(Config.namespace + "animationTicksTotal", animationTicksTotal)
      nbt.setInteger(Config.namespace + "animationTicksLeft", animationTicksLeft)
      nbt.setInteger(Config.namespace + "moveDirection", moveDirection.ordinal)
      nbt.setInteger(Config.namespace + "turnOldFacing", turnOldFacing.ordinal)
    }
  }

  // ----------------------------------------------------------------------- //

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

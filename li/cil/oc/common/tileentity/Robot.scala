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
import net.minecraft.entity.player.EntityPlayer
import net.minecraft.item.ItemStack
import net.minecraft.nbt.NBTTagCompound
import net.minecraftforge.common.ForgeDirection
import scala.Some

// Implementation note: this tile entity is never directly added to the world.
// It is always wrapped by a `RobotProxy` tile entity, which forwards any
// necessary calls to this class. This is done to make moves efficient: when a
// robot moves we only create a new proxy tile entity, hook the instance of this
// class that was held by the old proxy to it and can then safely forget the
// old proxy, which will be cleaned up by Minecraft like any other tile entity.
class Robot(isRemote: Boolean) extends Computer(isRemote) with Buffer with PowerInformation {
  def this() = this(false)

  var proxy: RobotProxy = _

  // ----------------------------------------------------------------------- //

  override def node = if (isClient) null else computer.node

  override val buffer_ = new common.component.Buffer(this) {
    override def maxResolution = (48, 14)
  }
  override val computer_ = if (isRemote) null else new component.Robot(this)
  val (battery, distributor, gpu, keyboard) = if (isServer) {
    val battery = api.Network.newNode(this, Visibility.Network).withConnector(10000).create()
    val distributor = new component.PowerDistributor(this)
    val gpu = new GraphicsCard.Tier1 {
      override val maxResolution = (48, 14)
    }
    val keyboard = new component.Keyboard(this) {
      override def isUseableByPlayer(p: EntityPlayer) =
        world.getBlockTileEntity(x, y, z) == proxy &&
          p.getDistanceSq(x + 0.5, y + 0.5, z + 0.5) <= 64
    }
    (battery, distributor, gpu, keyboard)
  }
  else (null, null, null, null)

  var selectedSlot = 0

  var equippedItem: Option[ItemStack] = None

  var animationTicksLeft = 0

  var animationTicksTotal = 0

  var moveDirection = ForgeDirection.UNKNOWN

  var swingingTool = false

  var turnAxis = 0

  private lazy val player_ = new Player(this)

  // ----------------------------------------------------------------------- //

  def player(facing: ForgeDirection = facing, side: ForgeDirection = facing) = {
    player_.updatePositionAndRotation(facing, side)
    player_
  }

  def actualSlot(n: Int) = n + 3

  def move(direction: ForgeDirection) = {
    val (ox, oy, oz) = (x, y, z)
    val (nx, ny, nz) = (x + direction.offsetX, y + direction.offsetY, z + direction.offsetZ)
    val blockId = world.getBlockId(nx, ny, nz)
    val metadata = world.getBlockMetadata(nx, ny, nz)
    try {
      // Setting this will make the tile entity created via the following call
      // to setBlock to re-use our "real" instance as the inner object, instead
      // of creating a new one.
      Blocks.robotProxy.moving.set(Some(this))
      // Do *not* immediately send the change to clients to allow checking if it
      // worked before the client is notified so that we can use the same trick on
      // the client by sending a corresponding packet. This also saves us from
      // having to send the complete state again (e.g. screen buffer) each move.
      val created = world.setBlock(nx, ny, nz, Blocks.robotProxy.parent.blockID, Blocks.robotProxy.blockId, 1)
      if (created) {
        assert(world.getBlockTileEntity(nx, ny, nz) == proxy)
        assert(x == nx && y == ny && z == nz)
        world.setBlock(ox, oy, oz, Blocks.robotAfterimage.parent.blockID, Blocks.robotAfterimage.blockId, 1)
        assert(Blocks.blockSpecial.subBlock(world, ox, oy, oz).exists(_ == Blocks.robotAfterimage))
        if (isServer) {
          ServerPacketSender.sendRobotMove(this, ox, oy, oz, direction)
        }
        else {
          // If we broke some replaceable block (like grass) play its break sound.
          if (blockId > 0) {
            world.playAuxSFX(2001, nx, ny, nz, blockId + (metadata << 12))
          }
          world.markBlockForRenderUpdate(ox, oy, oz)
          world.markBlockForRenderUpdate(nx, ny, nz)
        }
        assert(!isInvalid)
      }
      if (created) {
        // Here instead of Lua callback so that it gets triggered on client.
        animateMove(direction, Config.moveDelay)
        checkRedstoneInputChanged()
      }
      created
    }
    finally {
      Blocks.robotProxy.moving.set(None)
    }
  }

  def isAnimatingMove = animationTicksLeft > 0 && moveDirection != ForgeDirection.UNKNOWN

  def isAnimatingSwing = animationTicksLeft > 0 && swingingTool

  def isAnimatingTurn = animationTicksLeft > 0 && turnAxis != 0

  def animateMove(direction: ForgeDirection, duration: Double) =
    setAnimateMove(direction, (duration * 20).toInt)

  def animateSwing(duration: Double) = {
    setAnimateSwing((duration * 20).toInt)
    ServerPacketSender.sendRobotAnimateSwing(this)
  }

  def animateTurn(clockwise: Boolean, duration: Double) = {
    setAnimateTurn(if (clockwise) 1 else -1, (duration * 20).toInt)
    ServerPacketSender.sendRobotAnimateTurn(this)
  }

  def setAnimateMove(direction: ForgeDirection, ticks: Int) {
    animationTicksTotal = ticks
    prepareForAnimation()
    moveDirection = direction
  }

  def setAnimateSwing(ticks: Int) {
    animationTicksTotal = ticks
    prepareForAnimation()
    swingingTool = true
  }

  def setAnimateTurn(axis: Int, ticks: Int) {
    animationTicksTotal = ticks
    prepareForAnimation()
    turnAxis = axis
  }

  private def prepareForAnimation() {
    animationTicksLeft = animationTicksTotal
    moveDirection = ForgeDirection.UNKNOWN
    swingingTool = false
    turnAxis = 0
  }

  // ----------------------------------------------------------------------- //

  override def getRenderBoundingBox =
    getBlockType.getCollisionBoundingBoxFromPool(world, x, y, z).expand(0.5, 0.5, 0.5)

  override def installedMemory = 64 * 1024

  def tier = 0

  // ----------------------------------------------------------------------- //

  override def updateEntity() {
    if (node != null && node.network == null) {
      api.Network.joinNewNetwork(node)
    }
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

  override def invalidate() {
    super.invalidate()
    if (currentGui.isDefined) {
      Minecraft.getMinecraft.displayGuiScreen(null)
    }
  }

  // ----------------------------------------------------------------------- //

  override def readFromNBT(nbt: NBTTagCompound) {
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
    if (animationTicksLeft > 0) {
      moveDirection = ForgeDirection.getOrientation(nbt.getByte(Config.namespace + "moveDirection"))
      swingingTool = nbt.getBoolean(Config.namespace + "swingingTool")
      turnAxis = nbt.getByte(Config.namespace + "turnAxis")
    }
  }

  override def writeToNBT(nbt: NBTTagCompound) {
    if (isServer) {
      nbt.setNewCompoundTag(Config.namespace + "battery", battery.save)
      nbt.setNewCompoundTag(Config.namespace + "buffer", buffer.save)
      nbt.setNewCompoundTag(Config.namespace + "distributor", distributor.save)
      nbt.setNewCompoundTag(Config.namespace + "gpu", gpu.save)
      nbt.setNewCompoundTag(Config.namespace + "keyboard", keyboard.save)
    }
    nbt.setInteger(Config.namespace + "selectedSlot", selectedSlot)
    if (isAnimatingMove || isAnimatingSwing || isAnimatingTurn) {
      nbt.setInteger(Config.namespace + "animationTicksTotal", animationTicksTotal)
      nbt.setInteger(Config.namespace + "animationTicksLeft", animationTicksLeft)
      nbt.setByte(Config.namespace + "moveDirection", moveDirection.ordinal.toByte)
      nbt.setBoolean(Config.namespace + "swingingTool", swingingTool)
      nbt.setByte(Config.namespace + "turnAxis", turnAxis.toByte)
    }
  }

  // ----------------------------------------------------------------------- //

  override def onConnect(node: Node) {
    super.onConnect(node)
    if (node == this.node) {
      computer.node.connect(buffer.node)
      computer.node.connect(distributor.node)
      computer.node.connect(gpu.node)
      distributor.node.connect(battery)
      buffer.node.connect(keyboard.node)
    }
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

  @SideOnly(Side.CLIENT)
  override protected def markForRenderUpdate() {
    super.markForRenderUpdate()
    currentGui.foreach(_.recompileDisplayLists())
  }

  // ----------------------------------------------------------------------- //

  def getInvName = Config.namespace + "container.Robot"

  def getSizeInventory = 19

  override def getInventoryStackLimit = 64

  override def isUseableByPlayer(player: EntityPlayer) =
    world.getBlockTileEntity(x, y, z) match {
      case t: RobotProxy if t == proxy => player.getDistanceSq(x + 0.5, y + 0.5, z + 0.5) <= 64
      case _ => false
    }

  def isItemValidForSlot(slot: Int, item: ItemStack) = (slot, Registry.driverFor(item)) match {
    case (0, _) => true // Allow anything in the tool slot.
    case (1, Some(driver)) => driver.slot(item) == Slot.Card
    case (2, Some(driver)) => driver.slot(item) == Slot.HardDiskDrive
    case (i, _) if 3 until getSizeInventory contains i => true // Normal inventory.
    case _ => false // Invalid slot.
  }

  override def onInventoryChanged() {
    super.onInventoryChanged()
    if (isServer) {
      computer.signal("inventory_changed")
    }
  }

  override protected def onItemRemoved(slot: Int, item: ItemStack) {
    super.onItemRemoved(slot, item)
    if (isServer) {
      if (slot == 0) {
        player_.getAttributeMap.removeAttributeModifiers(item.getAttributeModifiers)
        ServerPacketSender.sendRobotEquippedItemChange(this, null)
      }
      else if (slot >= actualSlot(0)) {
        computer.signal("inventory_changed", Int.box(slot - actualSlot(0) + 1))
      }
    }
  }

  override protected def onItemAdded(slot: Int, item: ItemStack) {
    if (isServer) {
      if (slot == 0) {
        player_.getAttributeMap.applyAttributeModifiers(item.getAttributeModifiers)
        ServerPacketSender.sendRobotEquippedItemChange(this, getStackInSlot(0))
      }
      else if (slot == 1 || slot == 2) {
        super.onItemAdded(slot, item)
      }
      else if (slot >= actualSlot(0)) {
        computer.signal("inventory_changed", Int.box(slot - actualSlot(0) + 1))
      }
    }
  }
}

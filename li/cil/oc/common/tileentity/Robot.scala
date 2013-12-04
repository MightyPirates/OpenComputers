package li.cil.oc.common.tileentity

import cpw.mods.fml.relauncher.{SideOnly, Side}
import li.cil.oc.api.driver.Slot
import li.cil.oc.api.network._
import li.cil.oc.common.block.Delegator
import li.cil.oc.server.component.GraphicsCard
import li.cil.oc.server.component.robot.Player
import li.cil.oc.server.driver.Registry
import li.cil.oc.server.{PacketSender => ServerPacketSender, driver, component}
import li.cil.oc.util.ExtendedNBT._
import li.cil.oc.{Blocks, Settings, api, common}
import net.minecraft.client.Minecraft
import net.minecraft.entity.player.EntityPlayer
import net.minecraft.inventory.ISidedInventory
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
class Robot(isRemote: Boolean) extends Computer(isRemote) with ISidedInventory with Buffer with PowerInformation {
  def this() = this(false)

  var proxy: RobotProxy = _

  // ----------------------------------------------------------------------- //

  override def node = if (isClient) null else computer.node

  override val _buffer = new common.component.Buffer(this) {
    override def maxResolution = (48, 14)
  }
  override val _computer = if (isRemote) null else new component.Robot(this)
  val (battery, distributor, gpu, keyboard) = if (isServer) {
    val battery = api.Network.newNode(this, Visibility.Network).
      withConnector(Settings.get.bufferRobot).
      create()
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

  var owner = "OpenComputers"

  var xp = 0.0

  def xpForNextLevel = Settings.get.baseXpToLevel + Math.pow((level + 1) * Settings.get.constantXpGrowth, Settings.get.exponentialXpGrowth)

  var level = 0

  var xpChanged = false

  var globalBuffer = 0.0

  var globalBufferSize = 0.0

  var selectedSlot = actualSlot(0)

  var equippedItem: Option[ItemStack] = None

  var animationTicksLeft = 0

  var animationTicksTotal = 0

  var moveDirection = ForgeDirection.UNKNOWN

  var swingingTool = false

  var turnAxis = 0

  private lazy val player_ = new Player(this)

  def addXp(value: Double) {
    if (level < 29 && isServer) {
      xp = xp + value
      xpChanged = true
      if (xp >= xpForNextLevel) {
        updateXpInfo()
      }
    }
  }

  def updateXpInfo() {
    // xp(level) = base + (level * const) ^ exp
    // pow(xp(level) - base, 1/exp) / const = level
    level = (Math.pow(xp - Settings.get.baseXpToLevel, 1 / Settings.get.exponentialXpGrowth) / Settings.get.constantXpGrowth).toInt min 29
    battery.setLocalBufferSize(Settings.get.bufferRobot + Settings.get.bufferPerLevel * level)
  }

  // ----------------------------------------------------------------------- //

  override def onAnalyze(stats: NBTTagCompound, player: EntityPlayer, side: Int, hitX: Float, hitY: Float, hitZ: Float) = {
    stats.setString(Settings.namespace + "gui.Analyzer.RobotOwner", owner)
    stats.setString(Settings.namespace + "gui.Analyzer.RobotName", player_.getCommandSenderName)
    stats.setString(Settings.namespace + "gui.Analyzer.RobotXp", xp.formatted("%.2f"))
    super.onAnalyze(stats, player, side, hitX, hitY, hitZ)
  }

  def player(facing: ForgeDirection = facing, side: ForgeDirection = facing) = {
    player_.updatePositionAndRotation(facing, side)
    player_
  }

  def actualSlot(n: Int) = n + 4

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
      val created = Blocks.robotProxy.setBlock(world, nx, ny, nz, 1)
      if (created) {
        assert(world.getBlockTileEntity(nx, ny, nz) == proxy)
        assert(x == nx && y == ny && z == nz)
        Blocks.robotAfterimage.setBlock(world, ox, oy, oz, 1)
        assert(Delegator.subBlock(world, ox, oy, oz).exists(_ == Blocks.robotAfterimage))
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
        animateMove(direction, Settings.get.moveDelay)
        checkRedstoneInputChanged()
      }
      created
    }
    finally {
      Blocks.robotProxy.moving.set(None)
    }
  }

  def createItemStack() = {
    val stack = Blocks.robotProxy.createItemStack()
    if (globalBuffer > 1 || xp > 0) {
      stack.setTagCompound(new NBTTagCompound("tag"))
    }
    if (xp > 0) {
      stack.getTagCompound.setDouble(Settings.namespace + "xp", xp)
    }
    if (globalBuffer > 1) {
      stack.getTagCompound.setInteger(Settings.namespace + "storedEnergy", globalBuffer.toInt)
    }
    stack
  }

  def parseItemStack(stack: ItemStack) {
    if (stack.hasTagCompound) {
      xp = stack.getTagCompound.getDouble(Settings.namespace + "xp")
      updateXpInfo()
      battery.changeBuffer(stack.getTagCompound.getInteger(Settings.namespace + "storedEnergy"))
    }
  }

  // ----------------------------------------------------------------------- //

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

  override def shouldRenderInPass(pass: Int) = true

  override def getRenderBoundingBox =
    getBlockType.getCollisionBoundingBoxFromPool(world, x, y, z).expand(0.5, 0.5, 0.5)

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
      distributor.update()
      gpu.update()
      if (xpChanged && world.getWorldInfo.getWorldTotalTime % 200 == 0) {
        xpChanged = false
        ServerPacketSender.sendRobotXp(this)
      }
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
      battery.load(nbt.getCompoundTag(Settings.namespace + "battery"))
      buffer.load(nbt.getCompoundTag(Settings.namespace + "buffer"))
      distributor.load(nbt.getCompoundTag(Settings.namespace + "distributor"))
      gpu.load(nbt.getCompoundTag(Settings.namespace + "gpu"))
      keyboard.load(nbt.getCompoundTag(Settings.namespace + "keyboard"))
    }
    if (nbt.hasKey(Settings.namespace + "owner")) {
      owner = nbt.getString(Settings.namespace + "owner")
    }
    xp = nbt.getDouble(Settings.namespace + "xp")
    updateXpInfo()
    selectedSlot = nbt.getInteger(Settings.namespace + "selectedSlot")
    animationTicksTotal = nbt.getInteger(Settings.namespace + "animationTicksTotal")
    animationTicksLeft = nbt.getInteger(Settings.namespace + "animationTicksLeft")
    if (animationTicksLeft > 0) {
      moveDirection = ForgeDirection.getOrientation(nbt.getByte(Settings.namespace + "moveDirection"))
      swingingTool = nbt.getBoolean(Settings.namespace + "swingingTool")
      turnAxis = nbt.getByte(Settings.namespace + "turnAxis")
    }
  }

  override def writeToNBT(nbt: NBTTagCompound) {
    nbt.setNewCompoundTag(Settings.namespace + "battery", battery.save)
    nbt.setNewCompoundTag(Settings.namespace + "buffer", buffer.save)
    nbt.setNewCompoundTag(Settings.namespace + "distributor", distributor.save)
    nbt.setNewCompoundTag(Settings.namespace + "gpu", gpu.save)
    nbt.setNewCompoundTag(Settings.namespace + "keyboard", keyboard.save)
    nbt.setString(Settings.namespace + "owner", owner)
    nbt.setDouble(Settings.namespace + "xp", xp)
    nbt.setInteger(Settings.namespace + "selectedSlot", selectedSlot)
    if (isAnimatingMove || isAnimatingSwing || isAnimatingTurn) {
      nbt.setInteger(Settings.namespace + "animationTicksTotal", animationTicksTotal)
      nbt.setInteger(Settings.namespace + "animationTicksLeft", animationTicksLeft)
      nbt.setByte(Settings.namespace + "moveDirection", moveDirection.ordinal.toByte)
      nbt.setBoolean(Settings.namespace + "swingingTool", swingingTool)
      nbt.setByte(Settings.namespace + "turnAxis", turnAxis.toByte)
    }
  }

  @SideOnly(Side.CLIENT)
  override def readFromNBTForClient(nbt: NBTTagCompound) {
    super.readFromNBTForClient(nbt)
    selectedSlot = nbt.getInteger("selectedSlot")
    if (nbt.hasKey("equipped")) {
      equippedItem = Option(ItemStack.loadItemStackFromNBT(nbt.getCompoundTag("equipped")))
    }
    xp = nbt.getDouble(Settings.namespace + "xp")
    animationTicksTotal = nbt.getInteger("animationTicksTotal")
    animationTicksLeft = nbt.getInteger("animationTicksLeft")
    if (animationTicksLeft > 0) {
      moveDirection = ForgeDirection.getOrientation(nbt.getByte("moveDirection"))
      swingingTool = nbt.getBoolean("swingingTool")
      turnAxis = nbt.getByte("turnAxis")
    }
  }

  override def writeToNBTForClient(nbt: NBTTagCompound) {
    super.writeToNBTForClient(nbt)
    nbt.setInteger("selectedSlot", selectedSlot)
    if (getStackInSlot(0) != null) {
      nbt.setNewCompoundTag("equipped", getStackInSlot(0).writeToNBT)
    }
    nbt.setDouble(Settings.namespace + "xp", xp)
    if (isAnimatingMove || isAnimatingSwing || isAnimatingTurn) {
      nbt.setInteger("animationTicksTotal", animationTicksTotal)
      nbt.setInteger("animationTicksLeft", animationTicksLeft)
      nbt.setByte("moveDirection", moveDirection.ordinal.toByte)
      nbt.setBoolean("swingingTool", swingingTool)
      nbt.setByte("turnAxis", turnAxis.toByte)
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

  override protected def onItemRemoved(slot: Int, stack: ItemStack) {
    super.onItemRemoved(slot, stack)
    if (isServer) {
      if (slot == 0) {
        player_.getAttributeMap.removeAttributeModifiers(stack.getAttributeModifiers)
        ServerPacketSender.sendRobotEquippedItemChange(this, null)
      }
      else if (slot >= actualSlot(0)) {
        computer.signal("inventory_changed", Int.box(slot - actualSlot(0) + 1))
      }
    }
  }

  override protected def onItemAdded(slot: Int, stack: ItemStack) {
    if (isServer) {
      if (slot == 0) {
        player_.getAttributeMap.applyAttributeModifiers(stack.getAttributeModifiers)
        ServerPacketSender.sendRobotEquippedItemChange(this, getStackInSlot(0))
      }
      else if (isComponentSlot(slot)) {
        super.onItemAdded(slot, stack)
      }
      else if (slot >= actualSlot(0)) {
        computer.signal("inventory_changed", Int.box(slot - actualSlot(0) + 1))
      }
    }
  }

  override protected def isComponentSlot(slot: Int) = slot > 0 && slot < actualSlot(0)

  // ----------------------------------------------------------------------- //

  override def installedMemory = 64 * 1024

  def tier = 0

  override def hasRedstoneCard = items(1).fold(false)(driver.item.RedstoneCard.worksWith)

  @SideOnly(Side.CLIENT)
  override protected def markForRenderUpdate() {
    super.markForRenderUpdate()
    currentGui.foreach(_.recompileDisplayLists())
  }

  // ----------------------------------------------------------------------- //

  def getInvName = Settings.namespace + "container.Robot"

  def getSizeInventory = 20

  override def getInventoryStackLimit = 64

  override def setInventorySlotContents(slot: Int, stack: ItemStack) = {
    if ((1 to 2 contains slot) && stack != null && stack.stackSize > 1) {
      super.setInventorySlotContents(slot, stack.splitStack(1))
      if (stack.stackSize > 0 && isServer) {
        val p = player()
        p.inventory.addItemStackToInventory(stack)
        p.dropPlayerItemWithRandomChoice(stack, inPlace = false)
      }
    }
    else super.setInventorySlotContents(slot, stack)
  }

  override def isUseableByPlayer(player: EntityPlayer) =
    world.getBlockTileEntity(x, y, z) match {
      case t: RobotProxy if t == proxy && computer.canInteract(player.getCommandSenderName) =>
        player.getDistanceSq(x + 0.5, y + 0.5, z + 0.5) <= 64
      case _ => false
    }

  def isItemValidForSlot(slot: Int, stack: ItemStack) = (slot, Registry.driverFor(stack)) match {
    case (0, _) => true // Allow anything in the tool slot.
    case (1, Some(driver)) => driver.slot(stack) == Slot.Card
    case (2, Some(driver)) => driver.slot(stack) == Slot.Disk
    case (3, Some(driver)) => driver.slot(stack) == Slot.Upgrade
    case (i, _) if actualSlot(0) until getSizeInventory contains i => true // Normal inventory.
    case _ => false // Invalid slot.
  }

  // ----------------------------------------------------------------------- //

  def canExtractItem(slot: Int, stack: ItemStack, side: Int) =
    getAccessibleSlotsFromSide(side).contains(slot)

  def canInsertItem(slot: Int, stack: ItemStack, side: Int) =
    getAccessibleSlotsFromSide(side).contains(slot) &&
      isItemValidForSlot(slot, stack)

  def getAccessibleSlotsFromSide(side: Int) =
    toLocal(ForgeDirection.getOrientation(side)) match {
      case ForgeDirection.WEST => Array(0)
      case ForgeDirection.EAST => Array(1)
      case ForgeDirection.NORTH => Array(2)
      case _ => (actualSlot(3) until getSizeInventory).toArray
    }
}

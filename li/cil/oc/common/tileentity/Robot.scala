package li.cil.oc.common.tileentity

import cpw.mods.fml.relauncher.{SideOnly, Side}
import li.cil.oc.api.driver.Slot
import li.cil.oc.api.network._
import li.cil.oc.common.block.Delegator
import li.cil.oc.server.component.GraphicsCard
import li.cil.oc.server.component.robot
import li.cil.oc.server.driver.Registry
import li.cil.oc.server.{PacketSender => ServerPacketSender, driver, component}
import li.cil.oc.util.ExtendedNBT._
import li.cil.oc.{Blocks, Settings, api, common}
import net.minecraft.client.Minecraft
import net.minecraft.entity.player.EntityPlayer
import net.minecraft.inventory.ISidedInventory
import net.minecraft.item.ItemStack
import net.minecraft.nbt.NBTTagCompound
import net.minecraft.util.ChatMessageComponent
import net.minecraftforge.common.ForgeDirection
import scala.Some

// Implementation note: this tile entity is never directly added to the world.
// It is always wrapped by a `RobotProxy` tile entity, which forwards any
// necessary calls to this class. This is done to make moves efficient: when a
// robot moves we only create a new proxy tile entity, hook the instance of this
// class that was held by the old proxy to it and can then safely forget the
// old proxy, which will be cleaned up by Minecraft like any other tile entity.
class Robot(isRemote: Boolean) extends Computer(isRemote) with ISidedInventory with Buffer with PowerInformation with RobotContext {
  def this() = this(false)

  var proxy: RobotProxy = _

  // ----------------------------------------------------------------------- //

  // Note: we implement IRobotContext in the TE to allow external components
  //to cast their owner to it (to allow interacting with their owning robot).

  var selectedSlot = actualSlot(0)

  def player() = player(facing, facing)

  def saveUpgrade() = this.synchronized {
    components(3) match {
      case Some(environment) =>
        val stack = getStackInSlot(3)
        // We're guaranteed to have a driver for entries.
        environment.save(dataTag(Registry.itemDriverFor(stack).get, stack))
        ServerPacketSender.sendRobotEquippedUpgradeChange(this, stack)
      case _ =>
    }
  }

  // ----------------------------------------------------------------------- //

  override def node = if (isClient) null else computer.node

  override val _buffer = new common.component.Buffer(this) {
    override def maxResolution = (48, 14)
  }
  override val _computer = if (isRemote) null else new robot.Robot(this)
  val (gpu, keyboard) = if (isServer) {
    val gpu = new GraphicsCard.Tier1 {
      override val maxResolution = (48, 14)
    }
    val keyboard = new component.Keyboard {
      override def isUseableByPlayer(p: EntityPlayer) =
        world.getBlockTileEntity(x, y, z) == proxy &&
          p.getDistanceSq(x + 0.5, y + 0.5, z + 0.5) <= 64
    }
    (gpu, keyboard)
  }
  else (null, null)

  var owner = "OpenComputers"

  var xp = 0.0

  def xpForNextLevel = xpForLevel(level + 1)

  def xpForLevel(level: Int) = Settings.get.baseXpToLevel + Math.pow(level * Settings.get.constantXpGrowth, Settings.get.exponentialXpGrowth)

  var level = 0

  var xpChanged = false

  var globalBuffer, globalBufferSize = 0.0

  var equippedItem: Option[ItemStack] = None

  var equippedUpgrade: Option[ItemStack] = None

  var animationTicksLeft = 0

  var animationTicksTotal = 0

  var moveFromX, moveFromY, moveFromZ = Int.MaxValue

  var swingingTool = false

  var turnAxis = 0

  private lazy val player_ = new robot.Player(this)

  def addXp(value: Double) {
    if (level < 30 && isServer) {
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
    level = math.min((Math.pow(xp - Settings.get.baseXpToLevel, 1 / Settings.get.exponentialXpGrowth) / Settings.get.constantXpGrowth).toInt, 30)
    if (isServer) {
      computer.node.setLocalBufferSize(Settings.get.bufferRobot + Settings.get.bufferPerLevel * level)
    }
  }

  def maxComponents = 12

  // ----------------------------------------------------------------------- //

  override def onAnalyze(player: EntityPlayer, side: Int, hitX: Float, hitY: Float, hitZ: Float) = {
    player.sendChatToPlayer(ChatMessageComponent.createFromTranslationWithSubstitutions(
      Settings.namespace + "gui.Analyzer.RobotOwner", owner))
    player.sendChatToPlayer(ChatMessageComponent.createFromTranslationWithSubstitutions(
      Settings.namespace + "gui.Analyzer.RobotName", player_.getCommandSenderName))
    player.sendChatToPlayer(ChatMessageComponent.createFromTranslationWithSubstitutions(
      Settings.namespace + "gui.Analyzer.RobotXp", xp.formatted("%.2f")))
    super.onAnalyze(player, side, hitX, hitY, hitZ)
  }

  def player(facing: ForgeDirection = facing, side: ForgeDirection = facing) = {
    player_.updatePositionAndRotation(facing, side)
    player_
  }

  def actualSlot(n: Int) = n + 4

  def move(direction: ForgeDirection): Boolean = {
    val (ox, oy, oz) = (x, y, z)
    val (nx, ny, nz) = (x + direction.offsetX, y + direction.offsetY, z + direction.offsetZ)
    if (!world.blockExists(nx, ny, nz)) {
      return false // Don't fall off the earth.
    }
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
      world.setBlockToAir(nx, ny, nz)
      val created = Blocks.robotProxy.setBlock(world, nx, ny, nz, 1)
      if (created) {
        assert(world.getBlockTileEntity(nx, ny, nz) == proxy)
        assert(x == nx && y == ny && z == nz)
        world.setBlock(ox, oy, oz, 0, 0, 1)
        Blocks.robotAfterimage.setBlock(world, ox, oy, oz, 1)
        assert(Delegator.subBlock(world, ox, oy, oz).exists(_ == Blocks.robotAfterimage))
        // Here instead of Lua callback so that it gets called on client, too.
        val moveTicks = math.max((Settings.get.moveDelay * 20).toInt, 1)
        setAnimateMove(ox, oy, oz, moveTicks)
        if (isServer) {
          ServerPacketSender.sendRobotMove(this, ox, oy, oz, direction)
          checkRedstoneInputChanged()
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
      computer.node.changeBuffer(stack.getTagCompound.getInteger(Settings.namespace + "storedEnergy"))
    }
  }

  // ----------------------------------------------------------------------- //

  def isAnimatingMove = animationTicksLeft > 0 && (moveFromX != Int.MaxValue || moveFromY != Int.MaxValue || moveFromZ != Int.MaxValue)

  def isAnimatingSwing = animationTicksLeft > 0 && swingingTool

  def isAnimatingTurn = animationTicksLeft > 0 && turnAxis != 0

  def animateSwing(duration: Double) = {
    setAnimateSwing((duration * 20).toInt)
    ServerPacketSender.sendRobotAnimateSwing(this)
  }

  def animateTurn(clockwise: Boolean, duration: Double) = {
    setAnimateTurn(if (clockwise) 1 else -1, (duration * 20).toInt)
    ServerPacketSender.sendRobotAnimateTurn(this)
  }

  def setAnimateMove(fromX: Int, fromY: Int, fromZ: Int, ticks: Int) {
    animationTicksTotal = ticks
    prepareForAnimation()
    moveFromX = fromX
    moveFromY = fromY
    moveFromZ = fromZ
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
    moveFromX = Int.MaxValue
    moveFromY = Int.MaxValue
    moveFromZ = Int.MaxValue
    swingingTool = false
    turnAxis = 0
  }

  // ----------------------------------------------------------------------- //

  override def shouldRenderInPass(pass: Int) = true

  override def getRenderBoundingBox =
    getBlockType.getCollisionBoundingBoxFromPool(world, x, y, z).expand(0.5, 0.5, 0.5)

  // ----------------------------------------------------------------------- //

  override def updateEntity() {
    if (!addedToNetwork) {
      addedToNetwork = true
      api.Network.joinNewNetwork(node)
    }
    if (animationTicksLeft > 0) {
      animationTicksLeft -= 1
      if (animationTicksLeft == 0) {
        moveFromX = Int.MaxValue
        moveFromY = Int.MaxValue
        moveFromZ = Int.MaxValue
        swingingTool = false
        turnAxis = 0
      }
    }
    super.updateEntity()
    if (isServer) {
      gpu.update()
      globalBuffer = computer.node.globalBuffer
      globalBufferSize = computer.node.globalBufferSize
      updatePowerInformation()
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
      updateXpInfo()
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
    buffer.load(nbt.getCompoundTag(Settings.namespace + "buffer"))
    gpu.load(nbt.getCompoundTag(Settings.namespace + "gpu"))
    keyboard.load(nbt.getCompoundTag(Settings.namespace + "keyboard"))
    if (nbt.hasKey(Settings.namespace + "owner")) {
      owner = nbt.getString(Settings.namespace + "owner")
    }
    xp = nbt.getDouble(Settings.namespace + "xp") max 0
    updateXpInfo()
    selectedSlot = nbt.getInteger(Settings.namespace + "selectedSlot") max actualSlot(0) min (getSizeInventory - 1)
    animationTicksTotal = nbt.getInteger(Settings.namespace + "animationTicksTotal")
    animationTicksLeft = nbt.getInteger(Settings.namespace + "animationTicksLeft")
    if (animationTicksLeft > 0) {
      moveFromX = nbt.getInteger(Settings.namespace + "moveFromX")
      moveFromY = nbt.getInteger(Settings.namespace + "moveFromY")
      moveFromZ = nbt.getInteger(Settings.namespace + "moveFromZ")
      swingingTool = nbt.getBoolean(Settings.namespace + "swingingTool")
      turnAxis = nbt.getByte(Settings.namespace + "turnAxis")
    }
  }

  override def writeToNBT(nbt: NBTTagCompound) = this.synchronized {
    // Note: computer is saved when proxy is saved (in proxy's super writeToNBT)
    // which is a bit ugly, and may be refactored some day, but it works.
    nbt.setNewCompoundTag(Settings.namespace + "buffer", buffer.save)
    nbt.setNewCompoundTag(Settings.namespace + "gpu", gpu.save)
    nbt.setNewCompoundTag(Settings.namespace + "keyboard", keyboard.save)
    nbt.setString(Settings.namespace + "owner", owner)
    nbt.setDouble(Settings.namespace + "xp", xp)
    nbt.setInteger(Settings.namespace + "selectedSlot", selectedSlot)
    if (isAnimatingMove || isAnimatingSwing || isAnimatingTurn) {
      nbt.setInteger(Settings.namespace + "animationTicksTotal", animationTicksTotal)
      nbt.setInteger(Settings.namespace + "animationTicksLeft", animationTicksLeft)
      nbt.setInteger(Settings.namespace + "moveFromX", moveFromX)
      nbt.setInteger(Settings.namespace + "moveFromY", moveFromY)
      nbt.setInteger(Settings.namespace + "moveFromZ", moveFromZ)
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
    if (nbt.hasKey("upgrade")) {
      equippedUpgrade = Option(ItemStack.loadItemStackFromNBT(nbt.getCompoundTag("upgrade")))
    }
    xp = nbt.getDouble(Settings.namespace + "xp")
    updateXpInfo()
    animationTicksTotal = nbt.getInteger("animationTicksTotal")
    animationTicksLeft = nbt.getInteger("animationTicksLeft")
    moveFromX = nbt.getInteger("moveFromX")
    moveFromY = nbt.getInteger("moveFromY")
    moveFromZ = nbt.getInteger("moveFromZ")
    if (animationTicksLeft > 0) {
      swingingTool = nbt.getBoolean("swingingTool")
      turnAxis = nbt.getByte("turnAxis")
    }
  }

  override def writeToNBTForClient(nbt: NBTTagCompound) = this.synchronized {
    super.writeToNBTForClient(nbt)
    nbt.setInteger("selectedSlot", selectedSlot)
    if (getStackInSlot(0) != null) {
      nbt.setNewCompoundTag("equipped", getStackInSlot(0).writeToNBT)
    }
    if (getStackInSlot(3) != null) {
      // Force saving to item's NBT if necessary before sending, to make sure
      // we transfer the component's current state (e.g. running or not for
      // generator upgrades).
      components(3) match {
        case Some(environment) =>
          val stack = getStackInSlot(3)
          // We're guaranteed to have a driver for entries.
          environment.save(dataTag(Registry.itemDriverFor(stack).get, stack))
        case _ => // See onConnect()
      }
      nbt.setNewCompoundTag("upgrade", getStackInSlot(3).writeToNBT)
    }
    nbt.setDouble(Settings.namespace + "xp", xp)
    if (isAnimatingMove || isAnimatingSwing || isAnimatingTurn) {
      nbt.setInteger("animationTicksTotal", animationTicksTotal)
      nbt.setInteger("animationTicksLeft", animationTicksLeft)
      nbt.setInteger("moveFromX", moveFromX)
      nbt.setInteger("moveFromY", moveFromY)
      nbt.setInteger("moveFromZ", moveFromZ)
      nbt.setBoolean("swingingTool", swingingTool)
      nbt.setByte("turnAxis", turnAxis.toByte)
    }
  }

  // ----------------------------------------------------------------------- //

  override def onConnect(node: Node) {
    super.onConnect(node)
    if (node == this.node) {
      computer.node.connect(buffer.node)
      computer.node.connect(gpu.node)
      buffer.node.connect(keyboard.node)
      // There's a chance the server sends a robot tile entity to its clients
      // before the tile entity's first update was called, in which case the
      // component list isn't initialized (e.g. when a client triggers a chunk
      // load, most noticeable in single player). In that case the current
      // equipment will be initialized incorrectly. So we have to send it
      // again when the first update is run. One of the two (this and the info
      // sent in writeToNBTForClient) may be superfluous, but the packet is
      // quite small compared to what else is sent on a chunk load, so we don't
      // really worry about it and just send it.
      saveUpgrade()
    }
  }

  override def onDisconnect(node: Node) {
    super.onDisconnect(node)
    if (node == this.node) {
      buffer.node.remove()
      computer.node.remove()
      gpu.node.remove()
      keyboard.node.remove()
    }
  }

  // ----------------------------------------------------------------------- //

  override protected def onItemRemoved(slot: Int, stack: ItemStack) {
    super.onItemRemoved(slot, stack)
    if (isServer) {
      if (isToolSlot(slot)) {
        player_.getAttributeMap.removeAttributeModifiers(stack.getAttributeModifiers)
        ServerPacketSender.sendRobotEquippedItemChange(this, null)
      }
      if (isUpgradeSlot(slot)) {
        ServerPacketSender.sendRobotEquippedUpgradeChange(this, null)
      }
      if (isInventorySlot(slot)) {
        computer.signal("inventory_changed", Int.box(slot - actualSlot(0) + 1))
      }
    }
  }

  override protected def onItemAdded(slot: Int, stack: ItemStack) {
    if (isServer) {
      if (isToolSlot(slot)) {
        player_.getAttributeMap.applyAttributeModifiers(stack.getAttributeModifiers)
        ServerPacketSender.sendRobotEquippedItemChange(this, getStackInSlot(slot))
      }
      if (isUpgradeSlot(slot)) {
        ServerPacketSender.sendRobotEquippedUpgradeChange(this, getStackInSlot(slot))
      }
      if (isComponentSlot(slot)) {
        super.onItemAdded(slot, stack)
      }
      if (isInventorySlot(slot)) {
        computer.signal("inventory_changed", Int.box(slot - actualSlot(0) + 1))
      }
    }
  }

  override protected def isComponentSlot(slot: Int) = slot > 0 && slot < actualSlot(0)

  private def isInventorySlot(slot: Int) = slot >= actualSlot(0)

  private def isToolSlot(slot: Int) = slot == 0

  private def isUpgradeSlot(slot: Int) = slot == 3

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
    if ((1 until actualSlot(0) contains slot) && stack != null && stack.stackSize > 1) {
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

  def isItemValidForSlot(slot: Int, stack: ItemStack) = (slot, Registry.itemDriverFor(stack)) match {
    case (0, _) => true // Allow anything in the tool slot.
    case (1, Some(driver)) => driver.slot(stack) == Slot.Card && driver.tier(stack) < 2
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
      case ForgeDirection.NORTH => Array(2, 3)
      case _ => (actualSlot(0) until getSizeInventory).toArray
    }
}

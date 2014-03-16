package li.cil.oc.common.tileentity

import cpw.mods.fml.relauncher.{SideOnly, Side}
import java.util.logging.Level
import li.cil.oc._
import li.cil.oc.api.driver.Slot
import li.cil.oc.api.network._
import li.cil.oc.common.block.Delegator
import li.cil.oc.server.component.GraphicsCard
import li.cil.oc.server.component.robot
import li.cil.oc.server.driver.Registry
import li.cil.oc.server.{PacketSender => ServerPacketSender, driver, component}
import li.cil.oc.util.ExtendedNBT._
import net.minecraft.client.Minecraft
import net.minecraft.entity.player.EntityPlayer
import net.minecraft.inventory.ISidedInventory
import net.minecraft.item.ItemStack
import net.minecraft.nbt.NBTTagCompound
import net.minecraft.util.ChatMessageComponent
import net.minecraftforge.common.ForgeDirection
import scala.io.Source

// Implementation note: this tile entity is never directly added to the world.
// It is always wrapped by a `RobotProxy` tile entity, which forwards any
// necessary calls to this class. This is done to make moves efficient: when a
// robot moves we only create a new proxy tile entity, hook the instance of this
// class that was held by the old proxy to it and can then safely forget the
// old proxy, which will be cleaned up by Minecraft like any other tile entity.
class Robot(val isRemote: Boolean) extends Computer with ISidedInventory with Buffer with PowerInformation with api.machine.Robot {
  def this() = this(false)

  if (isServer) {
    computer.setCostPerTick(Settings.get.robotCost)
  }

  var proxy: RobotProxy = _

  // ----------------------------------------------------------------------- //

  // Note: we implement IRobotContext in the TE to allow external components
  //to cast their owner to it (to allow interacting with their owning robot).

  var selectedSlot = actualSlot(0)

  override def player() = player(facing, facing)

  def name: String = {
    if (tag != null && tag.hasKey("display")) {
      val display = tag.getCompoundTag("display")
      if (display != null && display.hasKey("Name")) {
        return display.getString("Name")
      }
    }
    null
  }

  override def saveUpgrade() = this.synchronized {
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

  override def node = if (isServer) computer.node else null

  override val _buffer = new common.component.Buffer(this) {
    override def maxResolution = (48, 14)
  }
  val (bot, gpu, keyboard) = if (isServer) {
    val bot = new robot.Robot(this)
    val gpu = new GraphicsCard.Tier1 {
      override val maxResolution = (48, 14)
    }
    val keyboard = new component.Keyboard {
      override def isUseableByPlayer(p: EntityPlayer) =
        world.getBlockTileEntity(x, y, z) == proxy &&
          p.getDistanceSq(x + 0.5, y + 0.5, z + 0.5) <= 64
    }
    (bot, gpu, keyboard)
  }
  else (null, null, null)

  var owner = "OpenComputers"

  var tag: NBTTagCompound = _

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
      bot.node.setLocalBufferSize(Settings.get.bufferRobot + Settings.get.bufferPerLevel * level)
    }
  }

  override def maxComponents = 8

  // ----------------------------------------------------------------------- //

  override def onAnalyze(player: EntityPlayer, side: Int, hitX: Float, hitY: Float, hitZ: Float) = {
    player.sendChatToPlayer(ChatMessageComponent.createFromTranslationWithSubstitutions(
      Settings.namespace + "gui.Analyzer.RobotOwner", owner))
    player.sendChatToPlayer(ChatMessageComponent.createFromTranslationWithSubstitutions(
      Settings.namespace + "gui.Analyzer.RobotName", player_.getCommandSenderName))
    player.sendChatToPlayer(ChatMessageComponent.createFromTranslationWithSubstitutions(
      Settings.namespace + "gui.Analyzer.RobotXp", xp.formatted("%.2f"), level: Integer))
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
      // In some cases (though I couldn't quite figure out which one) setBlock
      // will return true, even though the block was not created / adjusted.
      val created = Blocks.robotProxy.setBlock(world, nx, ny, nz, 1) &&
        world.getBlockTileEntity(nx, ny, nz) == proxy
      if (created) {
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
    val tag = if (this.tag != null) this.tag.copy.asInstanceOf[NBTTagCompound] else new NBTTagCompound("tag")
    stack.setTagCompound(tag)
    if (xp > 0) {
      tag.setDouble(Settings.namespace + "xp", xp)
    }
    if (globalBuffer > 1) {
      tag.setInteger(Settings.namespace + "storedEnergy", globalBuffer.toInt)
    }
    stack
  }

  def parseItemStack(stack: ItemStack) {
    if (stack.hasTagCompound) {
      tag = stack.getTagCompound.copy.asInstanceOf[NBTTagCompound]
      xp = tag.getDouble(Settings.namespace + "xp")
      updateXpInfo()
      bot.node.changeBuffer(stack.getTagCompound.getInteger(Settings.namespace + "storedEnergy"))
    }
    else {
      tag = new NBTTagCompound("tag")
    }
    if (name == null) {
      tag.setNewCompoundTag("display", tag => tag.setString("Name", Robot.randomName))
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
    if (isServer && !addedToNetwork) {
      addedToNetwork = true
      api.Network.joinNewNetwork(node)
      // For upgrading from when the energy was stored by the machine's node.
      node.asInstanceOf[Connector].setLocalBufferSize(0)
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
      globalBuffer = bot.node.globalBuffer
      globalBufferSize = bot.node.globalBufferSize
      updatePowerInformation()
      if (xpChanged && world.getWorldInfo.getWorldTotalTime % 200 == 0) {
        xpChanged = false
        ServerPacketSender.sendRobotXp(this)
      }
    }
    else if (isRunning && isAnimatingMove) {
      client.Sound.updatePosition(this)
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
    bot.load(nbt.getCompoundTag(Settings.namespace + "robot"))
    if (nbt.hasKey(Settings.namespace + "owner")) {
      owner = nbt.getString(Settings.namespace + "owner")
    }
    if (nbt.hasKey(Settings.namespace + "tag")) {
      tag = nbt.getCompoundTag(Settings.namespace + "tag")
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

  // Side check for Waila (and other mods that may call this client side).
  override def writeToNBT(nbt: NBTTagCompound) = if (isServer) this.synchronized {
    // Note: computer is saved when proxy is saved (in proxy's super writeToNBT)
    // which is a bit ugly, and may be refactored some day, but it works.
    nbt.setNewCompoundTag(Settings.namespace + "buffer", buffer.save)
    nbt.setNewCompoundTag(Settings.namespace + "gpu", gpu.save)
    nbt.setNewCompoundTag(Settings.namespace + "keyboard", keyboard.save)
    nbt.setNewCompoundTag(Settings.namespace + "robot", bot.save)
    nbt.setString(Settings.namespace + "owner", owner)
    if (tag != null) {
      nbt.setCompoundTag(Settings.namespace + "tag", tag)
    }
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
    if (nbt.hasKey(Settings.namespace + "tag")) {
      tag = nbt.getCompoundTag(Settings.namespace + "tag")
    }
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
    if (tag != null) {
      nbt.setCompoundTag(Settings.namespace + "tag", tag)
    }
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

  override def onMachineConnect(node: Node) {
    super.onConnect(node)
    if (node == this.node) {
      node.connect(bot.node)
      node.connect(buffer.node)
      node.connect(gpu.node)
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

  override def onMachineDisconnect(node: Node) {
    super.onDisconnect(node)
    if (node == this.node) {
      buffer.node.remove()
      node.remove()
      gpu.node.remove()
      keyboard.node.remove()
      bot.node.remove()
    }
  }

  // ----------------------------------------------------------------------- //

  override protected def onItemAdded(slot: Int, stack: ItemStack) {
    if (isServer) {
      if (isToolSlot(slot)) {
        player_.getAttributeMap.applyAttributeModifiers(stack.getAttributeModifiers)
        ServerPacketSender.sendRobotEquippedItemChange(this, getStackInSlot(slot))
      }
      if (isUpgradeSlot(slot)) {
        ServerPacketSender.sendRobotEquippedUpgradeChange(this, getStackInSlot(slot))
      }
      if (isFloppySlot(slot)) {
        common.Sound.playDiskInsert(this)
      }
      if (isComponentSlot(slot)) {
        super.onItemAdded(slot, stack)
      }
      if (isInventorySlot(slot)) {
        computer.signal("inventory_changed", Int.box(slot - actualSlot(0) + 1))
      }
    }
  }

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
      if (isFloppySlot(slot)) {
        common.Sound.playDiskEject(this)
      }
      if (isInventorySlot(slot)) {
        computer.signal("inventory_changed", Int.box(slot - actualSlot(0) + 1))
      }
    }
  }

  override protected def isComponentSlot(slot: Int) = slot > 0 && slot < actualSlot(0)

  private def isInventorySlot(slot: Int) = slot >= actualSlot(0)

  private def isToolSlot(slot: Int) = slot == 0

  private def isFloppySlot(slot: Int) = slot == 2

  private def isUpgradeSlot(slot: Int) = slot == 3

  // ----------------------------------------------------------------------- //

  override def installedMemory = 96 * 1024

  override def tier = 0

  override def hasRedstoneCard = items(1).fold(false)(driver.item.RedstoneCard.worksWith)

  @SideOnly(Side.CLIENT)
  override protected def markForRenderUpdate() {
    super.markForRenderUpdate()
    currentGui.foreach(_.recompileDisplayLists())
  }

  // ----------------------------------------------------------------------- //

  override def getInvName = Settings.namespace + "container.Robot"

  override def getSizeInventory = 20

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

  override def isItemValidForSlot(slot: Int, stack: ItemStack) = (slot, Registry.itemDriverFor(stack)) match {
    case (0, _) => true // Allow anything in the tool slot.
    case (1, Some(driver)) => driver.slot(stack) == Slot.Card && driver.tier(stack) < 2
    case (2, Some(driver)) => driver.slot(stack) == Slot.Disk
    case (3, Some(driver)) => driver.slot(stack) == Slot.Upgrade
    case (i, _) if actualSlot(0) until getSizeInventory contains i => true // Normal inventory.
    case _ => false // Invalid slot.
  }

  // ----------------------------------------------------------------------- //

  override def canExtractItem(slot: Int, stack: ItemStack, side: Int) =
    getAccessibleSlotsFromSide(side).contains(slot)

  override def canInsertItem(slot: Int, stack: ItemStack, side: Int) =
    getAccessibleSlotsFromSide(side).contains(slot) &&
      isItemValidForSlot(slot, stack)

  override def getAccessibleSlotsFromSide(side: Int) =
    toLocal(ForgeDirection.getOrientation(side)) match {
      case ForgeDirection.WEST => Array(0)
      case ForgeDirection.EAST => Array(1)
      case ForgeDirection.NORTH => Array(2, 3)
      case _ => (actualSlot(0) until getSizeInventory).toArray
    }
}

object Robot {
  val names = try {
    Source.fromInputStream(getClass.getResourceAsStream(
      "/assets/" + Settings.resourceDomain + "/robot.names"))("UTF-8").
      getLines().map(_.trim).filter(!_.startsWith("#")).filter(_ != "").toArray
  }
  catch {
    case t: Throwable =>
      OpenComputers.log.log(Level.WARNING, "Failed loading robot name list.", t)
      Array.empty[String]
  }

  def randomName = names((math.random * names.length).toInt)
}
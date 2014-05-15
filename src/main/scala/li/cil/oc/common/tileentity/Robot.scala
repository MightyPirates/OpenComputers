package li.cil.oc.common.tileentity

import cpw.mods.fml.relauncher.{SideOnly, Side}
import li.cil.oc._
import li.cil.oc.api.Driver
import li.cil.oc.api.driver.Slot
import li.cil.oc.api.event.{RobotAnalyzeEvent, RobotMoveEvent}
import li.cil.oc.api.network._
import li.cil.oc.client.gui
import li.cil.oc.common.block.Delegator
import li.cil.oc.server.component.robot
import li.cil.oc.server.{PacketSender => ServerPacketSender, driver}
import li.cil.oc.util.ExtendedNBT._
import li.cil.oc.util.ItemUtils
import net.minecraft.block.{BlockFlowing, Block}
import net.minecraft.client.Minecraft
import net.minecraft.entity.player.EntityPlayer
import net.minecraft.item.ItemStack
import net.minecraft.nbt.NBTTagCompound
import net.minecraft.util.ChatMessageComponent
import net.minecraftforge.common.{MinecraftForge, ForgeDirection}
import net.minecraftforge.fluids.{BlockFluidBase, FluidRegistry}
import scala.collection.mutable
import li.cil.oc.common.InventorySlots.Tier

// Implementation note: this tile entity is never directly added to the world.
// It is always wrapped by a `RobotProxy` tile entity, which forwards any
// necessary calls to this class. This is done to make moves efficient: when a
// robot moves we only create a new proxy tile entity, hook the instance of this
// class that was held by the old proxy to it and can then safely forget the
// old proxy, which will be cleaned up by Minecraft like any other tile entity.
class Robot(val isRemote: Boolean) extends traits.Computer with traits.PowerInformation with api.machine.Robot {
  def this() = this(false)

  var proxy: RobotProxy = _

  val info = new ItemUtils.RobotData()

  val bot = if (isServer) new robot.Robot(this) else null

  if (isServer) {
    computer.setCostPerTick(Settings.get.robotCost)
  }

  // ----------------------------------------------------------------------- //

  val actualInventorySize = 275

  var inventorySize = 0

  var selectedSlot = actualSlot(0)

  override def containerCount = info.containers.length

  override def componentCount = info.components.length

  override def getComponentInSlot(index: Int) = components(index).orNull

  override def player() = player(facing, facing)

  override def saveUpgrade() = this.synchronized {
    components(3) match {
      case Some(environment) =>
        val stack = getStackInSlot(3)
        // We're guaranteed to have a driver for entries.
        environment.save(dataTag(Driver.driverFor(stack), stack))
        ServerPacketSender.sendRobotEquippedUpgradeChange(this, stack)
      case _ =>
    }
  }

  def containerSlots = 1 to containerCount

  def componentSlots = getSizeInventory - componentCount until getSizeInventory

  def inventorySlots = actualSlot(0) until actualSlot(0) + inventorySize

  // ----------------------------------------------------------------------- //

  override def node = if (isServer) computer.node else null

  var globalBuffer, globalBufferSize = 0.0

  var maxComponents = 0

  var owner = "OpenComputers"

  var equippedItem: Option[ItemStack] = None

  var equippedUpgrade: Option[ItemStack] = None

  var animationTicksLeft = 0

  var animationTicksTotal = 0

  var moveFromX, moveFromY, moveFromZ = Int.MaxValue

  var swingingTool = false

  var turnAxis = 0

  private lazy val player_ = new robot.Player(this)

  // ----------------------------------------------------------------------- //

  def name = info.name

  override def onAnalyze(player: EntityPlayer, side: Int, hitX: Float, hitY: Float, hitZ: Float) = {
    player.sendChatToPlayer(ChatMessageComponent.createFromTranslationWithSubstitutions(
      Settings.namespace + "gui.Analyzer.RobotOwner", owner))
    player.sendChatToPlayer(ChatMessageComponent.createFromTranslationWithSubstitutions(
      Settings.namespace + "gui.Analyzer.RobotName", player_.getCommandSenderName))
    MinecraftForge.EVENT_BUS.post(new RobotAnalyzeEvent(this, player))
    super.onAnalyze(player, side, hitX, hitY, hitZ)
  }

  def player(facing: ForgeDirection = facing, side: ForgeDirection = facing) = {
    player_.updatePositionAndRotation(facing, side)
    player_
  }

  def actualSlot(n: Int) = n + 1 + containerCount

  def move(direction: ForgeDirection): Boolean = {
    val (ox, oy, oz) = (x, y, z)
    val (nx, ny, nz) = (x + direction.offsetX, y + direction.offsetY, z + direction.offsetZ)
    if (!world.blockExists(nx, ny, nz)) {
      return false // Don't fall off the earth.
    }

    if (isServer) {
      val event = new RobotMoveEvent.Pre(this, direction)
      MinecraftForge.EVENT_BUS.post(event)
      if (event.isCanceled) return false
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
          MinecraftForge.EVENT_BUS.post(new RobotMoveEvent.Post(this, direction))
        }
        else {
          // If we broke some replaceable block (like grass) play its break sound.
          if (blockId > 0) {
            val block = Block.blocksList(blockId)
            if (block != null) {
              if (FluidRegistry.lookupFluidForBlock(block) == null &&
                !block.isInstanceOf[BlockFluidBase] &&
                !block.isInstanceOf[BlockFlowing]) {
                world.playAuxSFX(2001, nx, ny, nz, blockId + (metadata << 12))
              }
              else {
                world.playSound(nx + 0.5, ny + 0.5, nz + 0.5, "liquid.water",
                  world.rand.nextFloat * 0.25F + 0.75F, world.rand.nextFloat * 1.0F + 0.5F, false)
              }
            }
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
      globalBuffer = bot.node.globalBuffer
      globalBufferSize = bot.node.globalBufferSize
      updatePowerInformation()
    }
    else if (isRunning && isAnimatingMove) {
      client.Sound.updatePosition(this)
    }
  }

  override protected def initialize() {
    if (isServer) {
      Option(getStackInSlot(0)) match {
        case Some(item) => player_.getAttributeMap.applyAttributeModifiers(item.getAttributeModifiers)
        case _ =>
      }

      // Ensure we have a node address, because the proxy needs this to initialize
      // its own node to the same address ours has.
      api.Network.joinNewNetwork(node)
    }
  }

  override protected def dispose() {
    super.dispose()
    Minecraft.getMinecraft.currentScreen match {
      case robotGui: gui.Robot if robotGui.robot == this =>
        Minecraft.getMinecraft.displayGuiScreen(null)
      case _ =>
    }
  }

  // ----------------------------------------------------------------------- //

  override def readFromNBT(nbt: NBTTagCompound) {
    info.load(nbt)

    updateInventorySize()
    updateMaxComponentCount()

    bot.load(nbt.getCompoundTag(Settings.namespace + "robot"))
    if (nbt.hasKey(Settings.namespace + "owner")) {
      owner = nbt.getString(Settings.namespace + "owner")
    }
    selectedSlot = nbt.getInteger(Settings.namespace + "selectedSlot") max inventorySlots.min min inventorySlots.max
    animationTicksTotal = nbt.getInteger(Settings.namespace + "animationTicksTotal")
    animationTicksLeft = nbt.getInteger(Settings.namespace + "animationTicksLeft")
    if (animationTicksLeft > 0) {
      moveFromX = nbt.getInteger(Settings.namespace + "moveFromX")
      moveFromY = nbt.getInteger(Settings.namespace + "moveFromY")
      moveFromZ = nbt.getInteger(Settings.namespace + "moveFromZ")
      swingingTool = nbt.getBoolean(Settings.namespace + "swingingTool")
      turnAxis = nbt.getByte(Settings.namespace + "turnAxis")
    }

    // TODO migration: xp to xp upgrade
    // xp = nbt.getDouble(Settings.namespace + "xp") max 0
  }

  // Side check for Waila (and other mods that may call this client side).
  override def writeToNBT(nbt: NBTTagCompound) = if (isServer) this.synchronized {
    info.save(nbt)

    // Note: computer is saved when proxy is saved (in proxy's super writeToNBT)
    // which is a bit ugly, and may be refactored some day, but it works.
    nbt.setNewCompoundTag(Settings.namespace + "robot", bot.save)
    nbt.setString(Settings.namespace + "owner", owner)
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
    info.load(nbt)

    updateInventorySize()

    selectedSlot = nbt.getInteger("selectedSlot")
    if (nbt.hasKey("equipped")) {
      equippedItem = Option(ItemStack.loadItemStackFromNBT(nbt.getCompoundTag("equipped")))
    }
    if (nbt.hasKey("upgrade")) {
      equippedUpgrade = Option(ItemStack.loadItemStackFromNBT(nbt.getCompoundTag("upgrade")))
    }
    animationTicksTotal = nbt.getInteger("animationTicksTotal")
    animationTicksLeft = nbt.getInteger("animationTicksLeft")
    moveFromX = nbt.getInteger("moveFromX")
    moveFromY = nbt.getInteger("moveFromY")
    moveFromZ = nbt.getInteger("moveFromZ")
    if (animationTicksLeft > 0) {
      swingingTool = nbt.getBoolean("swingingTool")
      turnAxis = nbt.getByte("turnAxis")
    }
    connectComponents()
  }

  override def writeToNBTForClient(nbt: NBTTagCompound) = this.synchronized {
    super.writeToNBTForClient(nbt)
    info.save(nbt)
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
          environment.save(dataTag(Driver.driverFor(stack), stack))
        case _ => // See onConnect()
      }
      nbt.setNewCompoundTag("upgrade", getStackInSlot(3).writeToNBT)
    }
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
      node.remove()
      bot.node.remove()
      for (slot <- componentSlots) {
        Option(getComponentInSlot(slot)).foreach(_.node.remove())
      }
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

  override def onInventoryChanged() {
    super.onInventoryChanged()
    updateInventorySize()
    updateMaxComponentCount()
  }

  override protected def connectItemNode(node: Node) {
    super.connectItemNode(node)
    if (node != null) node.host match {
      case buffer: api.component.TextBuffer =>
        buffer.setMaximumResolution(48, 14)
        for (slot <- componentSlots) {
          getComponentInSlot(slot) match {
            case keyboard: api.component.Keyboard => buffer.node.connect(keyboard.node)
            case _ =>
          }
        }
      case keyboard: api.component.Keyboard =>
        for (slot <- componentSlots) {
          getComponentInSlot(slot) match {
            case buffer: api.component.TextBuffer => keyboard.node.connect(buffer.node)
            case _ =>
          }
        }
      case _ =>
    }
  }

  override def isComponentSlot(slot: Int) = (containerSlots ++ componentSlots) contains slot

  def containerSlotType(slot: Int) = if (containerSlots contains slot) {
    val stack = info.containers(slot - 1)
    Option(Driver.driverFor(stack)) match {
      case Some(driver: api.driver.UpgradeContainer) => driver.providedSlot(stack)
      case _ => Slot.None
    }
  }
  else Slot.None

  def containerSlotTier(slot: Int) = if (containerSlots contains slot) {
    val stack = info.containers(slot - 1)
    Option(Driver.driverFor(stack)) match {
      case Some(driver: api.driver.UpgradeContainer) => driver.tier(stack)
      case _ => Tier.None
    }
  }
  else Tier.None

  def isToolSlot(slot: Int) = slot == 0

  def isInventorySlot(slot: Int) = !isToolSlot(slot) && !isComponentSlot(slot)

  def isFloppySlot(slot: Int) = isComponentSlot(slot) && (Option(getStackInSlot(slot)) match {
    case Some(stack) => Option(Driver.driverFor(stack)) match {
      case Some(driver) => driver.slot(stack) == Slot.Disk
      case _ => false
    }
    case _ => false
  })

  def isUpgradeSlot(slot: Int) = false // slot == 3 TODO upgrade synching for rendering

  // ----------------------------------------------------------------------- //

  override def installedMemory = (containerSlots ++ componentSlots).foldLeft(0)((acc, slot) => acc + (Option(getStackInSlot(slot)) match {
    case Some(stack) => Option(Driver.driverFor(stack)) match {
      case Some(driver: api.driver.Memory) => driver.amount(stack)
      case _ => 0
    }
    case _ => 0
  }))

  override def hasRedstoneCard = (containerSlots ++ componentSlots).exists(slot => Option(getStackInSlot(slot)).fold(false)(driver.item.RedstoneCard.worksWith))

  private def computeInventorySize() = math.min(256, (containerSlots ++ componentSlots).foldLeft(0)((acc, slot) => acc + (Option(getStackInSlot(slot)) match {
    case Some(stack) => Option(Driver.driverFor(stack)) match {
      case Some(driver: api.driver.Inventory) => driver.inventoryCapacity(stack)
      case _ => 0
    }
    case _ => 0
  })))

  private def computeMaxComponents() = (containerSlots ++ componentSlots).foldLeft(0)((sum, slot) => sum + (Option(getStackInSlot(slot)) match {
    case Some(stack) => Option(Driver.driverFor(stack)) match {
      case Some(driver: api.driver.Processor) => driver.supportedComponents(stack)
      case _ => 0
    }
    case _ => 0
  }))

  private var updatingInventorySize = false

  private def updateInventorySize() = this.synchronized(if (!updatingInventorySize) try {
    updatingInventorySize = true
    inventorySize = computeInventorySize()
    val realSize = 1 + containerCount + inventorySize
    val oldSelected = selectedSlot - actualSlot(0)
    val removed = mutable.ArrayBuffer.empty[ItemStack]
    for (slot <- realSize until getSizeInventory - componentCount) {
      val stack = getStackInSlot(slot)
      setInventorySlotContents(slot, null)
      if (stack != null) removed += stack
    }
    Array.copy(components, getSizeInventory - componentCount, components, realSize, componentCount)
    getSizeInventory = realSize + componentCount
    if (world != null) {
      val p = player()
      for (stack <- removed) {
        p.inventory.addItemStackToInventory(stack)
        p.dropPlayerItemWithRandomChoice(stack, inPlace = false)
      }
    } // else: save is screwed and we potentially lose items. Life is hard.
    selectedSlot = math.max(1 + containerCount, math.min(realSize - 1, actualSlot(oldSelected)))
  }
  finally {
    updatingInventorySize = false
  })

  private def updateMaxComponentCount() {
    maxComponents = computeMaxComponents()
  }

  // ----------------------------------------------------------------------- //

  override def getInvName = Settings.namespace + "container.Robot"

  var getSizeInventory = actualInventorySize

  override def getInventoryStackLimit = 64

  override def getStackInSlot(slot: Int) = {
    if (slot >= getSizeInventory) null // Required to always show 16 inventory slots in GUI.
    else if (slot >= getSizeInventory - componentCount) {
      info.components(slot - (getSizeInventory - componentCount))
    }
    else super.getStackInSlot(slot)
  }

  override def setInventorySlotContents(slot: Int, stack: ItemStack) {
    if (slot < getSizeInventory - componentCount) {
      if (stack != null && stack.stackSize > 1 && isComponentSlot(slot)) {
        super.setInventorySlotContents(slot, stack.splitStack(1))
        if (stack.stackSize > 0 && isServer) {
          val p = player()
          p.inventory.addItemStackToInventory(stack)
          p.dropPlayerItemWithRandomChoice(stack, inPlace = false)
        }
      }
      else super.setInventorySlotContents(slot, stack)
    }
  }

  override def isUseableByPlayer(player: EntityPlayer) =
    world.getBlockTileEntity(x, y, z) match {
      case t: RobotProxy if t == proxy && computer.canInteract(player.getCommandSenderName) =>
        player.getDistanceSq(x + 0.5, y + 0.5, z + 0.5) <= 64
      case _ => false
    }

  override def isItemValidForSlot(slot: Int, stack: ItemStack) = (slot, Option(Driver.driverFor(stack))) match {
    case (0, _) => true // Allow anything in the tool slot.
    case (i, Some(driver)) if containerSlots contains i => driver.slot(stack) == containerSlotType(i) && driver.tier(stack) <= containerSlotTier(i)
    case (i, _) if isInventorySlot(i) => true // Normal inventory.
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
      case ForgeDirection.WEST => Array(0) // Tool
      case ForgeDirection.EAST => containerSlots.toArray
      case _ => inventorySlots.toArray
    }
}
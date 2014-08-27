package li.cil.oc.common.tileentity

import cpw.mods.fml.relauncher.{Side, SideOnly}
import li.cil.oc._
import li.cil.oc.api.Driver
import li.cil.oc.api.event.{RobotAnalyzeEvent, RobotMoveEvent}
import li.cil.oc.api.network._
import li.cil.oc.client.gui
import li.cil.oc.common.block.Delegator
import li.cil.oc.common.{Slot, Tier}
import li.cil.oc.server.component.robot
import li.cil.oc.server.component.robot.Inventory
import li.cil.oc.server.{driver, PacketSender => ServerPacketSender}
import li.cil.oc.util.ExtendedNBT._
import li.cil.oc.util.ItemUtils
import net.minecraft.block.{Block, BlockFlowing}
import net.minecraft.client.Minecraft
import net.minecraft.entity.player.EntityPlayer
import net.minecraft.item.ItemStack
import net.minecraft.nbt.NBTTagCompound
import net.minecraftforge.common.{ForgeDirection, MinecraftForge}
import net.minecraftforge.fluids.{BlockFluidBase, FluidRegistry}

import scala.collection.mutable

// Implementation note: this tile entity is never directly added to the world.
// It is always wrapped by a `RobotProxy` tile entity, which forwards any
// necessary calls to this class. This is done to make moves efficient: when a
// robot moves we only create a new proxy tile entity, hook the instance of this
// class that was held by the old proxy to it and can then safely forget the
// old proxy, which will be cleaned up by Minecraft like any other tile entity.
class Robot extends traits.Computer with traits.PowerInformation with api.machine.Robot {
  var proxy: RobotProxy = _

  val info = new ItemUtils.RobotData()

  val bot = if (isServer) new robot.Robot(this) else null

  val inventory = new Inventory(this)

  if (isServer) {
    computer.setCostPerTick(Settings.get.robotCost)
  }

  // ----------------------------------------------------------------------- //

  val actualInventorySize = 86

  def maxInventorySize = actualInventorySize - 1 - containerCount - componentCount

  var inventorySize = -1

  var selectedSlot = actualSlot(0)

  // For client.
  var renderingErrored = false

  // Fixed number of containers (mostly due to GUI limitation, but also because
  // I find three to be a large enough number for sufficient flexibility).
  override def containerCount = 3

  override def componentCount = info.components.length

  override def getComponentInSlot(index: Int) = components(index).orNull

  override def player() = player(facing, facing)

  override def synchronizeSlot(slot: Int) = if (slot >= 0 && slot < getSizeInventory) this.synchronized {
    val stack = getStackInSlot(slot)
    components(slot) match {
      case Some(component) =>
        // We're guaranteed to have a driver for entries.
        save(component, Driver.driverFor(stack), stack)
      case _ =>
    }
    ServerPacketSender.sendRobotInventory(this, slot, stack)
  }

  def containerSlots = 1 to info.containers.length

  def componentSlots = getSizeInventory - componentCount until getSizeInventory

  def inventorySlots = actualSlot(0) until actualSlot(0) + inventorySize

  // ----------------------------------------------------------------------- //

  override def node = if (isServer) computer.node else null

  var globalBuffer, globalBufferSize = 0.0

  var maxComponents = 0

  var owner = "OpenComputers"

  var animationTicksLeft = 0

  var animationTicksTotal = 0

  var moveFromX, moveFromY, moveFromZ = Int.MaxValue

  var swingingTool = false

  var turnAxis = 0

  var appliedToolEnchantments = false

  private lazy val player_ = new robot.Player(this)

  // ----------------------------------------------------------------------- //

  def name = info.name

  override def onAnalyze(player: EntityPlayer, side: Int, hitX: Float, hitY: Float, hitZ: Float) = {
    player.sendChatToPlayer(Localization.Analyzer.RobotOwner(owner))
    player.sendChatToPlayer(Localization.Analyzer.RobotName(player_.getCommandSenderName))
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
            if (block != null && !Delegator.subBlock(block, metadata).exists(_ == Blocks.robotAfterimage)) {
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
      else {
        world.setBlockToAir(nx, ny, nz)
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

  def animateSwing(duration: Double) = if (items(0).isDefined) {
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
      if (world.getTotalWorldTime % Settings.get.tickFrequency == 0) {
        if (info.tier == 3) {
          bot.node.changeBuffer(Double.PositiveInfinity)
        }
        globalBuffer = bot.node.globalBuffer
        globalBufferSize = bot.node.globalBufferSize
        info.totalEnergy = globalBuffer.toInt
        info.robotEnergy = bot.node.localBuffer.toInt
        updatePowerInformation()
      }
      if (!appliedToolEnchantments) {
        appliedToolEnchantments = true
        Option(getStackInSlot(0)) match {
          case Some(item) => player_.getAttributeMap.applyAttributeModifiers(item.getAttributeModifiers)
          case _ =>
        }
      }
    }
    else if (isRunning && isAnimatingMove) {
      client.Sound.updatePosition(this)
    }
    inventory.decrementAnimations()
  }

  override protected def initialize() {
    if (isServer) {
      // Ensure we have a node address, because the proxy needs this to initialize
      // its own node to the same address ours has.
      api.Network.joinNewNetwork(node)

      // Flush excess energy to other components (mostly relevant for upgrading
      // robots from 1.2 to 1.3, to move energy to the experience upgrade).
      bot.node.setLocalBufferSize(bot.node.localBufferSize)
    }
  }

  override def dispose() {
    super.dispose()
    if (isClient) {
      Minecraft.getMinecraft.currentScreen match {
        case robotGui: gui.Robot if robotGui.robot == this =>
          Minecraft.getMinecraft.displayGuiScreen(null)
        case _ =>
      }
    }
  }

  // ----------------------------------------------------------------------- //

  override def readFromNBT(nbt: NBTTagCompound) {
    info.load(nbt)

    updateInventorySize()
    updateMaxComponentCount()
    computer.architecture.recomputeMemory()

    bot.load(nbt.getCompoundTag(Settings.namespace + "robot"))
    if (nbt.hasKey(Settings.namespace + "owner")) {
      owner = nbt.getString(Settings.namespace + "owner")
    }
    if (inventorySize > 0) {
      selectedSlot = nbt.getInteger(Settings.namespace + "selectedSlot") max inventorySlots.min min inventorySlots.max
    }
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
    load(nbt)
    info.load(nbt)

    updateInventorySize()

    selectedSlot = nbt.getInteger("selectedSlot")
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
    save(nbt)
    info.save(nbt)

    nbt.setInteger("selectedSlot", selectedSlot)
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
      node.asInstanceOf[Connector].setLocalBufferSize(0)
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
        ServerPacketSender.sendRobotInventory(this, slot, stack)
      }
      if (isUpgradeSlot(slot)) {
        ServerPacketSender.sendRobotInventory(this, slot, stack)
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
        ServerPacketSender.sendRobotInventory(this, slot, null)
      }
      if (isUpgradeSlot(slot)) {
        ServerPacketSender.sendRobotInventory(this, slot, null)
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
    renderingErrored = false
  }

  override protected def connectItemNode(node: Node) {
    super.connectItemNode(node)
    if (node != null) node.host match {
      case buffer: api.component.TextBuffer =>
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
      case Some(driver: api.driver.UpgradeContainer) => Slot.fromApi(driver.providedSlot(stack))
      case _ => Slot.None
    }
  }
  else Slot.None

  def containerSlotTier(slot: Int) = if (containerSlots contains slot) {
    val stack = info.containers(slot - 1)
    Option(Driver.driverFor(stack)) match {
      case Some(driver: api.driver.UpgradeContainer) => driver.providedTier(stack)
      case _ => Tier.None
    }
  }
  else Tier.None

  def isToolSlot(slot: Int) = slot == 0

  def isContainerSlot(slot: Int) = containerSlots contains slot

  def isInventorySlot(slot: Int) = inventorySlots contains slot

  def isFloppySlot(slot: Int) = isComponentSlot(slot) && (Option(getStackInSlot(slot)) match {
    case Some(stack) => Option(Driver.driverFor(stack)) match {
      case Some(driver) => Slot.fromApi(driver.slot(stack)) == Slot.Floppy
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

  private def computeInventorySize() = math.min(maxInventorySize, (containerSlots ++ componentSlots).foldLeft(0)((acc, slot) => acc + (Option(getStackInSlot(slot)) match {
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

  def updateInventorySize() = this.synchronized(if (!updatingInventorySize) try {
    updatingInventorySize = true
    val newInventorySize = computeInventorySize()
    if (newInventorySize != inventorySize) {
      inventorySize = newInventorySize
      val realSize = 1 + containerCount + inventorySize
      val oldSelected = selectedSlot - actualSlot(0)
      val removed = mutable.ArrayBuffer.empty[ItemStack]
      for (slot <- realSize until getSizeInventory - componentCount) {
        val stack = getStackInSlot(slot)
        setInventorySlotContents(slot, null)
        if (stack != null) removed += stack
      }
      Array.copy(components, getSizeInventory - componentCount, components, realSize, componentCount)
      for (slot <- getSizeInventory - componentCount until getSizeInventory if slot < realSize || slot >= realSize + componentCount) {
        components(slot) = None
      }
      getSizeInventory = realSize + componentCount
      if (world != null && isServer) {
        for (stack <- removed) {
          inventory.addItemStackToInventory(stack)
          spawnStackInWorld(stack, facing)
        }
      } // else: save is screwed and we potentially lose items. Life is hard.
      selectedSlot = math.max(actualSlot(0), math.min(actualSlot(inventorySize) - 1, actualSlot(oldSelected)))
    }
  }
  finally {
    updatingInventorySize = false
  })

  def updateMaxComponentCount() {
    maxComponents = computeMaxComponents()
  }

  // ----------------------------------------------------------------------- //

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
          inventory.addItemStackToInventory(stack)
          spawnStackInWorld(stack, facing)
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
    case (i, Some(driver)) if isContainerSlot(i) =>
      // Yay special cases! Dynamic screens kind of work, but are pretty derpy
      // because the item gets send around on changes, including the screen
      // state, which leads to weird effects. Also, it's really illogical that
      // a screen (and keyboard) could be attached to the robot on the fly.
      // Since these are very special (as they have special behavior in the
      // GUI) I feel it's OK to handle it like this, instead of some extra API
      // logic making the differentiation of assembler and containers generic.
      driver != server.driver.item.Screen &&
        driver != server.driver.item.Keyboard &&
        Slot.fromApi(driver.slot(stack)) == containerSlotType(i) &&
        driver.tier(stack) <= containerSlotTier(i)
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
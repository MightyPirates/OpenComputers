package li.cil.oc.common.tileentity

import java.util.UUID

import li.cil.oc._
import li.cil.oc.api.Driver
import li.cil.oc.api.driver.item
import li.cil.oc.api.driver.item.Container
import li.cil.oc.api.event.RobotAnalyzeEvent
import li.cil.oc.api.event.RobotMoveEvent
import li.cil.oc.api.internal
import li.cil.oc.api.network._
import li.cil.oc.client.gui
import li.cil.oc.common.EventHandler
import li.cil.oc.common.Slot
import li.cil.oc.common.Tier
import li.cil.oc.common.inventory.InventoryProxy
import li.cil.oc.common.inventory.InventorySelection
import li.cil.oc.common.inventory.TankSelection
import li.cil.oc.common.item.data.RobotData
import li.cil.oc.integration.opencomputers.DriverKeyboard
import li.cil.oc.integration.opencomputers.DriverRedstoneCard
import li.cil.oc.integration.opencomputers.DriverScreen
import li.cil.oc.server.agent
import li.cil.oc.server.component
import li.cil.oc.server.{PacketSender => ServerPacketSender}
import li.cil.oc.util.BlockPosition
import li.cil.oc.util.ExtendedNBT._
import li.cil.oc.util.ExtendedWorld._
import li.cil.oc.util.InventoryUtils
import net.minecraft.block.Block
import net.minecraft.block.BlockLiquid
import net.minecraft.client.Minecraft
import net.minecraft.entity.player.EntityPlayer
import net.minecraft.init.Blocks
import net.minecraft.item.ItemStack
import net.minecraft.nbt.NBTTagCompound
import net.minecraft.util.AxisAlignedBB
import net.minecraft.util.BlockPos
import net.minecraft.util.EnumFacing
import net.minecraftforge.common.MinecraftForge
import net.minecraftforge.fluids._
import net.minecraftforge.fml.relauncher.Side
import net.minecraftforge.fml.relauncher.SideOnly

import scala.collection.mutable

// Implementation note: this tile entity is never directly added to the world.
// It is always wrapped by a `RobotProxy` tile entity, which forwards any
// necessary calls to this class. This is done to make moves efficient: when a
// robot moves we only create a new proxy tile entity, hook the instance of this
// class that was held by the old proxy to it and can then safely forget the
// old proxy, which will be cleaned up by Minecraft like any other tile entity.
class Robot extends traits.Computer with traits.PowerInformation with traits.RotatableTile with IFluidHandler with internal.Robot with InventorySelection with TankSelection {
  var proxy: RobotProxy = _

  val info = new RobotData()

  val bot = if (isServer) new component.Robot(this) else null

  if (isServer) {
    machine.setCostPerTick(Settings.get.robotCost)
  }

  // ----------------------------------------------------------------------- //

  override def tier = info.tier

  def isCreative = tier == Tier.Four

  val equipmentInventory = new InventoryProxy {
    override def inventory = Robot.this

    override def getSizeInventory = 4
  }

  // Wrapper for the part of the inventory that is mutable.
  val mainInventory = new InventoryProxy {
    override def inventory = Robot.this

    override def getSizeInventory = Robot.this.inventorySize

    override def offset = equipmentInventory.getSizeInventory
  }

  val actualInventorySize = 100

  def maxInventorySize = actualInventorySize - equipmentInventory.getSizeInventory - componentCount

  var inventorySize = -1

  var selectedSlot = 0

  override def setSelectedSlot(index: Int): Unit = {
    selectedSlot = index max 0 min mainInventory.getSizeInventory - 1
    if (world != null) {
      ServerPacketSender.sendRobotSelectedSlotChange(this)
    }
  }

  val tank = new internal.MultiTank {
    override def tankCount = Robot.this.tankCount

    override def getFluidTank(index: Int) = Robot.this.getFluidTank(index)
  }

  var selectedTank = 0

  override def setSelectedTank(index: Int): Unit = selectedTank = index

  // For client.
  var renderingErrored = false

  override def componentCount = info.components.length

  override def getComponentInSlot(index: Int) = components(index).orNull

  override def player = {
    agent.Player.updatePositionAndRotation(player_, facing, facing)
    player_
  }

  override def synchronizeSlot(slot: Int) = if (slot >= 0 && slot < getSizeInventory) this.synchronized {
    val stack = getStackInSlot(slot)
    components(slot) match {
      case Some(component) =>
        // We're guaranteed to have a driver for entries.
        save(component, Driver.driverFor(stack, getClass), stack)
      case _ =>
    }
    ServerPacketSender.sendRobotInventory(this, slot, stack)
  }

  def containerSlots = 1 to info.containers.length

  def componentSlots = getSizeInventory - componentCount until getSizeInventory

  def inventorySlots: Range = equipmentInventory.getSizeInventory until (equipmentInventory.getSizeInventory + mainInventory.getSizeInventory)

  def setLightColor(value: Int): Unit = {
    info.lightColor = value
    ServerPacketSender.sendRobotLightChange(this)
  }

  override def shouldAnimate = isRunning

  // ----------------------------------------------------------------------- //

  override def node = if (isServer) machine.node else null

  var globalBuffer, globalBufferSize = 0.0

  val maxComponents = 32

  var ownerName = Settings.get.fakePlayerName

  var ownerUUID = Settings.get.fakePlayerProfile.getId

  var animationTicksLeft = 0

  var animationTicksTotal = 0

  var moveFrom: Option[BlockPos] = None

  var swingingTool = false

  var turnAxis = 0

  var appliedToolEnchantments = false

  private lazy val player_ = new agent.Player(this)

  // ----------------------------------------------------------------------- //

  override def name = info.name

  override def setName(name: String): Unit = info.name = name

  override def onAnalyze(player: EntityPlayer, side: EnumFacing, hitX: Float, hitY: Float, hitZ: Float) = {
    player.addChatMessage(Localization.Analyzer.RobotOwner(ownerName))
    player.addChatMessage(Localization.Analyzer.RobotName(player_.getName))
    MinecraftForge.EVENT_BUS.post(new RobotAnalyzeEvent(this, player))
    super.onAnalyze(player, side, hitX, hitY, hitZ)
  }

  def move(direction: EnumFacing): Boolean = {
    val oldPosition = getPos
    val newPosition = oldPosition.offset(direction)
    if (!world.isBlockLoaded(newPosition)) {
      return false // Don't fall off the earth.
    }

    if (isServer) {
      val event = new RobotMoveEvent.Pre(this, direction)
      MinecraftForge.EVENT_BUS.post(event)
      if (event.isCanceled) return false
    }

    val blockRobotProxy = api.Items.get(Constants.BlockName.Robot).block.asInstanceOf[common.block.RobotProxy]
    val blockRobotAfterImage = api.Items.get(Constants.BlockName.RobotAfterimage).block.asInstanceOf[common.block.RobotAfterimage]
    val wasAir = world.isAirBlock(newPosition)
    val state = world.getBlockState(newPosition)
    val block = state.getBlock
    val metadata = block.getMetaFromState(state)
    try {
      // Setting this will make the tile entity created via the following call
      // to setBlock to re-use our "real" instance as the inner object, instead
      // of creating a new one.
      blockRobotProxy.moving.set(Some(this))
      // Do *not* immediately send the change to clients to allow checking if it
      // worked before the client is notified so that we can use the same trick on
      // the client by sending a corresponding packet. This also saves us from
      // having to send the complete state again (e.g. screen buffer) each move.
      world.setBlockToAir(newPosition)
      // In some cases (though I couldn't quite figure out which one) setBlock
      // will return true, even though the block was not created / adjusted.
      val created = world.setBlockState(newPosition, world.getBlockState(oldPosition), 1) &&
        world.getTileEntity(newPosition) == proxy
      if (created) {
        assert(getPos == newPosition)
        world.setBlockState(oldPosition, net.minecraft.init.Blocks.air.getDefaultState, 1)
        world.setBlockState(oldPosition, blockRobotAfterImage.getDefaultState, 1)
        assert(world.getBlockState(oldPosition).getBlock == blockRobotAfterImage)
        // Here instead of Lua callback so that it gets called on client, too.
        val moveTicks = math.max((Settings.get.moveDelay * 20).toInt, 1)
        setAnimateMove(oldPosition, moveTicks)
        if (isServer) {
          ServerPacketSender.sendRobotMove(this, oldPosition, direction)
          checkRedstoneInputChanged()
          MinecraftForge.EVENT_BUS.post(new RobotMoveEvent.Post(this, direction))
        }
        else {
          // If we broke some replaceable block (like grass) play its break sound.
          if (!wasAir) {
            if (block != Blocks.air && block != blockRobotAfterImage) {
              if (FluidRegistry.lookupFluidForBlock(block) == null &&
                !block.isInstanceOf[BlockFluidBase] &&
                !block.isInstanceOf[BlockLiquid]) {
                world.playAuxSFX(2001, newPosition, Block.getIdFromBlock(block) + (metadata << 12))
              }
              else {
                val sx = newPosition.getX + 0.5
                val sy = newPosition.getY + 0.5
                val sz = newPosition.getZ + 0.5
                world.playSound(sx, sy, sz, "liquid.water",
                  world.rand.nextFloat * 0.25f + 0.75f, world.rand.nextFloat * 1.0f + 0.5f, false)
              }
            }
          }
          world.markBlockForUpdate(oldPosition)
          world.markBlockForUpdate(newPosition)
        }
        assert(!isInvalid)
      }
      else {
        world.setBlockToAir(newPosition)
      }
      created
    }
    finally {
      blockRobotProxy.moving.set(None)
    }
  }

  // ----------------------------------------------------------------------- //

  def isAnimatingMove = animationTicksLeft > 0 && moveFrom.isDefined

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

  def setAnimateMove(fromPosition: BlockPos, ticks: Int) {
    animationTicksTotal = ticks + 2
    prepareForAnimation()
    moveFrom = Some(fromPosition)
  }

  def setAnimateSwing(ticks: Int) {
    animationTicksTotal = math.max(ticks, 5)
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
    moveFrom = None
    swingingTool = false
    turnAxis = 0
  }

  // ----------------------------------------------------------------------- //

  override def shouldRenderInPass(pass: Int) = true

  override def getRenderBoundingBox =
    if (getBlockType != null && world != null)
      getBlockType.getCollisionBoundingBox(world, getPos, world.getBlockState(getPos)).expand(0.5, 0.5, 0.5)
    else
      AxisAlignedBB.fromBounds(0, 0, 0, 1, 1, 1)

  // ----------------------------------------------------------------------- //

  override def updateEntity() {
    if (animationTicksLeft > 0) {
      animationTicksLeft -= 1
      if (animationTicksLeft == 0) {
        moveFrom = None
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

    for (slot <- 0 until equipmentInventory.getSizeInventory + mainInventory.getSizeInventory) {
      getStackInSlot(slot) match {
        case stack: ItemStack => try stack.updateAnimation(world, if (!world.isRemote) player_ else null, slot, slot == 0) catch {
          case ignored: NullPointerException => // Client side item updates that need a player instance...
        }
        case _ =>
      }
    }
  }

  // The robot's machine is updated in a tick handler, to avoid delayed tile
  // entity creation when moving, which would screw over all the things...
  override protected def updateComputer(): Unit = {}

  override protected def onRunningChanged(): Unit = {
    super.onRunningChanged()
    if (isRunning) EventHandler.onRobotStart(this)
    else EventHandler.onRobotStopped(this)
  }

  override protected def initialize() {
    if (isServer) {
      // Ensure we have a node address, because the proxy needs this to initialize
      // its own node to the same address ours has.
      api.Network.joinNewNetwork(node)
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
    else EventHandler.onRobotStopped(this)
  }

  // ----------------------------------------------------------------------- //

  override def readFromNBTForServer(nbt: NBTTagCompound) {
    updateInventorySize()
    machine.onHostChanged()

    bot.load(nbt.getCompoundTag(Settings.namespace + "robot"))
    if (nbt.hasKey(Settings.namespace + "owner")) {
      ownerName = nbt.getString(Settings.namespace + "owner")
    }
    if (nbt.hasKey(Settings.namespace + "ownerUuid")) {
      ownerUUID = UUID.fromString(nbt.getString(Settings.namespace + "ownerUuid"))
    }
    if (inventorySize > 0) {
      selectedSlot = nbt.getInteger(Settings.namespace + "selectedSlot") max 0 min mainInventory.getSizeInventory - 1
    }
    selectedTank = nbt.getInteger(Settings.namespace + "selectedTank")
    animationTicksTotal = nbt.getInteger(Settings.namespace + "animationTicksTotal")
    animationTicksLeft = nbt.getInteger(Settings.namespace + "animationTicksLeft")
    if (animationTicksLeft > 0) {
      if (nbt.hasKey(Settings.namespace + "moveFromX")) {
        val moveFromX = nbt.getInteger(Settings.namespace + "moveFromX")
        val moveFromY = nbt.getInteger(Settings.namespace + "moveFromY")
        val moveFromZ = nbt.getInteger(Settings.namespace + "moveFromZ")
        moveFrom = Some(new BlockPos(moveFromX, moveFromY, moveFromZ))
      }
      swingingTool = nbt.getBoolean(Settings.namespace + "swingingTool")
      turnAxis = nbt.getByte(Settings.namespace + "turnAxis")
    }

    // Normally set in superclass, but that's not called directly, only in the
    // robot's proxy instance.
    _isOutputEnabled = hasRedstoneCard
    if (isRunning) EventHandler.onRobotStart(this)
  }

  // Side check for Waila (and other mods that may call this client side).
  override def writeToNBTForServer(nbt: NBTTagCompound) = if (isServer) this.synchronized {
    info.save(nbt)

    // Note: computer is saved when proxy is saved (in proxy's super writeToNBT)
    // which is a bit ugly, and may be refactored some day, but it works.
    nbt.setNewCompoundTag(Settings.namespace + "robot", bot.save)
    nbt.setString(Settings.namespace + "owner", ownerName)
    nbt.setString(Settings.namespace + "ownerUuid", ownerUUID.toString)
    nbt.setInteger(Settings.namespace + "selectedSlot", selectedSlot)
    nbt.setInteger(Settings.namespace + "selectedTank", selectedTank)
    if (isAnimatingMove || isAnimatingSwing || isAnimatingTurn) {
      nbt.setInteger(Settings.namespace + "animationTicksTotal", animationTicksTotal)
      nbt.setInteger(Settings.namespace + "animationTicksLeft", animationTicksLeft)
      moveFrom match {
        case Some(blockPos) =>
          nbt.setInteger(Settings.namespace + "moveFromX", blockPos.getX)
          nbt.setInteger(Settings.namespace + "moveFromY", blockPos.getY)
          nbt.setInteger(Settings.namespace + "moveFromZ", blockPos.getZ)
        case _ =>
      }
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
    if (animationTicksLeft > 0) {
      if (nbt.hasKey("moveFromX")) {
        val moveFromX = nbt.getInteger("moveFromX")
        val moveFromY = nbt.getInteger("moveFromY")
        val moveFromZ = nbt.getInteger("moveFromZ")
        moveFrom = Some(new BlockPos(moveFromX, moveFromY, moveFromZ))
      }
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
      moveFrom match {
        case Some(blockPos) =>
          nbt.setInteger("moveFromX", blockPos.getX)
          nbt.setInteger("moveFromY", blockPos.getY)
          nbt.setInteger("moveFromZ", blockPos.getZ)
        case _ =>
      }
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
      if (isComponentSlot(slot, stack)) {
        super.onItemAdded(slot, stack)
        world.notifyBlocksOfNeighborChange(position, getBlockType)
      }
      if (isInventorySlot(slot)) {
        machine.signal("inventory_changed", Int.box(slot - equipmentInventory.getSizeInventory + 1))
      }
    }
    else super.onItemAdded(slot, stack)
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
        machine.signal("inventory_changed", Int.box(slot - equipmentInventory.getSizeInventory + 1))
      }
      if (isComponentSlot(slot, stack)) {
        world.notifyBlocksOfNeighborChange(position, getBlockType)
      }
    }
  }

  override def markDirty() {
    super.markDirty()
    // Avoid getting into a bad state on the client when updating before we
    // got the descriptor packet from the server. If we manage to open the
    // GUI before the descriptor packet arrived, close it again because it is
    // invalid anyway.
    if (inventorySize >= 0) {
      updateInventorySize()
    }
    else if (isClient) {
      Minecraft.getMinecraft.currentScreen match {
        case robotGui: gui.Robot if robotGui.robot == this =>
          Minecraft.getMinecraft.displayGuiScreen(null)
        case _ =>
      }
    }
    renderingErrored = false
  }

  override protected def connectItemNode(node: Node) {
    super.connectItemNode(node)
    if (node != null) node.host match {
      case buffer: api.internal.TextBuffer =>
        for (slot <- componentSlots) {
          getComponentInSlot(slot) match {
            case keyboard: api.internal.Keyboard => buffer.node.connect(keyboard.node)
            case gpu: li.cil.oc.server.component.GraphicsCard => buffer.node.connect(gpu.node)
            case _ =>
          }
        }
      case keyboard: api.internal.Keyboard =>
        for (slot <- componentSlots) {
          getComponentInSlot(slot) match {
            case buffer: api.internal.TextBuffer => keyboard.node.connect(buffer.node)
            case _ =>
          }
        }
      case _ =>
    }
  }

  override def isComponentSlot(slot: Int, stack: ItemStack) = (containerSlots ++ componentSlots) contains slot

  def containerSlotType(slot: Int) = if (containerSlots contains slot) {
    val stack = info.containers(slot - 1)
    Option(Driver.driverFor(stack, getClass)) match {
      case Some(driver: Container) => driver.providedSlot(stack)
      case _ => Slot.None
    }
  }
  else Slot.None

  def containerSlotTier(slot: Int) = if (containerSlots contains slot) {
    val stack = info.containers(slot - 1)
    Option(Driver.driverFor(stack, getClass)) match {
      case Some(driver: Container) => driver.providedTier(stack)
      case _ => Tier.None
    }
  }
  else Tier.None

  def isToolSlot(slot: Int) = slot == 0

  def isContainerSlot(slot: Int) = containerSlots contains slot

  def isInventorySlot(slot: Int) = inventorySlots contains slot

  def isFloppySlot(slot: Int) = getStackInSlot(slot) != null && isComponentSlot(slot, getStackInSlot(slot)) && {
    val stack = getStackInSlot(slot)
    Option(Driver.driverFor(stack, getClass)) match {
      case Some(driver) => driver.slot(stack) == Slot.Floppy
      case _ => false
    }
  }

  def isUpgradeSlot(slot: Int) = containerSlotType(slot) == Slot.Upgrade

  // ----------------------------------------------------------------------- //

  override def componentSlot(address: String) = components.indexWhere(_.exists(env => env.node != null && env.node.address == address))

  override def hasRedstoneCard = (containerSlots ++ componentSlots).exists(slot => Option(getStackInSlot(slot)).fold(false)(DriverRedstoneCard.worksWith(_, getClass)))

  private def computeInventorySize() = math.min(maxInventorySize, (containerSlots ++ componentSlots).foldLeft(0)((acc, slot) => acc + (Option(getStackInSlot(slot)) match {
    case Some(stack) => Option(Driver.driverFor(stack, getClass)) match {
      case Some(driver: item.Inventory) => driver.inventoryCapacity(stack)
      case _ => 0
    }
    case _ => 0
  })))

  private var updatingInventorySize = false

  def updateInventorySize() = this.synchronized(if (!updatingInventorySize) try {
    updatingInventorySize = true
    val newInventorySize = computeInventorySize()
    if (newInventorySize != inventorySize) {
      inventorySize = newInventorySize
      val realSize = equipmentInventory.getSizeInventory + mainInventory.getSizeInventory
      val oldSelected = selectedSlot
      val removed = mutable.ArrayBuffer.empty[ItemStack]
      for (slot <- realSize until getSizeInventory - componentCount) {
        val stack = getStackInSlot(slot)
        setInventorySlotContents(slot, null)
        if (stack != null) removed += stack
      }
      val copyComponentCount = math.min(getSizeInventory, componentCount)
      Array.copy(components, getSizeInventory - copyComponentCount, components, realSize, copyComponentCount)
      for (slot <- math.max(0, getSizeInventory - componentCount) until getSizeInventory if slot < realSize || slot >= realSize + componentCount) {
        components(slot) = None
      }
      getSizeInventory = realSize + componentCount
      if (world != null && isServer) {
        for (stack <- removed) {
          player().inventory.addItemStackToInventory(stack)
          spawnStackInWorld(stack, Option(facing))
        }
        setSelectedSlot(oldSelected)
      } // else: save is screwed and we potentially lose items. Life is hard.
    }
  }
  finally {
    updatingInventorySize = false
  })

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
    if (slot < getSizeInventory - componentCount && (isItemValidForSlot(slot, stack) || stack == null)) {
      if (stack != null && stack.stackSize > 1 && isComponentSlot(slot, stack)) {
        super.setInventorySlotContents(slot, stack.splitStack(1))
        if (stack.stackSize > 0 && isServer) {
          player().inventory.addItemStackToInventory(stack)
          spawnStackInWorld(stack, Option(facing))
        }
      }
      else super.setInventorySlotContents(slot, stack)
    }
    else if (stack != null && stack.stackSize > 0 && !world.isRemote) spawnStackInWorld(stack, Option(EnumFacing.UP))
  }

  override def isUseableByPlayer(player: EntityPlayer) =
    super.isUseableByPlayer(player) && (!isCreative || player.capabilities.isCreativeMode)

  override def isItemValidForSlot(slot: Int, stack: ItemStack) = (slot, Option(Driver.driverFor(stack, getClass))) match {
    case (0, _) => true // Allow anything in the tool slot.
    case (i, Some(driver)) if isContainerSlot(i) =>
      // Yay special cases! Dynamic screens kind of work, but are pretty derpy
      // because the item gets send around on changes, including the screen
      // state, which leads to weird effects. Also, it's really illogical that
      // a screen (and keyboard) could be attached to the robot on the fly.
      // Since these are very special (as they have special behavior in the
      // GUI) I feel it's OK to handle it like this, instead of some extra API
      // logic making the differentiation of assembler and containers generic.
      driver != DriverScreen &&
        driver != DriverKeyboard &&
        driver.slot(stack) == containerSlotType(i) &&
        driver.tier(stack) <= containerSlotTier(i)
    case (i, _) if isInventorySlot(i) => true // Normal inventory.
    case _ => false // Invalid slot.
  }

  // ----------------------------------------------------------------------- //

  override def dropSlot(slot: Int, count: Int, direction: Option[EnumFacing]) =
    InventoryUtils.dropSlot(BlockPosition(x, y, z, world), mainInventory, slot, count, direction)

  override def dropAllSlots() = {
    InventoryUtils.dropSlot(BlockPosition(x, y, z, world), this, 0, Int.MaxValue)
    for (slot <- containerSlots) {
      InventoryUtils.dropSlot(BlockPosition(x, y, z, world), this, slot, Int.MaxValue)
    }
    InventoryUtils.dropAllSlots(BlockPosition(x, y, z, world), mainInventory)
  }

  // ----------------------------------------------------------------------- //

  override def canExtractItem(slot: Int, stack: ItemStack, side: EnumFacing) =
    getSlotsForFace(side).contains(slot)

  override def canInsertItem(slot: Int, stack: ItemStack, side: EnumFacing) =
    getSlotsForFace(side).contains(slot) &&
      isItemValidForSlot(slot, stack)

  override def getSlotsForFace(side: EnumFacing) =
    toLocal(side) match {
      case EnumFacing.WEST => Array(0) // Tool
      case EnumFacing.EAST => containerSlots.toArray
      case _ => inventorySlots.toArray
    }

  // ----------------------------------------------------------------------- //

  def tryGetTank(tank: Int) = {
    val tanks = components.collect {
      case Some(tank: IFluidTank) => tank
    }
    if (tank < 0 || tank >= tanks.length) None
    else Option(tanks(tank))
  }

  def tankCount = components.count {
    case Some(tank: IFluidTank) => true
    case _ => false
  }

  def getFluidTank(tank: Int) = tryGetTank(tank).orNull

  // ----------------------------------------------------------------------- //

  override def fill(from: EnumFacing, resource: FluidStack, doFill: Boolean) =
    tryGetTank(selectedTank) match {
      case Some(t) =>
        t.fill(resource, doFill)
      case _ => 0
    }

  override def drain(from: EnumFacing, resource: FluidStack, doDrain: Boolean) =
    tryGetTank(selectedTank) match {
      case Some(t) if t.getFluid != null && t.getFluid.isFluidEqual(resource) =>
        t.drain(resource.amount, doDrain)
      case _ => null
    }

  override def drain(from: EnumFacing, maxDrain: Int, doDrain: Boolean) = {
    tryGetTank(selectedTank) match {
      case Some(t) =>
        t.drain(maxDrain, doDrain)
      case _ => null
    }
  }

  override def canFill(from: EnumFacing, fluid: Fluid) = {
    tryGetTank(selectedTank) match {
      case Some(t) => t.getFluid == null || t.getFluid.getFluid == fluid
      case _ => false
    }
  }

  override def canDrain(from: EnumFacing, fluid: Fluid): Boolean = {
    tryGetTank(selectedTank) match {
      case Some(t) => t.getFluid != null && t.getFluid.getFluid == fluid
      case _ => false
    }
  }

  override def getTankInfo(from: EnumFacing) =
    components.collect {
      case Some(t: IFluidTank) => t.getInfo
    }
}

package li.cil.oc.common.tileentity

import java.util.UUID
import java.util.function.Consumer

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
import li.cil.oc.common.container
import li.cil.oc.common.container.ContainerTypes
import li.cil.oc.common.inventory.InventoryProxy
import li.cil.oc.common.inventory.InventorySelection
import li.cil.oc.common.inventory.TankSelection
import li.cil.oc.common.item.data.RobotData
import li.cil.oc.integration.opencomputers.DriverKeyboard
import li.cil.oc.integration.opencomputers.DriverRedstoneCard
import li.cil.oc.integration.opencomputers.DriverScreen
import li.cil.oc.server.agent
import li.cil.oc.server.agent.Player
import li.cil.oc.server.component
import li.cil.oc.server.{PacketSender => ServerPacketSender}
import li.cil.oc.util.BlockPosition
import li.cil.oc.util.ExtendedNBT._
import li.cil.oc.util.ExtendedWorld._
import li.cil.oc.util.InventoryUtils
import li.cil.oc.util.StackOption
import li.cil.oc.util.StackOption._
import net.minecraft.block.Block
import net.minecraft.block.Blocks
import net.minecraft.block.FlowingFluidBlock
import net.minecraft.client.Minecraft
import net.minecraft.entity.player.PlayerEntity
import net.minecraft.entity.player.PlayerInventory
import net.minecraft.fluid.Fluid
import net.minecraft.inventory.EquipmentSlotType
import net.minecraft.inventory.container.INamedContainerProvider
import net.minecraft.item.ItemStack
import net.minecraft.nbt.CompoundNBT
import net.minecraft.tileentity.TileEntity
import net.minecraft.util.Direction
import net.minecraft.util.SoundCategory
import net.minecraft.util.SoundEvents
import net.minecraft.util.Util
import net.minecraft.util.math.BlockPos
import net.minecraft.util.text.StringTextComponent
import net.minecraftforge.common.MinecraftForge
import net.minecraftforge.common.capabilities.Capability
import net.minecraftforge.common.util.LazyOptional
import net.minecraftforge.common.util.NonNullSupplier
import net.minecraftforge.fluids._
import net.minecraftforge.fluids.capability.CapabilityFluidHandler
import net.minecraftforge.fluids.capability.IFluidHandler
import net.minecraftforge.fluids.capability.IFluidHandler.FluidAction
import net.minecraftforge.api.distmarker.Dist
import net.minecraftforge.api.distmarker.OnlyIn

import scala.collection.mutable

// Implementation note: this tile entity is never directly added to the world.
// It is always wrapped by a `RobotProxy` tile entity, which forwards any
// necessary calls to this class. This is done to make moves efficient: when a
// robot moves we only create a new proxy tile entity, hook the instance of this
// class that was held by the old proxy to it and can then safely forget the
// old proxy, which will be cleaned up by Minecraft like any other tile entity.
class Robot extends TileEntity(TileEntityTypes.ROBOT) with traits.Computer with traits.PowerInformation with traits.RotatableTile
  with IFluidHandler with internal.Robot with InventorySelection with TankSelection with INamedContainerProvider {

  var proxy: RobotProxy = _

  val info = new RobotData()

  val bot: component.Robot = if (isServer) new component.Robot(this) else null

  val fluidCap: LazyOptional[IFluidHandler] = LazyOptional.of(new NonNullSupplier[IFluidHandler] {
    override def get = Robot.this
  })

  if (isServer) {
    machine.setCostPerTick(Settings.get.robotCost)
  }

  // ----------------------------------------------------------------------- //

  override def getCapability[T](capability: Capability[T], facing: Direction): LazyOptional[T] = {
    if (capability == CapabilityFluidHandler.FLUID_HANDLER_CAPABILITY)
      fluidCap.cast()
    else
      super.getCapability(capability, facing)
  }

  override def tier: Int = info.tier

  def isCreative: Boolean = tier == Tier.Four

  val equipmentInventory = new InventoryProxy {
    override def inventory: Robot = Robot.this

    override def getContainerSize = 4
  }

  // Wrapper for the part of the inventory that is mutable.
  val mainInventory = new InventoryProxy {
    override def inventory: Robot = Robot.this

    override def getContainerSize: Int = Robot.this.inventorySize

    override def offset: Int = equipmentInventory.getContainerSize
  }

  val actualInventorySize = 100

  def maxInventorySize: Int = actualInventorySize - equipmentInventory.getContainerSize - componentCount

  var inventorySize: Int = -1

  var selectedSlot = 0

  override def setSelectedSlot(index: Int): Unit = {
    selectedSlot = index max 0 min mainInventory.getContainerSize - 1
    if (getLevel != null) {
      ServerPacketSender.sendRobotSelectedSlotChange(this)
    }
  }

  val tank = new internal.MultiTank {
    override def tankCount: Int = Robot.this.tankCount

    override def getFluidTank(index: Int): ManagedEnvironment with IFluidTank = Robot.this.getFluidTank(index)
  }

  var selectedTank = 0

  override def setSelectedTank(index: Int): Unit = selectedTank = index

  // For client.
  var renderingErrored = false

  override def componentCount: Int = info.components.length

  override def getComponentInSlot(index: Int): ManagedEnvironment = if (components.length > index) components(index).orNull else null

  override def player: Player = {
    agent.Player.updatePositionAndRotation(player_, facing, facing)
    agent.Player.setPlayerInventoryItems(player_)
    player_
  }

  override def synchronizeSlot(slot: Int): Unit = if (slot >= 0 && slot < getContainerSize) this.synchronized {
    val stack = getItem(slot)
    components(slot) match {
      case Some(component) =>
        // We're guaranteed to have a driver for entries.
        save(component, Driver.driverFor(stack, getClass), stack)
      case _ =>
    }
    ServerPacketSender.sendRobotInventory(this, slot, stack)
  }

  def containerSlots: Range.Inclusive = 1 to info.containers.length

  def componentSlots: Range = getContainerSize - componentCount until getContainerSize

  def inventorySlots: Range = equipmentInventory.getContainerSize until (equipmentInventory.getContainerSize + mainInventory.getContainerSize)

  def setLightColor(value: Int): Unit = {
    info.lightColor = value
    ServerPacketSender.sendRobotLightChange(this)
  }

  override def shouldAnimate: Boolean = isRunning

  // ----------------------------------------------------------------------- //

  override def node: Node = if (isServer) machine.node else null

  var globalBuffer, globalBufferSize = 0.0

  val maxComponents = 32

  var ownerName: String = Settings.get.fakePlayerName

  var ownerUUID: UUID = Settings.get.fakePlayerProfile.getId

  var animationTicksLeft = 0

  var animationTicksTotal = 0

  var moveFrom: Option[BlockPos] = None

  var swingingTool = false

  var turnAxis = 0

  var appliedToolEnchantments = false

  private lazy val player_ = new agent.Player(this)

  // ----------------------------------------------------------------------- //

  override def name: String = info.name

  override def setName(name: String): Unit = info.name = name

  override def onAnalyze(player: PlayerEntity, side: Direction, hitX: Float, hitY: Float, hitZ: Float): Array[Node] = {
    player.sendMessage(Localization.Analyzer.RobotOwner(ownerName), Util.NIL_UUID)
    player.sendMessage(Localization.Analyzer.RobotName(player_.getName.getString), Util.NIL_UUID)
    MinecraftForge.EVENT_BUS.post(new RobotAnalyzeEvent(this, player))
    super.onAnalyze(player, side, hitX, hitY, hitZ)
  }

  def move(direction: Direction): Boolean = {
    val oldPosition = getBlockPos
    val newPosition = oldPosition.relative(direction)
    if (!getLevel.isLoaded(newPosition)) {
      return false // Don't fall off the earth.
    }

    if (isServer) {
      val event = new RobotMoveEvent.Pre(this, direction)
      MinecraftForge.EVENT_BUS.post(event)
      if (event.isCanceled) return false
    }

    val blockRobotProxy = api.Items.get(Constants.BlockName.Robot).block.asInstanceOf[common.block.RobotProxy]
    val blockRobotAfterImage = api.Items.get(Constants.BlockName.RobotAfterimage).block.asInstanceOf[common.block.RobotAfterimage]
    val wasAir = getLevel.isEmptyBlock(newPosition)
    val state = getLevel.getBlockState(newPosition)
    val block = state.getBlock
    try {
      // Setting this will make the tile entity created via the following call
      // to setBlock to re-use our "real" instance as the inner object, instead
      // of creating a new one.
      blockRobotProxy.moving.set(Some(this))
      // Do *not* immediately send the change to clients to allow checking if it
      // worked before the client is notified so that we can use the same trick on
      // the client by sending a corresponding packet. This also saves us from
      // having to send the complete state again (e.g. screen buffer) each move.
      getLevel.setBlockAndUpdate(newPosition, Blocks.AIR.defaultBlockState)
      // In some cases (though I couldn't quite figure out which one) setBlock
      // will return true, even though the block was not created / adjusted.
      val created = getLevel.setBlock(newPosition, getLevel.getBlockState(oldPosition), 1) &&
        getLevel.getBlockEntity(newPosition) == proxy
      if (created) {
        assert(getBlockPos == newPosition)
        getLevel.setBlock(oldPosition, Blocks.AIR.defaultBlockState, 1)
        getLevel.setBlock(oldPosition, blockRobotAfterImage.defaultBlockState, 1)
        assert(getLevel.getBlockState(oldPosition).getBlock == blockRobotAfterImage)
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
            if (block != Blocks.AIR && block != blockRobotAfterImage) {
              if (!state.getFluidState.isEmpty) {
                getLevel.playLocalSound(newPosition.getX + 0.5, newPosition.getY + 0.5, newPosition.getZ + 0.5, SoundEvents.WATER_AMBIENT, SoundCategory.BLOCKS,
                  getLevel.random.nextFloat * 0.25f + 0.75f, getLevel.random.nextFloat * 1.0f + 0.5f, false)
              }
              if (!block.isInstanceOf[FlowingFluidBlock]) {
                getLevel.levelEvent(2001, newPosition, Block.getId(state))
              }
            }
          }
          getLevel.notifyBlockUpdate(oldPosition)
          getLevel.notifyBlockUpdate(newPosition)
        }
        assert(!isRemoved)
      }
      else {
        getLevel.setBlockAndUpdate(newPosition, Blocks.AIR.defaultBlockState)
      }
      created && getBlockPos == newPosition
    }
    finally {
      blockRobotProxy.moving.set(None)
    }
  }

  // ----------------------------------------------------------------------- //

  def isAnimatingMove: Boolean = animationTicksLeft > 0 && moveFrom.isDefined

  def isAnimatingSwing: Boolean = animationTicksLeft > 0 && swingingTool

  def isAnimatingTurn: Boolean = animationTicksLeft > 0 && turnAxis != 0

  def animateSwing(duration: Double): Unit = if (!items(0).isEmpty) {
    setAnimateSwing((duration * 20).toInt)
    ServerPacketSender.sendRobotAnimateSwing(this)
  }

  def animateTurn(clockwise: Boolean, duration: Double): Unit = {
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
      if (getLevel.getGameTime % Settings.get.tickFrequency == 0) {
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
        StackOption(getItem(0)) match {
          case SomeStack(item) => player_.getAttributes.addTransientAttributeModifiers(item.getAttributeModifiers(EquipmentSlotType.MAINHAND))
          case _ =>
        }
      }
    }
    else if (isRunning && isAnimatingMove) {
      client.Sound.updatePosition(this)
    }

    for (slot <- 0 until equipmentInventory.getContainerSize + mainInventory.getContainerSize) {
      getItem(slot) match {
        case stack: ItemStack => try stack.inventoryTick(getLevel, if (!getLevel.isClientSide) player_ else null, slot, slot == 0) catch {
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
      Minecraft.getInstance.screen match {
        case robotGui: gui.Robot if robotGui.inventoryContainer.otherInventory == this =>
          robotGui.onClose()
        case _ =>
      }
    }
    else EventHandler.onRobotStopped(this)
  }

  // ----------------------------------------------------------------------- //

  private final val RobotTag = Settings.namespace + "robot"
  private final val OwnerTag = Settings.namespace + "owner"
  private final val OwnerUUIDTag = Settings.namespace + "ownerUuid"
  private final val SelectedSlotTag = Settings.namespace + "selectedSlot"
  private final val SelectedTankTag = Settings.namespace + "selectedTank"
  private final val AnimationTicksTotalTag = Settings.namespace + "animationTicksTotal"
  private final val AnimationTicksLeftTag = Settings.namespace + "animationTicksLeft"
  private final val MoveFromXTag = Settings.namespace + "moveFromX"
  private final val MoveFromYTag = Settings.namespace + "moveFromY"
  private final val MoveFromZTag = Settings.namespace + "moveFromZ"
  private final val SwingingToolTag = Settings.namespace + "swingingTool"
  private final val TurnAxisTag = Settings.namespace + "turnAxis"

  override def loadForServer(nbt: CompoundNBT) {
    updateInventorySize()
    machine.onHostChanged()

    bot.loadData(nbt.getCompound(RobotTag))
    if (nbt.contains(OwnerTag)) {
      ownerName = nbt.getString(OwnerTag)
    }
    if (nbt.contains(OwnerUUIDTag)) {
      ownerUUID = UUID.fromString(nbt.getString(OwnerUUIDTag))
    }
    if (inventorySize > 0) {
      selectedSlot = nbt.getInt(SelectedSlotTag) max 0 min mainInventory.getContainerSize - 1
    }
    selectedTank = nbt.getInt(SelectedTankTag)
    animationTicksTotal = nbt.getInt(AnimationTicksTotalTag)
    animationTicksLeft = nbt.getInt(AnimationTicksLeftTag)
    if (animationTicksLeft > 0) {
      if (nbt.contains(MoveFromXTag)) {
        val moveFromX = nbt.getInt(MoveFromXTag)
        val moveFromY = nbt.getInt(MoveFromYTag)
        val moveFromZ = nbt.getInt(MoveFromZTag)
        moveFrom = Some(new BlockPos(moveFromX, moveFromY, moveFromZ))
      }
      swingingTool = nbt.getBoolean(SwingingToolTag)
      turnAxis = nbt.getByte(TurnAxisTag)
    }

    // Normally set in superclass, but that's not called directly, only in the
    // robot's proxy instance.
    _isOutputEnabled = hasRedstoneCard
    if (isRunning) EventHandler.onRobotStart(this)
  }

  // Side check for Waila (and other mods that may call this client side).
  override def saveForServer(nbt: CompoundNBT): Unit = if (isServer) this.synchronized {
    info.saveData(nbt)

    // Note: computer is saved when proxy is saved (in proxy's super save)
    // which is a bit ugly, and may be refactored some day, but it works.
    nbt.setNewCompoundTag(RobotTag, bot.saveData)
    nbt.putString(OwnerTag, ownerName)
    nbt.putString(OwnerUUIDTag, ownerUUID.toString)
    nbt.putInt(SelectedSlotTag, selectedSlot)
    nbt.putInt(SelectedTankTag, selectedTank)
    if (isAnimatingMove || isAnimatingSwing || isAnimatingTurn) {
      nbt.putInt(AnimationTicksTotalTag, animationTicksTotal)
      nbt.putInt(AnimationTicksLeftTag, animationTicksLeft)
      moveFrom match {
        case Some(blockPos) =>
          nbt.putInt(MoveFromXTag, blockPos.getX)
          nbt.putInt(MoveFromYTag, blockPos.getY)
          nbt.putInt(MoveFromZTag, blockPos.getZ)
        case _ =>
      }
      nbt.putBoolean(SwingingToolTag, swingingTool)
      nbt.putByte(TurnAxisTag, turnAxis.toByte)
    }
  }

  @OnlyIn(Dist.CLIENT)
  override def loadForClient(nbt: CompoundNBT) {
    super.loadForClient(nbt)
    loadData(nbt)
    info.loadData(nbt)

    updateInventorySize()

    selectedSlot = nbt.getInt(SelectedSlotTag)
    animationTicksTotal = nbt.getInt(AnimationTicksTotalTag)
    animationTicksLeft = nbt.getInt(AnimationTicksLeftTag)
    if (animationTicksLeft > 0) {
      if (nbt.contains(MoveFromXTag)) {
        val moveFromX = nbt.getInt(MoveFromXTag)
        val moveFromY = nbt.getInt(MoveFromYTag)
        val moveFromZ = nbt.getInt(MoveFromZTag)
        moveFrom = Some(new BlockPos(moveFromX, moveFromY, moveFromZ))
      }
      swingingTool = nbt.getBoolean(SwingingToolTag)
      turnAxis = nbt.getByte(TurnAxisTag)
    }
    connectComponents()
  }

  override def saveForClient(nbt: CompoundNBT): Unit = this.synchronized {
    super.saveForClient(nbt)
    saveData(nbt)
    info.saveData(nbt)

    nbt.putInt(SelectedSlotTag, selectedSlot)
    if (isAnimatingMove || isAnimatingSwing || isAnimatingTurn) {
      nbt.putInt(AnimationTicksTotalTag, animationTicksTotal)
      nbt.putInt(AnimationTicksLeftTag, animationTicksLeft)
      moveFrom match {
        case Some(blockPos) =>
          nbt.putInt(MoveFromXTag, blockPos.getX)
          nbt.putInt(MoveFromYTag, blockPos.getY)
          nbt.putInt(MoveFromZTag, blockPos.getZ)
        case _ =>
      }
      nbt.putBoolean(SwingingToolTag, swingingTool)
      nbt.putByte(TurnAxisTag, turnAxis.toByte)
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
        player_.getAttributes.addTransientAttributeModifiers(stack.getAttributeModifiers(EquipmentSlotType.MAINHAND))
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
        getLevel.notifyBlocksOfNeighborChange(position, getBlockState.getBlock, updateObservers = false)
      }
      if (isInventorySlot(slot)) {
        machine.signal("inventory_changed", Int.box(slot - equipmentInventory.getContainerSize + 1))
      }
    }
    else super.onItemAdded(slot, stack)
  }

  override protected def onItemRemoved(slot: Int, stack: ItemStack) {
    super.onItemRemoved(slot, stack)
    if (isServer) {
      if (isToolSlot(slot)) {
        player_.getAttributes.removeAttributeModifiers(stack.getAttributeModifiers(EquipmentSlotType.MAINHAND))
        ServerPacketSender.sendRobotInventory(this, slot, ItemStack.EMPTY)
      }
      if (isUpgradeSlot(slot)) {
        ServerPacketSender.sendRobotInventory(this, slot, ItemStack.EMPTY)
      }
      if (isFloppySlot(slot)) {
        common.Sound.playDiskEject(this)
      }
      if (isInventorySlot(slot)) {
        machine.signal("inventory_changed", Int.box(slot - equipmentInventory.getContainerSize + 1))
      }
      if (isComponentSlot(slot, stack)) {
        getLevel.notifyBlocksOfNeighborChange(position, getBlockState.getBlock, updateObservers = false)
      }
    }
  }

  override def setChanged() {
    super.setChanged()
    // Avoid getting into a bad state on the client when updating before we
    // got the descriptor packet from the server. If we manage to open the
    // GUI before the descriptor packet arrived, close it again because it is
    // invalid anyway.
    if (inventorySize >= 0) {
      updateInventorySize()
    }
    else if (isClient) {
      Minecraft.getInstance.screen match {
        case robotGui: gui.Robot if robotGui.inventoryContainer.otherInventory == this =>
          robotGui.onClose()
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

  override def isComponentSlot(slot: Int, stack: ItemStack): Boolean = (containerSlots ++ componentSlots) contains slot

  def containerSlotType(slot: Int): String = if (containerSlots contains slot) {
    val stack = info.containers(slot - 1)
    Option(Driver.driverFor(stack, getClass)) match {
      case Some(driver: Container) => driver.providedSlot(stack)
      case _ => Slot.None
    }
  }
  else Slot.None

  def containerSlotTier(slot: Int): Int = if (containerSlots contains slot) {
    val stack = info.containers(slot - 1)
    Option(Driver.driverFor(stack, getClass)) match {
      case Some(driver: Container) => driver.providedTier(stack)
      case _ => Tier.None
    }
  }
  else Tier.None

  def isToolSlot(slot: Int): Boolean = slot == 0

  def isContainerSlot(slot: Int): Boolean = containerSlots contains slot

  def isInventorySlot(slot: Int): Boolean = inventorySlots contains slot

  def isFloppySlot(slot: Int): Boolean = !getItem(slot).isEmpty && isComponentSlot(slot, getItem(slot)) && {
    val stack = getItem(slot)
    Option(Driver.driverFor(stack, getClass)) match {
      case Some(driver) => driver.slot(stack) == Slot.Floppy
      case _ => false
    }
  }

  def isUpgradeSlot(slot: Int): Boolean = containerSlotType(slot) == Slot.Upgrade

  // ----------------------------------------------------------------------- //

  override def componentSlot(address: String): Int = components.indexWhere(_.exists(env => env.node != null && env.node.address == address))

  override def hasRedstoneCard: Boolean = (containerSlots ++ componentSlots).exists(slot => StackOption(getItem(slot)).fold(false)(DriverRedstoneCard.worksWith(_, getClass)))

  private def computeInventorySize() = math.min(maxInventorySize, (containerSlots ++ componentSlots).foldLeft(0)((acc, slot) => acc + (StackOption(getItem(slot)) match {
    case SomeStack(stack) => Option(Driver.driverFor(stack, getClass)) match {
      case Some(driver: item.Inventory) => driver.inventoryCapacity(stack)
      case _ => 0
    }
    case _ => 0
  })))

  private var updatingInventorySize = false

  def updateInventorySize(): Unit = this.synchronized(if (!updatingInventorySize) try {
    updatingInventorySize = true
    val newInventorySize = computeInventorySize()
    if (newInventorySize != inventorySize) {
      inventorySize = newInventorySize
      val realSize = equipmentInventory.getContainerSize + mainInventory.getContainerSize
      val oldSelected = selectedSlot
      val removed = mutable.ArrayBuffer.empty[ItemStack]
      for (slot <- realSize until getContainerSize - componentCount) {
        val stack = getItem(slot)
        setItem(slot, ItemStack.EMPTY)
        if (!stack.isEmpty) removed += stack
      }
      val copyComponentCount = math.min(getContainerSize, componentCount)
      Array.copy(components, getContainerSize - copyComponentCount, components, realSize, copyComponentCount)
      for (slot <- math.max(0, getContainerSize - componentCount) until getContainerSize if slot < realSize || slot >= realSize + componentCount) {
        components(slot) = None
      }
      getContainerSize = realSize + componentCount
      if (getLevel != null && isServer) {
        for (stack <- removed) {
          player().inventory.add(stack)
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

  var getContainerSize: Int = actualInventorySize

  override def getMaxStackSize = 64

  override def getItem(slot: Int): ItemStack = {
    if (slot >= getContainerSize) null // Required to always show 16 inventory slots in GUI.
    else if (slot >= getContainerSize - componentCount) {
      info.components(slot - (getContainerSize - componentCount))
    }
    else super.getItem(slot)
  }

  override def setItem(slot: Int, stack: ItemStack) {
    if (slot < getContainerSize - componentCount && (canPlaceItem(slot, stack) || stack.isEmpty)) {
      if (!stack.isEmpty && stack.getCount > 1 && isComponentSlot(slot, stack)) {
        super.setItem(slot, stack.split(1))
        if (stack.getCount > 0 && isServer) {
          player().inventory.add(stack)
          spawnStackInWorld(stack, Option(facing))
        }
      }
      else super.setItem(slot, stack)
    }
    else if (!stack.isEmpty && stack.getCount > 0 && !getLevel.isClientSide) spawnStackInWorld(stack, Option(Direction.UP))
  }

  override def stillValid(player: PlayerEntity): Boolean =
    super.stillValid(player) && (!isCreative || player.isCreative)

  override def canPlaceItem(slot: Int, stack: ItemStack): Boolean = (slot, Option(Driver.driverFor(stack, getClass))) match {
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

  override def getDisplayName = StringTextComponent.EMPTY

  override def createMenu(id: Int, playerInventory: PlayerInventory, player: PlayerEntity) =
    new container.Robot(ContainerTypes.ROBOT, id, playerInventory, this, new container.RobotInfo(this))

  // ----------------------------------------------------------------------- //

  override def forAllLoot(dst: Consumer[ItemStack]) {
    Option(getItem(0)) match {
      case Some(stack) if stack.getCount > 0 => dst.accept(stack)
      case _ =>
    }
    for (slot <- containerSlots) {
      Option(getItem(slot)) match {
        case Some(stack) if stack.getCount > 0 => dst.accept(stack)
        case _ =>
      }
    }
    InventoryUtils.forAllSlots(mainInventory, dst)
  }

  override def dropSlot(slot: Int, count: Int, direction: Option[Direction]): Boolean =
    InventoryUtils.dropSlot(BlockPosition(x, y, z, getLevel), mainInventory, slot, count, direction)

  override def dropAllSlots(): Unit = {
    InventoryUtils.dropSlot(BlockPosition(x, y, z, getLevel), this, 0, Int.MaxValue)
    for (slot <- containerSlots) {
      InventoryUtils.dropSlot(BlockPosition(x, y, z, getLevel), this, slot, Int.MaxValue)
    }
    InventoryUtils.dropAllSlots(BlockPosition(x, y, z, getLevel), mainInventory)
  }

  // ----------------------------------------------------------------------- //

  override def canTakeItemThroughFace(slot: Int, stack: ItemStack, side: Direction): Boolean =
    getSlotsForFace(side).toSeq.contains(slot)

  override def canPlaceItemThroughFace(slot: Int, stack: ItemStack, side: Direction): Boolean =
    getSlotsForFace(side).toSeq.contains(slot) &&
      canPlaceItem(slot, stack)

  override def getSlotsForFace(side: Direction): Array[Int] =
    toLocal(side) match {
      case Direction.WEST => Array(0) // Tool
      case Direction.EAST => containerSlots.toArray
      case _ => inventorySlots.toArray
    }

  // ----------------------------------------------------------------------- //

  def tryGetTank(tank: Int): Option[ManagedEnvironment with IFluidTank] = {
    val tanks = components.collect {
      case Some(tank: IFluidTank) => tank
    }
    if (tank < 0 || tank >= tanks.length) None
    else Option(tanks(tank))
  }

  def tankCount: Int = components.count {
    case Some(tank: IFluidTank) => true
    case _ => false
  }

  def getFluidTank(tank: Int): ManagedEnvironment with IFluidTank = tryGetTank(tank).orNull

  // ----------------------------------------------------------------------- //

  override def getTanks = tankCount

  override def getFluidInTank(tank: Int): FluidStack =
    tryGetTank(selectedTank) match {
      case Some(t) =>
        t.getFluid
      case _ => FluidStack.EMPTY
    }

  override def getTankCapacity(tank: Int): Int =
    tryGetTank(selectedTank) match {
      case Some(t) =>
        t.getCapacity
      case _ => 0
    }

  override def isFluidValid(tank: Int, resource: FluidStack) = true

  override def fill(resource: FluidStack, action: FluidAction): Int =
    tryGetTank(selectedTank) match {
      case Some(t) =>
        t.fill(resource, action)
      case _ => 0
    }

  override def drain(resource: FluidStack, action: FluidAction): FluidStack =
    tryGetTank(selectedTank) match {
      case Some(t) if t.getFluid != null && t.getFluid.isFluidEqual(resource) =>
        t.drain(resource.getAmount, action)
      case _ => FluidStack.EMPTY
    }

  override def drain(maxDrain: Int, action: FluidAction): FluidStack = {
    tryGetTank(selectedTank) match {
      case Some(t) =>
        t.drain(maxDrain, action)
      case _ => FluidStack.EMPTY
    }
  }
}

package li.cil.oc.server.agent

import java.util
import java.util.UUID

import com.mojang.datafixers.util.Either
import com.mojang.authlib.GameProfile
import li.cil.oc.OpenComputers
import li.cil.oc.Settings
import li.cil.oc.api.event._
import li.cil.oc.api.internal
import li.cil.oc.api.network.Connector
import li.cil.oc.common.EventHandler
import li.cil.oc.util.BlockPosition
import li.cil.oc.util.InventoryUtils
import net.minecraft.block.PistonBlock
import net.minecraft.entity.Entity
import net.minecraft.entity.EntitySize
import net.minecraft.entity.LivingEntity
import net.minecraft.entity.Pose
import net.minecraft.entity.item.ItemEntity
import net.minecraft.entity.merchant.IMerchant
import net.minecraft.entity.player.PlayerEntity
import net.minecraft.entity.player.PlayerEntity.SleepResult
import net.minecraft.block.Blocks
import net.minecraft.item.Items
import net.minecraft.inventory._
import net.minecraft.inventory.container.INamedContainerProvider
import net.minecraft.inventory.container.PlayerContainer
import net.minecraft.item.BlockItem
import net.minecraft.item.ItemUseContext
import net.minecraft.item.ItemStack
import net.minecraft.item.MerchantOffers
import net.minecraft.network.play.ServerPlayNetHandler
import net.minecraft.network.play.client.CPlayerDiggingPacket
import net.minecraft.potion.EffectInstance
import net.minecraft.server.management.{PlayerInteractionManager, OpEntry}
import net.minecraft.tileentity._
import net.minecraft.util.ActionResultType
import net.minecraft.util.DamageSource
import net.minecraft.util.Direction
import net.minecraft.util.Hand
import net.minecraft.util.math.BlockPos
import net.minecraft.util.math.BlockRayTraceResult
import net.minecraft.util.math.vector.Vector3d
import net.minecraft.util.text.ITextComponent
import net.minecraft.util.text.StringTextComponent
import net.minecraft.world.World
import net.minecraft.world.server.ServerWorld
import net.minecraftforge.common.MinecraftForge
import net.minecraftforge.common.util.FakePlayer
import net.minecraftforge.common.util.LazyOptional
import net.minecraftforge.common.util.NonNullSupplier
import net.minecraftforge.event.ForgeEventFactory
import net.minecraftforge.event.entity.living.LivingEntityUseItemEvent
import net.minecraftforge.event.entity.player.PlayerInteractEvent
import net.minecraftforge.fml.common.ObfuscationReflectionHelper
import net.minecraftforge.eventbus.api.{Event, EventPriority, SubscribeEvent}
import net.minecraftforge.items.IItemHandler
import net.minecraftforge.items.wrapper._

import scala.collection.convert.WrapAsScala._

object Player {
  def profileFor(agent: internal.Agent): GameProfile = {
    val uuid = agent.ownerUUID
    val randomId = (agent.world.random.nextInt(0xFFFFFF) + 1).toString
    val name = Settings.get.nameFormat.
      replace("$player$", agent.ownerName).
      replace("$random$", randomId)
    new GameProfile(uuid, name)
  }

  def determineUUID(playerUUID: Option[UUID] = None): UUID = {
    val format = Settings.get.uuidFormat
    val randomUUID = UUID.randomUUID()
    try UUID.fromString(format.
      replaceAllLiterally("$random$", randomUUID.toString).
      replaceAllLiterally("$player$", playerUUID.getOrElse(randomUUID).toString)) catch {
      case t: Throwable =>
        OpenComputers.log.warn("Failed determining robot UUID, check your config's `uuidFormat` entry!", t)
        randomUUID
    }
  }

  def updatePositionAndRotation(player: Player, facing: Direction, side: Direction) {
    player.facing = facing
    player.side = side
    val direction = new Vector3d(
      facing.getStepX + side.getStepX,
      facing.getStepY + side.getStepY,
      facing.getStepZ + side.getStepZ).normalize()
    val yaw = Math.toDegrees(-Math.atan2(direction.x, direction.z)).toFloat
    val pitch = Math.toDegrees(-Math.atan2(direction.y, Math.sqrt((direction.x * direction.x) + (direction.z * direction.z)))).toFloat * 0.99f
    player.setPos(player.agent.xPosition, player.agent.yPosition, player.agent.zPosition)
    player.setRot(yaw, pitch)
    player.xRotO = player.xRot
    player.yRotO = player.yRot
  }

  def setPlayerInventoryItems(player: Player): Unit = {
    // the offhand is simply the agent's tool item
    val agent = player.agent
    def setCopyOrNull(inv: net.minecraft.util.NonNullList[ItemStack], agentInv: IInventory, slot: Int): Unit = {
      val item = agentInv.getItem(slot)
      inv(slot) = if (item != null) item.copy() else ItemStack.EMPTY
    }

    for (i <- 0 until 4) {
      setCopyOrNull(player.inventory.armor, agent.equipmentInventory, i)
    }

    // items is 36 items
    // the agent inventory is 100 items with some space for components
    // leaving us 88..we'll copy what we can
    val size = player.inventory.items.length min agent.mainInventory.getContainerSize
    for (i <- 0 until size) {
      setCopyOrNull(player.inventory.items, agent.mainInventory, i)
    }
    player.inventoryMenu.broadcastChanges()
  }

  def detectPlayerInventoryChanges(player: Player): Unit = {
  	val agent = player.agent
    player.inventoryMenu.broadcastChanges()
    // The follow code will set agent.inventories = FakePlayer's inv.stack
    def setCopy(inv: IInventory, index: Int, item: ItemStack): Unit = {
      val result = if (item != null) item.copy else ItemStack.EMPTY
      val current = inv.getItem(index)
      if (!ItemStack.matches(result, current)) {
        inv.setItem(index, result)
      }
    }
    for (i <- 0 until 4) {
      setCopy(agent.equipmentInventory(), i, player.inventory.armor(i))
    }
    val size = player.inventory.items.length min agent.mainInventory.getContainerSize
    for (i <- 0 until size) {
      setCopy(agent.mainInventory, i, player.inventory.items(i))
    }
  }
}

class Player(val agent: internal.Agent) extends FakePlayer(agent.world.asInstanceOf[ServerWorld], Player.profileFor(agent)) {
  connection= new ServerPlayNetHandler(server, FakeNetworkManager, this)

  abilities.mayfly = true
  abilities.invulnerable = true
  abilities.flying = true
  setOnGround(true)

  override def getMyRidingOffset = 0.5

  override def getStandingEyeHeight(pose: Pose, size: EntitySize) = 0f

  override def getDimensions(pose: Pose) = new EntitySize(1, 1, true)
  refreshDimensions()

  {
    this.inventory = new Inventory(this, agent)
    // because the inventory was just overwritten, the container is now detached
    this.inventoryMenu = new PlayerContainer(inventory, !level.isClientSide, this)
    this.containerMenu = this.inventoryMenu

    try {
      ObfuscationReflectionHelper.setPrivateValue(classOf[PlayerEntity], this, LazyOptional.of(new NonNullSupplier[IItemHandler] {
        override def get = new PlayerMainInvWrapper(inventory)
      }), "playerMainHandler")
      ObfuscationReflectionHelper.setPrivateValue(classOf[PlayerEntity], this, LazyOptional.of(new NonNullSupplier[IItemHandler] {
        override def get = new CombinedInvWrapper(new PlayerArmorInvWrapper(inventory), new PlayerOffhandInvWrapper(inventory))
      }), "playerEquipmentHandler")
      ObfuscationReflectionHelper.setPrivateValue(classOf[PlayerEntity], this, LazyOptional.of(new NonNullSupplier[IItemHandler] {
        override def get = new PlayerInvWrapper(inventory)
      }), "playerJoinedHandler")
    } catch {
      case _: Exception =>
    }
  }

  var facing, side = Direction.SOUTH

  override def getName = new StringTextComponent(agent.name)

  // ----------------------------------------------------------------------- //

  def closestEntity[Type <: Entity](clazz: Class[Type], side: Direction = facing): Option[Entity] = {
    val bounds = BlockPosition(agent).offset(side).bounds
    val candidates = level.getEntitiesOfClass(clazz, bounds, null)
    if (candidates.isEmpty) return None
    Some(candidates.minBy(e => distanceToSqr(e)))
  }

  def entitiesOnSide[Type <: Entity](clazz: Class[Type], side: Direction): util.List[Type] = {
    entitiesInBlock(clazz, BlockPosition(agent).offset(side))
  }

  def entitiesInBlock[Type <: Entity](clazz: Class[Type], blockPos: BlockPosition): util.List[Type] = {
    level.getEntitiesOfClass(clazz, blockPos.bounds, null)
  }

  private def adjacentItems: util.List[ItemEntity] = {
    level.getEntitiesOfClass(classOf[ItemEntity], BlockPosition(agent).bounds.inflate(2, 2, 2), null)
  }

  private def collectDroppedItems(itemsBefore: Iterable[ItemEntity]) {
    val itemsAfter = adjacentItems
    val itemsDropped = itemsAfter -- itemsBefore
    if (itemsDropped.nonEmpty) {
      for (drop <- itemsDropped) {
        drop.setDefaultPickUpDelay()
        drop.playerTouch(this)
      }
    }
  }

  // ----------------------------------------------------------------------- //

  override def attack(entity: Entity) {
    callUsingItemInSlot(agent.equipmentInventory, 0, stack => entity match {
      case player: PlayerEntity if !canHarmPlayer(player) => // Avoid player damage.
      case _ =>
        val event = new RobotAttackEntityEvent.Pre(agent, entity)
        MinecraftForge.EVENT_BUS.post(event)
        if (!event.isCanceled) {
          super.attack(entity)
          MinecraftForge.EVENT_BUS.post(new RobotAttackEntityEvent.Post(agent, entity))
        }
    })
  }

  override def interactOn(entity: Entity, hand: Hand): ActionResultType = {
    val cancel = try MinecraftForge.EVENT_BUS.post(new PlayerInteractEvent.EntityInteract(this, hand, entity)) catch {
      case t: Throwable =>
        if (!t.getStackTrace.exists(_.getClassName.startsWith("mods.battlegear2."))) {
          OpenComputers.log.warn("Some event handler screwed up!", t)
        }
        false
    }
    if(!cancel && callUsingItemInSlot(agent.equipmentInventory, 0, stack => {
      val result = isItemUseAllowed(stack) && (entity.interact(this, hand).consumesAction || (entity match {
        case living: LivingEntity if !getItemInHand(Hand.MAIN_HAND).isEmpty => getItemInHand(Hand.MAIN_HAND).interactLivingEntity(this, living, hand).consumesAction
        case _ => false
      }))
      if (!getItemInHand(Hand.MAIN_HAND).isEmpty) {
        if (getItemInHand(Hand.MAIN_HAND).getCount <= 0) {
          val orig = getItemInHand(Hand.MAIN_HAND)
          this.inventory.setItem(this.inventory.selected, ItemStack.EMPTY)
          ForgeEventFactory.onPlayerDestroyItem(this, orig, hand)
        } else {
          // because of various hacks for IC2, we expect the in-hand result to be moved to our offhand buffer
          this.inventory.offhand.set(0, getItemInHand(Hand.MAIN_HAND))
          this.inventory.setItem(this.inventory.selected, ItemStack.EMPTY)
        }
      }
      result
    })) ActionResultType.sidedSuccess(level.isClientSide) else ActionResultType.PASS
  }

  def activateBlockOrUseItem(pos: BlockPos, side: Direction, hitX: Float, hitY: Float, hitZ: Float, duration: Double): ActivationType.Value = {
    callUsingItemInSlot(agent.equipmentInventory, 0, stack => {
      if (shouldCancel(() => fireRightClickBlock(pos, side))) {
        return ActivationType.None
      }

      val item = if (!stack.isEmpty) stack.getItem else null
      val state = level.getBlockState(pos)
      val traceEndPos = new Vector3d(pos.getX + hitX, pos.getY + hitY, pos.getZ + hitZ)
      val traceCtx = if (state.getBlock.isAir(state, level, pos)) BlockRayTraceResult.miss(traceEndPos, side, pos) else new BlockRayTraceResult(traceEndPos, side, pos, false)
      if (item != null && item.onItemUseFirst(stack, new ItemUseContext(level, this, Hand.OFF_HAND, stack, traceCtx)).consumesAction) {
        return ActivationType.ItemUsed
      }

      val canActivate = !state.getBlock.isAir(state, level, pos) && Settings.get.allowActivateBlocks
      val shouldActivate = canActivate && (!isCrouching || (item == null || item.doesSneakBypassUse(stack, level, pos, this)))
      val result =
        if (shouldActivate && state.use(level, this, Hand.OFF_HAND, new BlockRayTraceResult(new Vector3d(hitX, hitY, hitZ), side, pos, false)).consumesAction)
          ActivationType.BlockActivated
        else if (duration <= Double.MinPositiveValue && isItemUseAllowed(stack) && tryPlaceBlockWhileHandlingFunnySpecialCases(stack, pos, side, hitX, hitY, hitZ))
          ActivationType.ItemPlaced
        else if (useEquippedItem(duration, Option(stack)))
          ActivationType.ItemUsed
        else
          ActivationType.None

      result
    })
  }

  override def setItemSlot(slotIn: EquipmentSlotType, stack: ItemStack): Unit = {
    var superCall: () => Unit = () => super.setItemSlot(slotIn, stack)
    if (slotIn == EquipmentSlotType.MAINHAND) {
      agent.equipmentInventory.setItem(0, stack)
      superCall = () => {
        val slot = inventory.selected
        // So, if we're not in the main inventory, selected is set to -1
        // for compatibility with mods that try accessing the inv directly
        // using inventory.selected. See li.cil.oc.server.agent.Inventory
        if(inventory.selected < 0) inventory.selected = ~inventory.selected
        super.setItemSlot(slotIn, stack)
        inventory.selected = slot
      }
    } else if(slotIn == EquipmentSlotType.OFFHAND) {
      inventory.offhand.set(0, stack)
    }
    superCall()
  }

  override def getItemBySlot(slotIn: EquipmentSlotType): ItemStack = {
    if (slotIn == EquipmentSlotType.MAINHAND)
      agent.equipmentInventory.getItem(0)
    else if(slotIn == EquipmentSlotType.OFFHAND)
      inventory.offhand.get(0)
    else super.getItemBySlot(slotIn)
  }

  def fireRightClickBlock(pos: BlockPos, side: Direction): PlayerInteractEvent.RightClickBlock = {
    val hitVec = new Vector3d(0.5 + side.getStepX * 0.5, 0.5 + side.getStepY * 0.5, 0.5 + side.getStepZ * 0.5)
    val event = new PlayerInteractEvent.RightClickBlock(this, Hand.OFF_HAND, pos, new BlockRayTraceResult(hitVec, side, pos, false))
    MinecraftForge.EVENT_BUS.post(event)
    event
  }

  def fireLeftClickBlock(pos: BlockPos, side: Direction): PlayerInteractEvent.LeftClickBlock = {
    net.minecraftforge.common.ForgeHooks.onLeftClickBlock(this, pos, side)
  }

  def fireRightClickAir(): PlayerInteractEvent.RightClickItem = {
    val event = new PlayerInteractEvent.RightClickItem(this, Hand.OFF_HAND)
    MinecraftForge.EVENT_BUS.post(event)
    event
  }

  private def trySetActiveHand(duration: Double): Boolean = {
    releaseUsingItem()
    val entity = this
    val durationHandler = new {
      @SubscribeEvent(priority = EventPriority.LOWEST)
      def onItemUseStart(startUse: LivingEntityUseItemEvent.Start): Unit = {
        if (startUse.getEntityLiving == entity && !startUse.isCanceled) {
          startUse.setDuration(duration.toInt)
        }
      }
    }
    MinecraftForge.EVENT_BUS.register(durationHandler)
    try {
      startUsingItem(Hand.OFF_HAND)
      isUsingItem
    } catch {
        case _: Exception => false
    } finally {
      MinecraftForge.EVENT_BUS.unregister(durationHandler)
    }
  }

  def useItemWithHand(duration: Double, stack: ItemStack): Boolean = {
    if (!trySetActiveHand(duration)) {
      if (duration > 0) {
        return false
      }
    }

    val oldStack = stack.copy
    if (!isItemUseAllowed(stack)) {
      return false
    }

    val maxDuration = stack.getUseDuration
    val heldTicks = Math.max(0, Math.min(maxDuration, (duration * 20).toInt))
    agent.machine.pause(heldTicks / 20.0)

    // setting the active hand will also set its initial duration
    val useItemResult = stack.use(level, this, Hand.OFF_HAND)
    releaseUsingItem()

    if (!useItemResult.getResult.consumesAction) {
      return false
    }

    val newStack = useItemResult.getObject
    val stackChanged: Boolean =
      !ItemStack.matches(oldStack, newStack) ||
      !ItemStack.matches(oldStack, stack)

    if (stackChanged) {
      inventory.offhand.set(0, newStack)
    }
    stackChanged
  }

  def useEquippedItem(duration: Double, stackOption: Option[ItemStack] = None): Boolean = {
    if (stackOption.isEmpty) {
      return callUsingItemInSlot(agent.equipmentInventory, 0, {
        case item: ItemStack if item != null => useEquippedItem(duration, Option(item))
        case _ => false
      })
    }

    if (shouldCancel(() => fireRightClickAir())) {
      return false
    }

    // Change the offset at which items are used, to avoid hitting
    // the robot itself (e.g. with bows, potions, mining laser, ...).
    setPos(getX + facing.getStepX / 2.0, getY, getZ + facing.getStepZ / 2.0)

    try {
      useItemWithHand(duration, stackOption.get)
    }
    finally {
      setPos(getX - facing.getStepX / 2.0, getY, getZ - facing.getStepZ / 2.0)
    }
  }

  def placeBlock(slot: Int, pos: BlockPos, side: Direction, hitX: Float, hitY: Float, hitZ: Float): Boolean = {
    callUsingItemInSlot(agent.mainInventory, slot, stack => {
      if (shouldCancel(() => fireRightClickBlock(pos, side))) {
        return false
      }

      tryPlaceBlockWhileHandlingFunnySpecialCases(stack, pos, side, hitX, hitY, hitZ)
    }, repair = false)
  }

  def clickBlock(pos: BlockPos, side: Direction): Double = callUsingItemInSlot(agent.equipmentInventory, 0, stack => {
    val state = level.getBlockState(pos)
    val block = state.getBlock

    if (!state.canHarvestBlock(level, pos, this)) return 0

    val hardness = state.getDestroySpeed(level, pos)
    val cobwebOverride = block == Blocks.COBWEB && Settings.get.screwCobwebs

    val strength = getDigSpeed(state, pos)
    val breakTime =
      if (cobwebOverride) Settings.get.swingDelay
      else hardness * 1.5 / strength

    if (breakTime.isInfinity) return 0
    if (breakTime < 0) return breakTime

    val preEvent = new RobotBreakBlockEvent.Pre(agent, level, pos, breakTime * Settings.get.harvestRatio)
    MinecraftForge.EVENT_BUS.post(preEvent)
    if (preEvent.isCanceled) return 0
    val adjustedBreakTime = Math.max(0.05, preEvent.getBreakTime)

    if (!PlayerInteractionManagerHelper.onBlockClicked(this, pos, side)) {
      if (level.isEmptyBlock(pos)) {
        return 1.0 / 20.0
      }
      return 0
    }

    EventHandler.scheduleServer(() => new DamageOverTime(this, pos, side, (adjustedBreakTime * 20).toInt).tick())

    adjustedBreakTime
  })

  private def isItemUseAllowed(stack: ItemStack) = stack.isEmpty || {
    (Settings.get.allowUseItemsWithDuration || stack.getUseDuration <= 0) && !stack.sameItem(new ItemStack(Items.LEAD))
  }

  override def drop(stack: ItemStack, dropAround: Boolean, traceItem: Boolean): ItemEntity =
    InventoryUtils.spawnStackInWorld(BlockPosition(agent), stack, if (dropAround) None else Option(facing))

  private def shouldCancel(f: () => PlayerInteractEvent) = {
    try {
      val event = f()
      event.isCanceled || (event match {
        case rightClick: PlayerInteractEvent.RightClickBlock => rightClick.getUseBlock == Event.Result.DENY || rightClick.getUseItem == Event.Result.DENY
        case leftClick: PlayerInteractEvent.LeftClickBlock => leftClick.getUseBlock == Event.Result.DENY || leftClick.getUseItem == Event.Result.DENY
        case rightClick: PlayerInteractEvent.RightClickItem => rightClick.getResult == Event.Result.DENY
        case _ => false
      })
    }
    catch {
      case t: Throwable =>
        if (!t.getStackTrace.exists(_.getClassName.startsWith("mods.battlegear2."))) {
          OpenComputers.log.warn("Some event handler screwed up!", t)
        }
        false
    }
  }

  private def callUsingItemInSlot[T](inventory: IInventory, slot: Int, f: ItemStack => T, repair: Boolean = true) = {
    val itemsBefore = adjacentItems
    val stack = inventory.getItem(slot)
    val oldStack = stack.copy()
    this.inventory.selected = if (inventory == agent.mainInventory) slot else ~slot
    this.inventory.offhand.set(0, inventory.getItem(slot))
    try {
      f(stack)
    }
    finally {
      this.inventory.selected = 0
      inventory.setItem(slot, this.inventory.offhand.get(0))
      this.inventory.offhand.set(0, ItemStack.EMPTY)
      val newStack = inventory.getItem(slot)
      // this is only possible if f() modified the stack object in-place
      // looking at you, ic2
      if (ItemStack.matches(oldStack, newStack) &&
         !ItemStack.matches(oldStack, stack)) {
        inventory.setItem(slot, stack)
      }
      if (!newStack.isEmpty) {
        if (newStack.getCount <= 0) {
          inventory.setItem(slot, ItemStack.EMPTY)
        }
        if (repair) {
          if (newStack.getCount > 0) tryRepair(newStack, oldStack)
          else ForgeEventFactory.onPlayerDestroyItem(this, newStack, Hand.OFF_HAND)
        }
      }
      collectDroppedItems(itemsBefore)
    }
  }

  private def tryRepair(stack: ItemStack, oldStack: ItemStack) {
    // Only if the underlying type didn't change.
    if (!stack.isEmpty && !oldStack.isEmpty && stack.getItem == oldStack.getItem) {
      val damageRate = new RobotUsedToolEvent.ComputeDamageRate(agent, oldStack, stack, Settings.get.itemDamageRate)
      MinecraftForge.EVENT_BUS.post(damageRate)
      if (damageRate.getDamageRate < 1) {
        MinecraftForge.EVENT_BUS.post(new RobotUsedToolEvent.ApplyDamageRate(agent, oldStack, stack, damageRate.getDamageRate))
      }
    }
  }

  private def tryPlaceBlockWhileHandlingFunnySpecialCases(stack: ItemStack, pos: BlockPos, side: Direction, hitX: Float, hitY: Float, hitZ: Float) = {
    !stack.isEmpty && stack.getCount > 0 && {
      val event = new RobotPlaceBlockEvent.Pre(agent, stack, level, pos)
      MinecraftForge.EVENT_BUS.post(event)
      if (event.isCanceled) false
      else {
        val fakeEyeHeight = if (xRot < 0 && isSomeKindOfPiston(stack)) 1.82 else 0
        setPos(getX, getY - fakeEyeHeight, getZ)
        Player.setPlayerInventoryItems(this)
        val state = level.getBlockState(pos)
        val traceEndPos = new Vector3d(pos.getX + hitX, pos.getY + hitY, pos.getZ + hitZ)
        val traceCtx = if (state.getBlock.isAir(state, level, pos)) BlockRayTraceResult.miss(traceEndPos, side, pos) else new BlockRayTraceResult(traceEndPos, side, pos, false)
        val didPlace = stack.useOn(new ItemUseContext(level, this, Hand.OFF_HAND, stack, traceCtx))
        Player.detectPlayerInventoryChanges(this)
        setPos(getX, getY + fakeEyeHeight, getZ)
        if (didPlace.consumesAction) {
          MinecraftForge.EVENT_BUS.post(new RobotPlaceBlockEvent.Post(agent, stack, level, pos))
        }
        didPlace.consumesAction
      }
    }
  }

  private def isSomeKindOfPiston(stack: ItemStack) =
    stack.getItem match {
      case itemBlock: BlockItem =>
        val block = itemBlock.getBlock
        block != null && block.isInstanceOf[PistonBlock]
      case _ => false
    }

  // ----------------------------------------------------------------------- //

  override def causeFoodExhaustion(amount: Float) {
    if (Settings.get.robotExhaustionCost > 0) {
      agent.machine.node match {
        case connector: Connector => connector.changeBuffer(-Settings.get.robotExhaustionCost * amount)
        case _ => // This shouldn't happen... oh well.
      }
    }
    MinecraftForge.EVENT_BUS.post(new RobotExhaustionEvent(agent, amount))
  }

  override def closeContainer() {}

  override def swing(hand: Hand): Unit = {}

  override protected def getPermissionLevel: Int = {
    val config = server.getPlayerList
    if (config.isOp(getGameProfile)) {
      config.getOps.get(getGameProfile) match {
        case opEntry: OpEntry => opEntry.getLevel
        case _ => server.getOperatorUserPermissionLevel
      }
    }
    else 0
  }

  override def canHarmPlayer(player: PlayerEntity): Boolean = Settings.get.canAttackPlayers

  override def canEat(value: Boolean) = false

  override def canBeAffected(effect: EffectInstance) = false

  override def doHurtTarget(entity: Entity) = false

  override def hurt(source: DamageSource, damage: Float) = false

  override def heal(amount: Float) {}

  override def setHealth(value: Float) {}

  override def remove(invalidate: Boolean): Unit = super.remove(false)

  override def aiStep() {}

  override def take(entity: Entity, count: Int) {}

  override def setLastHurtByMob(entity: LivingEntity) {}

  override def setLastHurtMob(entity: Entity) {}

  override def startRiding(entityIn: Entity, force: Boolean): Boolean = false

  override def startSleepInBed(bedLocation: BlockPos) = Either.left[SleepResult, net.minecraft.util.Unit](SleepResult.OTHER_PROBLEM)

  override def sendMessage(message: ITextComponent, sender: UUID) {}

  override def openCommandBlock(commandBlock: CommandBlockTileEntity): Unit = {}

  override def sendMerchantOffers(containerId: Int, offers: MerchantOffers, villagerLevel: Int, villagerXP: Int, showProgress: Boolean, canRestock: Boolean): Unit = {}

  override def openMenu(guiOwner: INamedContainerProvider) = util.OptionalInt.empty

  override def openMinecartCommandBlock(thing: CommandBlockLogic): Unit = {}

  override def openTextEdit(signTile: SignTileEntity) {}

  // ----------------------------------------------------------------------- //

  class DamageOverTime(val player: Player, val pos: BlockPos, val side: Direction, val ticksTotal: Int) {
    val level: World = player.level
    var ticks = 0
    var lastDamageSent = 0

    def tick(): Unit = {
      // Cancel if the agent stopped or our action is invalidated some other way.
      if (level != player.level || !level.isLoaded(pos) || level.isEmptyBlock(pos) || !player.agent.machine.isRunning) {
        player.gameMode.handleBlockBreakAction(pos, CPlayerDiggingPacket.Action.ABORT_DESTROY_BLOCK, side, 0)
        return
      }

      val damage = 10 * ticks / Math.max(ticksTotal, 1)
      if (damage < 10) {
        ticks += 1
        if (damage != lastDamageSent) {
          lastDamageSent = damage
          if (!PlayerInteractionManagerHelper.updateBlockRemoving(player))
            return
        }
        EventHandler.scheduleServer(() => tick())
      }
      else {
        callUsingItemInSlot(player.agent.equipmentInventory(), 0, _ => {
          this.player.setPos(this.player.getX - side.getStepX / 2.0, this.player.getY, this.player.getZ - side.getStepZ / 2.0)
          val expGained: Int = PlayerInteractionManagerHelper.blockRemoving(player, pos)
          this.player.setPos(this.player.getX + side.getStepX / 2.0, this.player.getY, this.player.getZ + side.getStepZ / 2.0)
          if (expGained >= 0) {
            MinecraftForge.EVENT_BUS.post(new RobotBreakBlockEvent.Post(agent, expGained))
          }
        })
      }
    }
  }
}

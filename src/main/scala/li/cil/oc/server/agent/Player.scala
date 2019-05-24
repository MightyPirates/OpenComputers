package li.cil.oc.server.agent

import java.util
import java.util.UUID

import com.mojang.authlib.GameProfile
import li.cil.oc.OpenComputers
import li.cil.oc.Settings
import li.cil.oc.api.event._
import li.cil.oc.api.internal
import li.cil.oc.api.network.Connector
import li.cil.oc.common.EventHandler
import li.cil.oc.util.BlockPosition
import li.cil.oc.util.InventoryUtils
import net.minecraft.block.BlockPistonBase
import net.minecraft.entity.Entity
import net.minecraft.entity.EntityLivingBase
import net.minecraft.entity.IMerchant
import net.minecraft.entity.item.EntityItem
import net.minecraft.entity.player.EntityPlayer
import net.minecraft.entity.player.EntityPlayer.SleepResult
import net.minecraft.init.Blocks
import net.minecraft.init.Items
import net.minecraft.inventory._
import net.minecraft.item.ItemBlock
import net.minecraft.item.ItemStack
import net.minecraft.network.NetHandlerPlayServer
import net.minecraft.potion.PotionEffect
import net.minecraft.server.management.{PlayerInteractionManager, UserListOpsEntry}
import net.minecraft.tileentity._
import net.minecraft.util.EnumFacing
import net.minecraft.util._
import net.minecraft.util.math.BlockPos
import net.minecraft.util.math.Vec3d
import net.minecraft.util.text.ITextComponent
import net.minecraft.util.text.TextComponentString
import net.minecraft.world.IInteractionObject
import net.minecraft.world.World
import net.minecraft.world.WorldServer
import net.minecraftforge.common.MinecraftForge
import net.minecraftforge.common.util.FakePlayer
import net.minecraftforge.event.ForgeEventFactory
import net.minecraftforge.event.entity.living.LivingEntityUseItemEvent
import net.minecraftforge.event.entity.player.PlayerInteractEvent
import net.minecraftforge.fml.common.ObfuscationReflectionHelper
import net.minecraftforge.fml.common.eventhandler.{Event, EventPriority, SubscribeEvent}
import net.minecraftforge.items.IItemHandler
import net.minecraftforge.items.wrapper._

import scala.collection.convert.WrapAsScala._

object Player {
  def profileFor(agent: internal.Agent): GameProfile = {
    val uuid = agent.ownerUUID
    val randomId = (agent.world.rand.nextInt(0xFFFFFF) + 1).toString
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

  def updatePositionAndRotation(player: Player, facing: EnumFacing, side: EnumFacing) {
    player.facing = facing
    player.side = side
    val direction = new Vec3d(
      facing.getFrontOffsetX + side.getFrontOffsetX,
      facing.getFrontOffsetY + side.getFrontOffsetY,
      facing.getFrontOffsetZ + side.getFrontOffsetZ).normalize()
    val yaw = Math.toDegrees(-Math.atan2(direction.x, direction.z)).toFloat
    val pitch = Math.toDegrees(-Math.atan2(direction.y, Math.sqrt((direction.x * direction.x) + (direction.z * direction.z)))).toFloat * 0.99f
    player.setLocationAndAngles(player.agent.xPosition, player.agent.yPosition, player.agent.zPosition, yaw, pitch)
    player.prevRotationPitch = player.rotationPitch
    player.prevRotationYaw = player.rotationYaw
  }

  def setInventoryPlayerItems(player: Player): Unit = {
    // the offhand is simply the agent's tool item
    val agent = player.agent
    def setCopyOrNull(inv: net.minecraft.util.NonNullList[ItemStack], agentInv: IInventory, slot: Int): Unit = {
      val item = agentInv.getStackInSlot(slot)
      inv(slot) = if (item != null) item.copy() else ItemStack.EMPTY
    }

    for (i <- 0 until 4) {
      setCopyOrNull(player.inventory.armorInventory, agent.equipmentInventory, i)
    }

    // mainInventory is 36 items
    // the agent inventory is 100 items with some space for components
    // leaving us 88..we'll copy what we can
    val size = player.inventory.mainInventory.length min agent.mainInventory.getSizeInventory
    for (i <- 0 until size) {
      setCopyOrNull(player.inventory.mainInventory, agent.mainInventory, i)
    }
    player.inventoryContainer.detectAndSendChanges()
  }

  def detectInventoryPlayerChanges(player: Player): Unit = {
  	val agent = player.agent
    player.inventoryContainer.detectAndSendChanges()
    // The follow code will set agent.inventories = FakePlayer's inv.stack
    def setCopy(inv: IInventory, index: Int, item: ItemStack): Unit = {
      val result = if (item != null) item.copy else ItemStack.EMPTY
      val current = inv.getStackInSlot(index)
      if (!ItemStack.areItemStacksEqual(result, current)) {
        inv.setInventorySlotContents(index, result)
      }
    }
    for (i <- 0 until 4) {
      setCopy(agent.equipmentInventory(), i, player.inventory.armorInventory(i))
    }
    val size = player.inventory.mainInventory.length min agent.mainInventory.getSizeInventory
    for (i <- 0 until size) {
      setCopy(agent.mainInventory, i, player.inventory.mainInventory(i))
    }
  }
}

class Player(val agent: internal.Agent) extends FakePlayer(agent.world.asInstanceOf[WorldServer], Player.profileFor(agent)) {
  connection= new NetHandlerPlayServer(mcServer, FakeNetworkManager, this)

  capabilities.allowFlying = true
  capabilities.disableDamage = true
  capabilities.isFlying = true
  onGround = true

  override def getYOffset = 0.5f

  override def getEyeHeight = 0f

  setSize(1, 1)

  {
    this.inventory = new Inventory(this, agent)
    this.inventory.player = this
    // because the inventory was just overwritten, the container is now detached
    this.inventoryContainer = new AgentContainer(this)
    this.openContainer = this.inventoryContainer

    try {
      ObfuscationReflectionHelper.setPrivateValue(classOf[EntityPlayer], this, new PlayerMainInvWrapper(inventory), "playerMainHandler")
      ObfuscationReflectionHelper.setPrivateValue(classOf[EntityPlayer], this, new CombinedInvWrapper(new PlayerArmorInvWrapper(inventory), new PlayerOffhandInvWrapper(inventory)), "playerEquipmentHandler")
      ObfuscationReflectionHelper.setPrivateValue(classOf[EntityPlayer], this, new PlayerInvWrapper(inventory), "playerJoinedHandler")
    } catch {
      case _: Exception =>
    }
  }

  var facing, side = EnumFacing.SOUTH

  override def getPosition = new BlockPos(posX, posY, posZ)

  override def getDefaultEyeHeight = 0f

  override def getDisplayName = new TextComponentString(agent.name)

  interactionManager.setBlockReachDistance(1)

  // ----------------------------------------------------------------------- //

  def closestEntity[Type <: Entity](clazz: Class[Type], side: EnumFacing = facing): Option[Entity] = {
    val bounds = BlockPosition(agent).offset(side).bounds
    Option(world.findNearestEntityWithinAABB(clazz, bounds, this))
  }

  def entitiesOnSide[Type <: Entity](clazz: Class[Type], side: EnumFacing): util.List[Type] = {
    entitiesInBlock(clazz, BlockPosition(agent).offset(side))
  }

  def entitiesInBlock[Type <: Entity](clazz: Class[Type], blockPos: BlockPosition): util.List[Type] = {
    world.getEntitiesWithinAABB(clazz, blockPos.bounds)
  }

  private def adjacentItems: util.List[EntityItem] = {
    world.getEntitiesWithinAABB(classOf[EntityItem], BlockPosition(agent).bounds.grow(2, 2, 2))
  }

  private def collectDroppedItems(itemsBefore: Iterable[EntityItem]) {
    val itemsAfter = adjacentItems
    val itemsDropped = itemsAfter -- itemsBefore
    for (drop <- itemsDropped) {
      drop.setNoPickupDelay()
      drop.onCollideWithPlayer(this)
      drop.setDead()
    }
  }

  // ----------------------------------------------------------------------- //

  override def attackTargetEntityWithCurrentItem(entity: Entity) {
    callUsingItemInSlot(agent.equipmentInventory, 0, stack => entity match {
      case player: EntityPlayer if !canAttackPlayer(player) => // Avoid player damage.
      case _ =>
        val event = new RobotAttackEntityEvent.Pre(agent, entity)
        MinecraftForge.EVENT_BUS.post(event)
        if (!event.isCanceled) {
          super.attackTargetEntityWithCurrentItem(entity)
          MinecraftForge.EVENT_BUS.post(new RobotAttackEntityEvent.Post(agent, entity))
        }
    })
  }

  override def interactOn(entity: Entity, hand: EnumHand): EnumActionResult = {
    val cancel = try MinecraftForge.EVENT_BUS.post(new PlayerInteractEvent.EntityInteract(this, hand, entity)) catch {
      case t: Throwable =>
        if (!t.getStackTrace.exists(_.getClassName.startsWith("mods.battlegear2."))) {
          OpenComputers.log.warn("Some event handler screwed up!", t)
        }
        false
    }
    if(!cancel && callUsingItemInSlot(agent.equipmentInventory, 0, stack => {
      val result = isItemUseAllowed(stack) && (entity.processInitialInteract(this, hand) || (entity match {
        case living: EntityLivingBase if !getHeldItemMainhand.isEmpty => getHeldItemMainhand.interactWithEntity(this, living, hand)
        case _ => false
      }))
      if (!getHeldItemMainhand.isEmpty && getHeldItemMainhand.getCount <= 0) {
        val orig = getHeldItemMainhand
        this.inventory.setInventorySlotContents(this.inventory.currentItem, ItemStack.EMPTY)
        ForgeEventFactory.onPlayerDestroyItem(this, orig, hand)
      }
      result
    })) EnumActionResult.SUCCESS else EnumActionResult.PASS
  }

  def activateBlockOrUseItem(pos: BlockPos, side: EnumFacing, hitX: Float, hitY: Float, hitZ: Float, duration: Double): ActivationType.Value = {
    callUsingItemInSlot(agent.equipmentInventory, 0, stack => {
      if (shouldCancel(() => fireRightClickBlock(pos, side))) {
        return ActivationType.None
      }

      val item = if (!stack.isEmpty) stack.getItem else null
      if (item != null && item.onItemUseFirst(this, world, pos, side, hitX, hitY, hitZ, EnumHand.OFF_HAND) == EnumActionResult.SUCCESS) {
        return ActivationType.ItemUsed
      }

      val state = world.getBlockState(pos)
      val block = state.getBlock
      val canActivate = block != Blocks.AIR && Settings.get.allowActivateBlocks
      val shouldActivate = canActivate && (!isSneaking || (item == null || item.doesSneakBypassUse(stack, world, pos, this)))
      val result =
        if (shouldActivate && block.onBlockActivated(world, pos, state, this, EnumHand.OFF_HAND, side, hitX, hitY, hitZ))
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

  override def setItemStackToSlot(slotIn: EntityEquipmentSlot, stack: ItemStack): Unit = {
    var superCall: () => Unit = () => super.setItemStackToSlot(slotIn, stack)
    if (slotIn == EntityEquipmentSlot.MAINHAND) {
      agent.equipmentInventory.setInventorySlotContents(0, stack)
      superCall = () => {
        val slot = inventory.currentItem
        // So, if we're not in the main inventory, currentItem is set to -1
        // for compatibility with mods that try accessing the inv directly
        // using inventory.currentItem. See li.cil.oc.server.agent.Inventory
        if(inventory.currentItem < 0) inventory.currentItem = ~inventory.currentItem
        super.setItemStackToSlot(slotIn, stack)
        inventory.currentItem = slot
      }
    } else if(slotIn == EntityEquipmentSlot.OFFHAND) {
      inventory.offHandInventory.set(0, stack)
    }
    superCall()
  }

  override def getItemStackFromSlot(slotIn: EntityEquipmentSlot): ItemStack = {
    if (slotIn == EntityEquipmentSlot.MAINHAND)
      agent.equipmentInventory.getStackInSlot(0)
    else if(slotIn == EntityEquipmentSlot.OFFHAND)
      inventory.offHandInventory.get(0)
    else super.getItemStackFromSlot(slotIn)
  }

  def fireRightClickBlock(pos: BlockPos, side: EnumFacing): PlayerInteractEvent.RightClickBlock = {
    val hitVec = new Vec3d(0.5 + side.getDirectionVec.getX * 0.5, 0.5 + side.getDirectionVec.getY * 0.5, 0.5 + side.getDirectionVec.getZ * 0.5)
    val event = new PlayerInteractEvent.RightClickBlock(this, EnumHand.OFF_HAND, pos, side, hitVec)
    MinecraftForge.EVENT_BUS.post(event)
    event
  }

  def fireLeftClickBlock(pos: BlockPos, side: EnumFacing): PlayerInteractEvent.LeftClickBlock = {
    val hitVec = new Vec3d(0.5 + side.getDirectionVec.getX * 0.5, 0.5 + side.getDirectionVec.getY * 0.5, 0.5 + side.getDirectionVec.getZ * 0.5)
    net.minecraftforge.common.ForgeHooks.onLeftClickBlock(this, pos, side, hitVec)
  }

  def fireRightClickAir(): PlayerInteractEvent.RightClickItem = {
    val event = new PlayerInteractEvent.RightClickItem(this, EnumHand.OFF_HAND)
    MinecraftForge.EVENT_BUS.post(event)
    event
  }

  private def trySetActiveHand(duration: Double): Boolean = {
    stopActiveHand()
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
      setActiveHand(EnumHand.OFF_HAND)
      isHandActive
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

    val maxDuration = stack.getMaxItemUseDuration
    val heldTicks = Math.max(0, Math.min(maxDuration, (duration * 20).toInt))
    agent.machine.pause(heldTicks / 20.0)

    // setting the active hand will also set its initial duration
    val useItemResult = stack.useItemRightClick(world, this, EnumHand.OFF_HAND)
    stopActiveHand()

    if (useItemResult.getType != EnumActionResult.SUCCESS) {
      return false
    }

    val newStack = useItemResult.getResult
    val stackChanged: Boolean =
      !ItemStack.areItemStacksEqual(oldStack, newStack) ||
      !ItemStack.areItemStacksEqual(oldStack, stack)

    if (stackChanged) {
      inventory.offHandInventory.set(0, newStack)
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
    posX += facing.getFrontOffsetX / 2.0
    posZ += facing.getFrontOffsetZ / 2.0

    try {
      useItemWithHand(duration, stackOption.get)
    }
    finally {
      posX -= facing.getFrontOffsetX / 2.0
      posZ -= facing.getFrontOffsetZ / 2.0
    }
  }

  def placeBlock(slot: Int, pos: BlockPos, side: EnumFacing, hitX: Float, hitY: Float, hitZ: Float): Boolean = {
    callUsingItemInSlot(agent.mainInventory, slot, stack => {
      if (shouldCancel(() => fireRightClickBlock(pos, side))) {
        return false
      }

      tryPlaceBlockWhileHandlingFunnySpecialCases(stack, pos, side, hitX, hitY, hitZ)
    }, repair = false)
  }

  def clickBlock(pos: BlockPos, side: EnumFacing): Double = callUsingItemInSlot(agent.equipmentInventory, 0, stack => {
    val state = world.getBlockState(pos)
    val block = state.getBlock

    if (!block.canHarvestBlock(world, pos, this)) return 0

    val hardness = block.getBlockHardness(state, world, pos)
    val cobwebOverride = block == Blocks.WEB && Settings.get.screwCobwebs

    val strength = getDigSpeed(state, pos)
    val breakTime =
      if (cobwebOverride) Settings.get.swingDelay
      else hardness * 1.5 / strength

    if (breakTime.isInfinity) return 0
    if (breakTime < 0) return breakTime

    val preEvent = new RobotBreakBlockEvent.Pre(agent, world, pos, breakTime * Settings.get.harvestRatio)
    MinecraftForge.EVENT_BUS.post(preEvent)
    if (preEvent.isCanceled) return 0
    val adjustedBreakTime = Math.max(0.05, preEvent.getBreakTime)

    if (!PlayerInteractionManagerHelper.onBlockClicked(this, pos, side))
      return 0

    EventHandler.scheduleServer(() => new DamageOverTime(this, pos, side, (adjustedBreakTime * 20).toInt).tick())

    adjustedBreakTime
  })

  private def isItemUseAllowed(stack: ItemStack) = stack.isEmpty || {
    (Settings.get.allowUseItemsWithDuration || stack.getMaxItemUseDuration <= 0) && !stack.isItemEqual(new ItemStack(Items.LEAD))
  }

  override def dropItem(stack: ItemStack, dropAround: Boolean, traceItem: Boolean): EntityItem =
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
    val stack = inventory.getStackInSlot(slot)
    val oldStack = stack.copy()
    this.inventory.currentItem = if (inventory == agent.mainInventory) slot else ~slot
    this.inventory.offHandInventory.set(0, inventory.getStackInSlot(slot))
    try {
      f(stack)
    }
    finally {
      this.inventory.currentItem = 0
      inventory.setInventorySlotContents(slot, this.inventory.offHandInventory.get(0))
      this.inventory.offHandInventory.set(0, ItemStack.EMPTY)
      val newStack = inventory.getStackInSlot(slot)
      // this is only possible if f() modified the stack object in-place
      // looking at you, ic2
      if (ItemStack.areItemStacksEqual(oldStack, newStack) &&
         !ItemStack.areItemStacksEqual(oldStack, stack)) {
        inventory.setInventorySlotContents(slot, stack)
      }
      if (!newStack.isEmpty) {
        if (newStack.getCount <= 0) {
          inventory.setInventorySlotContents(slot, ItemStack.EMPTY)
        }
        if (repair) {
          if (newStack.getCount > 0) tryRepair(newStack, oldStack)
          else ForgeEventFactory.onPlayerDestroyItem(this, newStack, EnumHand.OFF_HAND)
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

  private def tryPlaceBlockWhileHandlingFunnySpecialCases(stack: ItemStack, pos: BlockPos, side: EnumFacing, hitX: Float, hitY: Float, hitZ: Float) = {
    !stack.isEmpty && stack.getCount > 0 && {
      val event = new RobotPlaceBlockEvent.Pre(agent, stack, world, pos)
      MinecraftForge.EVENT_BUS.post(event)
      if (event.isCanceled) false
      else {
        val fakeEyeHeight = if (rotationPitch < 0 && isSomeKindOfPiston(stack)) 1.82 else 0
        setPosition(posX, posY - fakeEyeHeight, posZ)
        Player.setInventoryPlayerItems(this)
        val didPlace = stack.onItemUse(this, world, pos, EnumHand.OFF_HAND, side, hitX, hitY, hitZ)
        Player.detectInventoryPlayerChanges(this)
        setPosition(posX, posY + fakeEyeHeight, posZ)
        if (didPlace == EnumActionResult.SUCCESS) {
          MinecraftForge.EVENT_BUS.post(new RobotPlaceBlockEvent.Post(agent, stack, world, pos))
        }
        didPlace == EnumActionResult.SUCCESS
      }
    }
  }

  private def isSomeKindOfPiston(stack: ItemStack) =
    stack.getItem match {
      case itemBlock: ItemBlock =>
        val block = itemBlock.getBlock
        block != null && block.isInstanceOf[BlockPistonBase]
      case _ => false
    }

  // ----------------------------------------------------------------------- //

  override def addExhaustion(amount: Float) {
    if (Settings.get.robotExhaustionCost > 0) {
      agent.machine.node match {
        case connector: Connector => connector.changeBuffer(-Settings.get.robotExhaustionCost * amount)
        case _ => // This shouldn't happen... oh well.
      }
    }
    MinecraftForge.EVENT_BUS.post(new RobotExhaustionEvent(agent, amount))
  }

  override def closeScreen() {}

  override def swingArm(hand: EnumHand): Unit = {}

  override def canUseCommand(level: Int, command: String): Boolean = {
    ("seed" == command && !mcServer.isDedicatedServer) ||
      "tell" == command ||
      "help" == command ||
      "me" == command || {
      val config = mcServer.getPlayerList
      config.canSendCommands(getGameProfile) && {
        config.getOppedPlayers.getEntry(getGameProfile) match {
          case opEntry: UserListOpsEntry => opEntry.getPermissionLevel >= level
          case _ => mcServer.getOpPermissionLevel >= level
        }
      }
    }
  }

  override def canAttackPlayer(player: EntityPlayer): Boolean = Settings.get.canAttackPlayers

  override def canEat(value: Boolean) = false

  override def isPotionApplicable(effect: PotionEffect) = false

  override def attackEntityAsMob(entity: Entity) = false

  override def attackEntityFrom(source: DamageSource, damage: Float) = false

  override def heal(amount: Float) {}

  override def setHealth(value: Float) {}

  override def setDead(): Unit = isDead = true

  override def onLivingUpdate() {}

  override def onItemPickup(entity: Entity, count: Int) {}

  override def setRevengeTarget(entity: EntityLivingBase) {}

  override def setLastAttackedEntity(entity: Entity) {}

  override def startRiding(entityIn: Entity, force: Boolean): Boolean = false

  override def trySleep(bedLocation: BlockPos) = SleepResult.OTHER_PROBLEM

  override def sendMessage(message: ITextComponent) {}

  override def displayGUIChest(inventory: IInventory) {}

  override def displayGuiCommandBlock(commandBlock: TileEntityCommandBlock): Unit = {}

  override def displayVillagerTradeGui(villager: IMerchant): Unit = {
    villager.setCustomer(null)
  }

  override def displayGui(guiOwner: IInteractionObject) {}

  override def displayGuiEditCommandCart(thing: CommandBlockBaseLogic): Unit = {}

  override def openEditSign(signTile: TileEntitySign) {}

  // ----------------------------------------------------------------------- //

  class DamageOverTime(val player: Player, val pos: BlockPos, val side: EnumFacing, val ticksTotal: Int) {
    val world: World = player.world
    var ticks = 0
    var lastDamageSent = 0

    def tick(): Unit = {
      // Cancel if the agent stopped or our action is invalidated some other way.
      if (world != player.world || !world.isBlockLoaded(pos) || world.isAirBlock(pos) || !player.agent.machine.isRunning) {
        player.interactionManager.cancelDestroyingBlock()
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
          this.player.posX -= side.getFrontOffsetX / 2.0
          this.player.posZ -= side.getFrontOffsetZ / 2.0
          val expGained: Int = PlayerInteractionManagerHelper.blockRemoving(player, pos)
          this.player.posX += side.getFrontOffsetX / 2.0
          this.player.posZ += side.getFrontOffsetZ / 2.0
          if (expGained >= 0) {
            MinecraftForge.EVENT_BUS.post(new RobotBreakBlockEvent.Post(agent, expGained))
          }
        })
      }
    }
  }
}

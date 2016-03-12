package li.cil.oc.server.agent

import java.util.UUID

import com.mojang.authlib.GameProfile
import li.cil.oc.OpenComputers
import li.cil.oc.Settings
import li.cil.oc.api.event._
import li.cil.oc.api.internal
import li.cil.oc.api.network.Connector
import li.cil.oc.common.EventHandler
import li.cil.oc.integration.Mods
import li.cil.oc.integration.util.PortalGun
import li.cil.oc.util.BlockPosition
import li.cil.oc.util.InventoryUtils
import net.minecraft.block.Block
import net.minecraft.block.BlockPistonBase
import net.minecraft.entity.Entity
import net.minecraft.entity.EntityLivingBase
import net.minecraft.entity.IMerchant
import net.minecraft.entity.item.EntityItem
import net.minecraft.entity.passive.EntityHorse
import net.minecraft.entity.player.EntityPlayer
import net.minecraft.entity.player.EntityPlayer.EnumStatus
import net.minecraft.init.Blocks
import net.minecraft.init.Items
import net.minecraft.inventory.IInventory
import net.minecraft.item.ItemBlock
import net.minecraft.item.ItemStack
import net.minecraft.network.NetHandlerPlayServer
import net.minecraft.potion.PotionEffect
import net.minecraft.server.MinecraftServer
import net.minecraft.server.management.UserListOpsEntry
import net.minecraft.tileentity._
import net.minecraft.util.EnumFacing
import net.minecraft.util._
import net.minecraft.world.IInteractionObject
import net.minecraft.world.WorldServer
import net.minecraftforge.common.ForgeHooks
import net.minecraftforge.common.MinecraftForge
import net.minecraftforge.common.util.FakePlayer
import net.minecraftforge.event.ForgeEventFactory
import net.minecraftforge.event.entity.player.EntityInteractEvent
import net.minecraftforge.event.entity.player.PlayerInteractEvent
import net.minecraftforge.event.entity.player.PlayerInteractEvent.Action
import net.minecraftforge.event.world.BlockEvent
import net.minecraftforge.fluids.FluidRegistry
import net.minecraftforge.fml.common.ObfuscationReflectionHelper
import net.minecraftforge.fml.common.eventhandler.Event

import scala.collection.convert.WrapAsScala._

object Player {
  def profileFor(agent: internal.Agent) = {
    val uuid = agent.ownerUUID
    val randomId = (agent.world.rand.nextInt(0xFFFFFF) + 1).toString
    val name = Settings.get.nameFormat.
      replace("$player$", agent.ownerName).
      replace("$random$", randomId)
    new GameProfile(uuid, name)
  }

  def determineUUID(playerUUID: Option[UUID] = None) = {
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
    val direction = new Vec3(
      facing.getFrontOffsetX + side.getFrontOffsetX,
      facing.getFrontOffsetY + side.getFrontOffsetY,
      facing.getFrontOffsetZ + side.getFrontOffsetZ).normalize()
    val yaw = Math.toDegrees(-Math.atan2(direction.xCoord, direction.zCoord)).toFloat
    val pitch = Math.toDegrees(-Math.atan2(direction.yCoord, Math.sqrt((direction.xCoord * direction.xCoord) + (direction.zCoord * direction.zCoord)))).toFloat * 0.99f
    player.setLocationAndAngles(player.agent.xPosition, player.agent.yPosition, player.agent.zPosition, yaw, pitch)
    player.prevRotationPitch = player.rotationPitch
    player.prevRotationYaw = player.rotationYaw
  }
}

class Player(val agent: internal.Agent) extends FakePlayer(agent.world.asInstanceOf[WorldServer], Player.profileFor(agent)) {
  playerNetServerHandler = new NetHandlerPlayServer(mcServer, FakeNetworkManager, this)

  capabilities.allowFlying = true
  capabilities.disableDamage = true
  capabilities.isFlying = true
  onGround = true

  override def getYOffset = 0.5f

  override def getEyeHeight = 0f

  setSize(1, 1)

  {
    val inventory = new Inventory(agent)
    if (Mods.BattleGear2.isAvailable) {
      ObfuscationReflectionHelper.setPrivateValue(classOf[EntityPlayer], this, inventory, "inventory", "field_71071_by", "bm")
    }
    else this.inventory = inventory
  }

  var facing, side = EnumFacing.SOUTH

  var customItemInUseBecauseMinecraftIsBloodyStupidAndMakesRandomMethodsClientSided: ItemStack = _

  def world = agent.world

  override def getPosition = new BlockPos(posX, posY, posZ)

  override def getDefaultEyeHeight = 0f

  override def getDisplayName = new ChatComponentText(agent.name)

  theItemInWorldManager.setBlockReachDistance(1)

  // ----------------------------------------------------------------------- //

  def closestEntity[Type <: Entity](clazz: Class[Type], side: EnumFacing = facing) = {
    val bounds = BlockPosition(agent).offset(side).bounds
    Option(world.findNearestEntityWithinAABB(clazz, bounds, this))
  }

  def entitiesOnSide[Type <: Entity](clazz: Class[Type], side: EnumFacing) = {
    entitiesInBlock(clazz, BlockPosition(agent).offset(side))
  }

  def entitiesInBlock[Type <: Entity](clazz: Class[Type], blockPos: BlockPosition) = {
    world.getEntitiesWithinAABB(clazz, blockPos.bounds)
  }

  private def adjacentItems = {
    world.getEntitiesWithinAABB(classOf[EntityItem], BlockPosition(agent).bounds.expand(2, 2, 2))
  }

  private def collectDroppedItems(itemsBefore: Iterable[EntityItem]) {
    val itemsAfter = adjacentItems
    val itemsDropped = itemsAfter -- itemsBefore
    for (drop <- itemsDropped) {
      drop.setNoPickupDelay()
      drop.onCollideWithPlayer(this)
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

  override def interactWith(entity: Entity) = {
    val cancel = try MinecraftForge.EVENT_BUS.post(new EntityInteractEvent(this, entity)) catch {
      case t: Throwable =>
        if (!t.getStackTrace.exists(_.getClassName.startsWith("mods.battlegear2."))) {
          OpenComputers.log.warn("Some event handler screwed up!", t)
        }
        false
    }
    !cancel && callUsingItemInSlot(agent.equipmentInventory, 0, stack => {
      val result = isItemUseAllowed(stack) && (entity.interactFirst(this) || (entity match {
        case living: EntityLivingBase if getHeldItem != null => getHeldItem.interactWithEntity(this, living)
        case _ => false
      }))
      if (getHeldItem != null && getHeldItem.stackSize <= 0) {
        destroyCurrentEquippedItem()
      }
      result
    })
  }

  def activateBlockOrUseItem(pos: BlockPos, side: EnumFacing, hitX: Float, hitY: Float, hitZ: Float, duration: Double): ActivationType.Value = {
    callUsingItemInSlot(agent.equipmentInventory, 0, stack => {
      if (shouldCancel(() => ForgeEventFactory.onPlayerInteract(this, Action.RIGHT_CLICK_BLOCK, world, pos, side))) {
        return ActivationType.None
      }

      val item = if (stack != null) stack.getItem else null
      if (!PortalGun.isPortalGun(stack)) {
        if (item != null && item.onItemUseFirst(stack, this, world, pos, side, hitX, hitY, hitZ)) {
          return ActivationType.ItemUsed
        }
      }

      val state = world.getBlockState(pos)
      val block = state.getBlock
      val canActivate = block != Blocks.air && Settings.get.allowActivateBlocks
      val shouldActivate = canActivate && (!isSneaking || (item == null || item.doesSneakBypassUse(world, pos, this)))
      val result =
        if (shouldActivate && block.onBlockActivated(world, pos, state, this, side, hitX, hitY, hitZ))
          ActivationType.BlockActivated
        else if (isItemUseAllowed(stack) && tryPlaceBlockWhileHandlingFunnySpecialCases(stack, pos, side, hitX, hitY, hitZ))
          ActivationType.ItemPlaced
        else if (tryUseItem(stack, duration))
          ActivationType.ItemUsed
        else
          ActivationType.None

      result
    })
  }

  def useEquippedItem(duration: Double) = {
    callUsingItemInSlot(agent.equipmentInventory, 0, stack => {
      if (!shouldCancel(() => ForgeEventFactory.onPlayerInteract(this, Action.RIGHT_CLICK_AIR, world, getPosition, facing))) {
        tryUseItem(stack, duration)
      }
      else false
    })
  }

  private def tryUseItem(stack: ItemStack, duration: Double) = {
    clearItemInUse()
    stack != null && stack.stackSize > 0 && isItemUseAllowed(stack) && {
      val oldSize = stack.stackSize
      val oldDamage = if (stack != null) stack.getItemDamage else 0
      val oldData = if (stack.hasTagCompound) stack.getTagCompound.copy() else null
      val heldTicks = math.max(0, math.min(stack.getMaxItemUseDuration, (duration * 20).toInt))
      // Change the offset at which items are used, to avoid hitting
      // the robot itself (e.g. with bows, potions, mining laser, ...).
      val offset = facing
      posX += offset.getFrontOffsetX * 0.6
      posY += offset.getFrontOffsetY * 0.6
      posZ += offset.getFrontOffsetZ * 0.6
      val newStack = stack.useItemRightClick(world, this)
      if (isUsingItem) {
        val remaining = customItemInUseBecauseMinecraftIsBloodyStupidAndMakesRandomMethodsClientSided.getMaxItemUseDuration - heldTicks
        customItemInUseBecauseMinecraftIsBloodyStupidAndMakesRandomMethodsClientSided.onPlayerStoppedUsing(world, this, remaining)
        clearItemInUse()
      }
      posX -= offset.getFrontOffsetX * 0.6
      posY -= offset.getFrontOffsetY * 0.6
      posZ -= offset.getFrontOffsetZ * 0.6
      agent.machine.pause(heldTicks / 20.0)
      // These are functions to avoid null pointers if newStack is null.
      def sizeOrDamageChanged = newStack.stackSize != oldSize || newStack.getItemDamage != oldDamage
      def tagChanged = (oldData == null && newStack.hasTagCompound) || (oldData != null && !newStack.hasTagCompound) ||
        (oldData != null && newStack.hasTagCompound && !oldData.equals(newStack.getTagCompound))
      val stackChanged = newStack != stack || (newStack != null && (sizeOrDamageChanged || tagChanged || PortalGun.isStandardPortalGun(stack)))
      if (stackChanged) {
        agent.equipmentInventory.setInventorySlotContents(0, newStack)
      }
      stackChanged
    }
  }

  def placeBlock(slot: Int, pos: BlockPos, side: EnumFacing, hitX: Float, hitY: Float, hitZ: Float): Boolean = {
    callUsingItemInSlot(agent.mainInventory, slot, stack => {
      if (shouldCancel(() => ForgeEventFactory.onPlayerInteract(this, Action.RIGHT_CLICK_BLOCK, world, pos, side))) {
        return false
      }

      tryPlaceBlockWhileHandlingFunnySpecialCases(stack, pos, side, hitX, hitY, hitZ)
    }, repair = false)
  }

  def clickBlock(pos: BlockPos, side: EnumFacing, immediate: Boolean = false): Double = {
    callUsingItemInSlot(agent.equipmentInventory, 0, stack => {
      if (shouldCancel(() => ForgeEventFactory.onPlayerInteract(this, Action.LEFT_CLICK_BLOCK, world, pos, side))) {
        return 0
      }

      if (MinecraftServer.getServer.isBlockProtected(world, pos, this)) {
        return 0
      }

      val state = world.getBlockState(pos)
      val block = state.getBlock
      val metadata = block.getMetaFromState(state)
      val mayClickBlock = block != null
      val canClickBlock = mayClickBlock &&
        !block.isAir(world, pos) &&
        FluidRegistry.lookupFluidForBlock(block) == null
      if (!canClickBlock) {
        return 0
      }

      val breakEvent = new BlockEvent.BreakEvent(world, pos, state, this)
      MinecraftForge.EVENT_BUS.post(breakEvent)
      if (breakEvent.isCanceled) {
        return 0
      }

      block.onBlockClicked(world, pos, this)
      world.extinguishFire(this, pos, side)

      val hardness = block.getBlockHardness(world, pos)
      val isBlockUnbreakable = hardness < 0
      val canDestroyBlock = !isBlockUnbreakable && block.canEntityDestroy(world, pos, this)
      if (!canDestroyBlock) {
        return 0
      }

      if (world.getWorldInfo.getGameType.isAdventure && !canPlayerEdit(pos, side, stack)) {
        return 0
      }

      val cobwebOverride = block == Blocks.web && Settings.get.screwCobwebs

      if (!ForgeHooks.canHarvestBlock(block, this, world, pos) && !cobwebOverride) {
        return 0
      }

      val strength = getBreakSpeed(state, pos)
      val breakTime =
        if (cobwebOverride) Settings.get.swingDelay
        else hardness * 1.5 / strength

      if (breakTime.isInfinity) return 0

      val preEvent = new RobotBreakBlockEvent.Pre(agent, world, pos, breakTime * Settings.get.harvestRatio)
      MinecraftForge.EVENT_BUS.post(preEvent)
      if (preEvent.isCanceled) return 0
      val adjustedBreakTime = math.max(0.05, preEvent.getBreakTime)

      // Special handling for Tinkers Construct - tools like the hammers do
      // their break logic in onBlockStartBreak but return true to cancel
      // further processing. We also need to adjust our offset for their ray-
      // tracing implementation.
      val needsSpecialPlacement = false // ModTinkersConstruct.isInfiTool(stack) || ModMagnanimousTools.isMagTool(stack) // TODO TCon / MagTools
      if (needsSpecialPlacement) {
        posY -= 1.62
        prevPosY = posY
      }
      val cancel = stack != null && stack.getItem.onBlockStartBreak(stack, pos, this)
      if (cancel && needsSpecialPlacement) {
        posY += 1.62
        prevPosY = posY
        return adjustedBreakTime
      }
      if (cancel) {
        return 0
      }

      if (!immediate) {
        EventHandler.scheduleServer(() => new DamageOverTime(this, pos, side, (adjustedBreakTime * 20).toInt).tick())
        return adjustedBreakTime
      }

      world.sendBlockBreakProgress(-1, pos, -1)

      world.playAuxSFXAtEntity(this, 2001, pos, Block.getIdFromBlock(block) + (metadata << 12))

      if (stack != null) {
        stack.onBlockDestroyed(world, block, pos, this)
      }

      val te = world.getTileEntity(pos)
      val canHarvest = block.canHarvestBlock(world, pos, this)
      block.onBlockHarvested(world, pos, state, this)
      if (block.removedByPlayer(world, pos, this, block.canHarvestBlock(world, pos, this))) {
        block.onBlockDestroyedByPlayer(world, pos, state)
        if (canHarvest) {
          block.harvestBlock(world, this, pos, state, te)
          MinecraftForge.EVENT_BUS.post(new RobotBreakBlockEvent.Post(agent, breakEvent.getExpToDrop))
        }
        else if (stack != null) {
          MinecraftForge.EVENT_BUS.post(new RobotBreakBlockEvent.Post(agent, 0))
        }
        return adjustedBreakTime
      }
      0
    })
  }

  private def isItemUseAllowed(stack: ItemStack) = stack == null || {
    (Settings.get.allowUseItemsWithDuration || stack.getMaxItemUseDuration <= 0) &&
      (!PortalGun.isPortalGun(stack) || PortalGun.isStandardPortalGun(stack)) &&
      !stack.isItemEqual(new ItemStack(Items.lead))
  }

  override def dropPlayerItemWithRandomChoice(stack: ItemStack, inPlace: Boolean) =
    InventoryUtils.spawnStackInWorld(BlockPosition(agent), stack, if (inPlace) None else Option(facing))

  private def shouldCancel(f: () => PlayerInteractEvent) = {
    try {
      val event = f()
      event.isCanceled || event.useBlock == Event.Result.DENY || event.useItem == Event.Result.DENY
    }
    catch {
      case t: Throwable =>
        if (!t.getStackTrace.exists(_.getClassName.startsWith("mods.battlegear2."))) {
          OpenComputers.log.warn("Some event handler screwed up!", t)
        }
        false
    }
  }

  private def callUsingItemInSlot[T](inventory: IInventory, slot: Int, f: (ItemStack) => T, repair: Boolean = true) = {
    val itemsBefore = adjacentItems
    val stack = inventory.getStackInSlot(slot)
    val oldStack = if (stack != null) stack.copy() else null
    this.inventory.currentItem = if (inventory == agent.mainInventory) slot else ~slot
    try {
      f(stack)
    }
    finally {
      this.inventory.currentItem = 0
      val newStack = inventory.getStackInSlot(slot)
      if (newStack != null) {
        if (newStack.stackSize <= 0) {
          inventory.setInventorySlotContents(slot, null)
        }
        if (repair) {
          if (newStack.stackSize > 0) tryRepair(newStack, oldStack)
          else ForgeEventFactory.onPlayerDestroyItem(this, newStack)
        }
      }
      collectDroppedItems(itemsBefore)
    }
  }

  private def tryRepair(stack: ItemStack, oldStack: ItemStack) {
    // Only if the underlying type didn't change.
    if (stack != null && oldStack != null && stack.getItem == oldStack.getItem) {
      val damageRate = new RobotUsedToolEvent.ComputeDamageRate(agent, oldStack, stack, Settings.get.itemDamageRate)
      MinecraftForge.EVENT_BUS.post(damageRate)
      if (damageRate.getDamageRate < 1) {
        MinecraftForge.EVENT_BUS.post(new RobotUsedToolEvent.ApplyDamageRate(agent, oldStack, stack, damageRate.getDamageRate))
      }
    }
  }

  private def tryPlaceBlockWhileHandlingFunnySpecialCases(stack: ItemStack, pos: BlockPos, side: EnumFacing, hitX: Float, hitY: Float, hitZ: Float) = {
    stack != null && stack.stackSize > 0 && {
      val event = new RobotPlaceBlockEvent.Pre(agent, stack, world, pos)
      MinecraftForge.EVENT_BUS.post(event)
      if (event.isCanceled) false
      else {
        val fakeEyeHeight = if (rotationPitch < 0 && isSomeKindOfPiston(stack)) 1.82 else 0
        setPosition(posX, posY - fakeEyeHeight, posZ)
        val didPlace = stack.onItemUse(this, world, pos, side, hitX, hitY, hitZ)
        setPosition(posX, posY + fakeEyeHeight, posZ)
        if (didPlace) {
          MinecraftForge.EVENT_BUS.post(new RobotPlaceBlockEvent.Post(agent, stack, world, pos))
        }
        didPlace
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

  override def setItemInUse(stack: ItemStack, useDuration: Int) {
    super.setItemInUse(stack, useDuration)
    customItemInUseBecauseMinecraftIsBloodyStupidAndMakesRandomMethodsClientSided = stack
  }

  override def clearItemInUse() {
    super.clearItemInUse()
    customItemInUseBecauseMinecraftIsBloodyStupidAndMakesRandomMethodsClientSided = null
  }

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

  override def swingItem() {}

  override def canCommandSenderUseCommand(level: Int, command: String): Boolean = {
    ("seed" == command && !mcServer.isDedicatedServer) ||
      "tell" == command ||
      "help" == command ||
      "me" == command || {
      val config = mcServer.getConfigurationManager
      config.canSendCommands(getGameProfile) && {
        config.getOppedPlayers.getEntry(getGameProfile) match {
          case opEntry: UserListOpsEntry => opEntry.getPermissionLevel >= level
          case _ => mcServer.getOpPermissionLevel >= level
        }
      }
    }
  }

  override def canAttackPlayer(player: EntityPlayer) = Settings.get.canAttackPlayers

  override def canEat(value: Boolean) = false

  override def isPotionApplicable(effect: PotionEffect) = false

  override def attackEntityAsMob(entity: Entity) = false

  override def attackEntityFrom(source: DamageSource, damage: Float) = false

  override def heal(amount: Float) {}

  override def setHealth(value: Float) {}

  override def setDead() = isDead = true

  override def onLivingUpdate() {}

  override def onItemPickup(entity: Entity, count: Int) {}

  override def setCurrentItemOrArmor(slot: Int, stack: ItemStack): Unit = {
    if (slot == 0 && agent.equipmentInventory.getSizeInventory > 0) {
      agent.equipmentInventory.setInventorySlotContents(slot, stack)
    }
    // else: armor slots, which are unsupported in agents.
  }

  override def setRevengeTarget(entity: EntityLivingBase) {}

  override def setLastAttacker(entity: Entity) {}

  override def mountEntity(entity: Entity) {}

  override def trySleep(bedLocation: BlockPos) = EnumStatus.OTHER_PROBLEM

  override def addChatMessage(message: IChatComponent) {}

  override def displayGUIChest(inventory: IInventory) {}

  override def displayGUIBook(bookStack: ItemStack) {}

  override def displayVillagerTradeGui(villager: IMerchant): Unit = {
    villager.setCustomer(null)
  }

  override def displayGui(guiOwner: IInteractionObject) {}

  override def displayGUIHorse(horse: EntityHorse, inventory: IInventory) {}

  override def openEditSign(signTile: TileEntitySign) {}

  // ----------------------------------------------------------------------- //

  class DamageOverTime(val player: Player, val pos: BlockPos, val side: EnumFacing, val ticksTotal: Int) {
    val world = player.world
    var ticks = 0
    var lastDamageSent = 0

    def tick(): Unit = {
      // Cancel if the agent stopped or our action is invalidated some other way.
      if (world != player.world || !world.isBlockLoaded(pos) || world.isAirBlock(pos) || !player.agent.machine.isRunning) {
        world.sendBlockBreakProgress(-1, pos, -1)
        return
      }

      val damage = 10 * ticks / math.max(ticksTotal, 1)
      if (damage >= 10) {
        player.clickBlock(pos, side, immediate = true)
      }
      else {
        ticks += 1
        if (damage != lastDamageSent) {
          lastDamageSent = damage
          world.sendBlockBreakProgress(-1, pos, damage)
        }
        EventHandler.scheduleServer(() => tick())
      }
    }
  }

}

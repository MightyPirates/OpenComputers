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
import net.minecraft.entity.player.EntityPlayer
import net.minecraft.entity.player.EntityPlayer.SleepResult
import net.minecraft.init.Blocks
import net.minecraft.init.Items
import net.minecraft.inventory.IInventory
import net.minecraft.item.ItemBlock
import net.minecraft.item.ItemStack
import net.minecraft.network.NetHandlerPlayServer
import net.minecraft.potion.PotionEffect
import net.minecraft.server.management.UserListOpsEntry
import net.minecraft.tileentity._
import net.minecraft.util.EnumFacing
import net.minecraft.util._
import net.minecraft.util.math.BlockPos
import net.minecraft.util.math.Vec3d
import net.minecraft.util.text.ITextComponent
import net.minecraft.util.text.TextComponentString
import net.minecraft.world.IInteractionObject
import net.minecraft.world.WorldServer
import net.minecraftforge.common.ForgeHooks
import net.minecraftforge.common.MinecraftForge
import net.minecraftforge.common.util.FakePlayer
import net.minecraftforge.event.ForgeEventFactory
import net.minecraftforge.event.entity.player.PlayerInteractEvent
import net.minecraftforge.event.world.BlockEvent
import net.minecraftforge.fluids.FluidRegistry
import net.minecraftforge.fml.common.FMLCommonHandler
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
    val direction = new Vec3d(
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
  connection= new NetHandlerPlayServer(mcServer, FakeNetworkManager, this)

  capabilities.allowFlying = true
  capabilities.disableDamage = true
  capabilities.isFlying = true
  onGround = true

  override def getYOffset = 0.5f

  override def getEyeHeight = 0f

  setSize(1, 1)

  {
    val inventory = new Inventory(this, agent)
    if (Mods.BattleGear2.isModAvailable) {
      ObfuscationReflectionHelper.setPrivateValue(classOf[EntityPlayer], this, inventory, "inventory", "field_71071_by", "bm")
    }
    else this.inventory = inventory
  }

  var facing, side = EnumFacing.SOUTH

  def world = agent.world

  override def getPosition = new BlockPos(posX, posY, posZ)

  override def getDefaultEyeHeight = 0f

  override def getDisplayName = new TextComponentString(agent.name)

  interactionManager.setBlockReachDistance(1)

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

  override def interact(entity: Entity, stack: ItemStack, hand: EnumHand): EnumActionResult = {
    val cancel = try MinecraftForge.EVENT_BUS.post(new PlayerInteractEvent.EntityInteract(this, EnumHand.MAIN_HAND, getHeldItemMainhand, entity)) catch {
      case t: Throwable =>
        if (!t.getStackTrace.exists(_.getClassName.startsWith("mods.battlegear2."))) {
          OpenComputers.log.warn("Some event handler screwed up!", t)
        }
        false
    }
    if(!cancel && callUsingItemInSlot(agent.equipmentInventory, 0, stack => {
      val result = isItemUseAllowed(stack) && (entity.processInitialInteract(this, stack, EnumHand.MAIN_HAND) || (entity match {
        case living: EntityLivingBase if getHeldItemMainhand != null => getHeldItemMainhand.interactWithEntity(this, living, EnumHand.MAIN_HAND)
        case _ => false
      }))
      if (getHeldItemMainhand != null && getHeldItemMainhand.stackSize <= 0) {
        val orig = getHeldItemMainhand
        this.inventory.setInventorySlotContents(this.inventory.currentItem, null)
        ForgeEventFactory.onPlayerDestroyItem(this, orig, EnumHand.MAIN_HAND)
      }
      result
    })) EnumActionResult.SUCCESS else EnumActionResult.PASS
  }

  def activateBlockOrUseItem(pos: BlockPos, side: EnumFacing, hitX: Float, hitY: Float, hitZ: Float, duration: Double): ActivationType.Value = {
    callUsingItemInSlot(agent.equipmentInventory, 0, stack => {
      if (shouldCancel(() => fireRightClickBlock(pos, side))) {
        return ActivationType.None
      }

      val item = if (stack != null) stack.getItem else null
      if (!PortalGun.isPortalGun(stack)) {
        if (item != null && item.onItemUseFirst(stack, this, world, pos, side, hitX, hitY, hitZ, EnumHand.MAIN_HAND) == EnumActionResult.SUCCESS) {
          return ActivationType.ItemUsed
        }
      }

      val state = world.getBlockState(pos)
      val block = state.getBlock
      val canActivate = block != Blocks.AIR && Settings.get.allowActivateBlocks
      val shouldActivate = canActivate && (!isSneaking || (item == null || item.doesSneakBypassUse(stack, world, pos, this)))
      val result =
        if (shouldActivate && block.onBlockActivated(world, pos, state, this, EnumHand.MAIN_HAND, stack, side, hitX, hitY, hitZ))
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

  def fireRightClickBlock(pos: BlockPos, side: EnumFacing): PlayerInteractEvent.RightClickBlock = {
    val hitVec = new Vec3d(0.5 + side.getDirectionVec.getX * 0.5, 0.5 + side.getDirectionVec.getY * 0.5, 0.5 + side.getDirectionVec.getZ * 0.5)
    val event = new PlayerInteractEvent.RightClickBlock(this, EnumHand.MAIN_HAND, getHeldItemMainhand, pos, side, hitVec)
    MinecraftForge.EVENT_BUS.post(event)
    event
  }

  def fireLeftClickBlock(pos: BlockPos, side: EnumFacing): PlayerInteractEvent.LeftClickBlock = {
    val hitVec = new Vec3d(0.5 + side.getDirectionVec.getX * 0.5, 0.5 + side.getDirectionVec.getY * 0.5, 0.5 + side.getDirectionVec.getZ * 0.5)
    val event = new PlayerInteractEvent.LeftClickBlock(this, pos, side, hitVec)
    MinecraftForge.EVENT_BUS.post(event)
    event
  }

  def fireRightClickAir(): PlayerInteractEvent.RightClickItem = {
    val event = new PlayerInteractEvent.RightClickItem(this, EnumHand.MAIN_HAND, getHeldItemMainhand)
    MinecraftForge.EVENT_BUS.post(event)
    event
  }

  def useEquippedItem(duration: Double) = {
    callUsingItemInSlot(agent.equipmentInventory, 0, stack => {
      if (!shouldCancel(() => fireRightClickAir())) {
        tryUseItem(stack, duration)
      }
      else false
    })
  }

  private def tryUseItem(stack: ItemStack, duration: Double) = {
    stopActiveHand()
    stack != null && stack.stackSize > 0 && isItemUseAllowed(stack) && {
      val oldSize = stack.stackSize
      val oldDamage = if (stack != null) stack.getItemDamage else 0
      val oldData = if (stack.hasTagCompound) stack.getTagCompound.copy() else null
      val heldTicks = Math.max(0, Math.min(stack.getMaxItemUseDuration, (duration * 20).toInt))
      // Change the offset at which items are used, to avoid hitting
      // the robot itself (e.g. with bows, potions, mining laser, ...).
      val offset = facing
      posX += offset.getFrontOffsetX * 0.6
      posY += offset.getFrontOffsetY * 0.6
      posZ += offset.getFrontOffsetZ * 0.6
      val newStack = stack.useItemRightClick(world, this, EnumHand.MAIN_HAND).getResult
      if (isHandActive) {
        getActiveItemStack
        val remaining = getActiveItemStack.getMaxItemUseDuration - heldTicks
        getActiveItemStack.onPlayerStoppedUsing(world, this, remaining)
        stopActiveHand()
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
      if (shouldCancel(() => fireRightClickBlock(pos, side))) {
        return false
      }

      tryPlaceBlockWhileHandlingFunnySpecialCases(stack, pos, side, hitX, hitY, hitZ)
    }, repair = false)
  }

  def clickBlock(pos: BlockPos, side: EnumFacing, immediate: Boolean = false): Double = {
    callUsingItemInSlot(agent.equipmentInventory, 0, stack => {
      if (shouldCancel(() => fireLeftClickBlock(pos, side))) {
        return 0
      }

      if (FMLCommonHandler.instance.getMinecraftServerInstance.isBlockProtected(world, pos, this)) {
        return 0
      }

      val state = world.getBlockState(pos)
      val block = state.getBlock
      val metadata = block.getMetaFromState(state)
      val mayClickBlock = block != null
      val canClickBlock = mayClickBlock &&
        !block.isAir(state, world, pos) &&
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

      val hardness = block.getBlockHardness(state, world, pos)
      val isBlockUnbreakable = hardness < 0
      val canDestroyBlock = !isBlockUnbreakable && block.canEntityDestroy(state, world, pos, this)
      if (!canDestroyBlock) {
        return 0
      }

      if (world.getWorldInfo.getGameType.isAdventure && !canPlayerEdit(pos, side, stack)) {
        return 0
      }

      val cobwebOverride = block == Blocks.WEB && Settings.get.screwCobwebs

      if (!ForgeHooks.canHarvestBlock(block, this, world, pos) && !cobwebOverride) {
        return 0
      }

      val strength = getDigSpeed(state, pos)
      val breakTime =
        if (cobwebOverride) Settings.get.swingDelay
        else hardness * 1.5 / strength

      if (breakTime.isInfinity) return 0

      val preEvent = new RobotBreakBlockEvent.Pre(agent, world, pos, breakTime * Settings.get.harvestRatio)
      MinecraftForge.EVENT_BUS.post(preEvent)
      if (preEvent.isCanceled) return 0
      val adjustedBreakTime = Math.max(0.05, preEvent.getBreakTime)

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

      world.playEvent(this, 2001, pos, Block.getIdFromBlock(block) + (metadata << 12))

      if (stack != null) {
        stack.onBlockDestroyed(world, state, pos, this)
      }

      val te = world.getTileEntity(pos)
      val canHarvest = block.canHarvestBlock(world, pos, this)
      block.onBlockHarvested(world, pos, state, this)
      if (block.removedByPlayer(state, world, pos, this, block.canHarvestBlock(world, pos, this))) {
        block.onBlockDestroyedByPlayer(world, pos, state)
        if (canHarvest) {
          block.harvestBlock(world, this, pos, state, te, stack)
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
      !stack.isItemEqual(new ItemStack(Items.LEAD))
  }

  override def dropItem(stack: ItemStack, dropAround: Boolean, traceItem: Boolean): EntityItem =
    InventoryUtils.spawnStackInWorld(BlockPosition(agent), stack, if (dropAround) None else Option(facing))

  private def shouldCancel(f: () => PlayerInteractEvent) = {
    try {
      val event = f()
      event.isCanceled || (event match {
        case rightClick: PlayerInteractEvent.RightClickBlock => rightClick.getUseBlock == Event.Result.DENY || rightClick.getUseItem == Event.Result.DENY
        case leftClick: PlayerInteractEvent.LeftClickBlock => leftClick.getUseBlock == Event.Result.DENY || leftClick.getUseItem == Event.Result.DENY
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
          else ForgeEventFactory.onPlayerDestroyItem(this, newStack, EnumHand.MAIN_HAND)
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
        val didPlace = stack.onItemUse(this, world, pos, EnumHand.MAIN_HAND, side, hitX, hitY, hitZ)
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

  override def canCommandSenderUseCommand(level: Int, command: String): Boolean = {
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

//  override def setCurrentItemOrArmor(slot: Int, stack: ItemStack): Unit = {
//    if (slot == 0 && agent.equipmentInventory.getSizeInventory > 0) {
//      agent.equipmentInventory.setInventorySlotContents(slot, stack)
//    }
//    // else: armor slots, which are unsupported in agents.
//  }

  override def setRevengeTarget(entity: EntityLivingBase) {}

  override def setLastAttacker(entity: Entity) {}

  override def startRiding(entityIn: Entity, force: Boolean): Boolean = false

  override def trySleep(bedLocation: BlockPos) = SleepResult.OTHER_PROBLEM

  override def addChatMessage(message: ITextComponent) {}

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
    val world = player.world
    var ticks = 0
    var lastDamageSent = 0

    def tick(): Unit = {
      // Cancel if the agent stopped or our action is invalidated some other way.
      if (world != player.world || !world.isBlockLoaded(pos) || world.isAirBlock(pos) || !player.agent.machine.isRunning) {
        world.sendBlockBreakProgress(-1, pos, -1)
        return
      }

      val damage = 10 * ticks / Math.max(ticksTotal, 1)
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

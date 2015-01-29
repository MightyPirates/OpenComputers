package li.cil.oc.server.component.robot

import com.mojang.authlib.GameProfile
import li.cil.oc.OpenComputers
import li.cil.oc.Settings
import li.cil.oc.api.event._
import li.cil.oc.common.tileentity
import li.cil.oc.integration.Mods
import li.cil.oc.integration.util.PortalGun
import li.cil.oc.integration.util.TinkersConstruct
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
import scala.reflect._

object Player {
  def profileFor(robot: tileentity.Robot) = {
    val uuid = robot.ownerUuid.getOrElse(robot.determineUUID())
    val randomId = (robot.world.rand.nextInt(0xFFFFFF) + 1).toString
    val name = Settings.get.nameFormat.
      replace("$player$", robot.owner).
      replace("$random$", randomId)
    new GameProfile(uuid, name)
  }
}

class Player(val robot: tileentity.Robot) extends FakePlayer(robot.world.asInstanceOf[WorldServer], Player.profileFor(robot)) {
  playerNetServerHandler = new NetHandlerPlayServer(mcServer, FakeNetworkManager, this)

  capabilities.allowFlying = true
  capabilities.disableDamage = true
  capabilities.isFlying = true
  onGround = true

  override def getYOffset = 0.5f

  override def getEyeHeight = 0f

  setSize(1, 1)

  if (Mods.BattleGear2.isAvailable) {
    ObfuscationReflectionHelper.setPrivateValue(classOf[EntityPlayer], this, robot.inventory, "inventory", "field_71071_by")
  }
  else inventory = robot.inventory

  var facing, side = EnumFacing.SOUTH

  var customItemInUseBecauseMinecraftIsBloodyStupidAndMakesRandomMethodsClientSided: ItemStack = _

  def world = robot.world

  override def getPosition = new BlockPos(posX, posY, posZ)

  override def getDefaultEyeHeight = 0f

  override def getDisplayName = new ChatComponentText(robot.name)

  theItemInWorldManager.setBlockReachDistance(1)

  // ----------------------------------------------------------------------- //

  def updatePositionAndRotation(facing: EnumFacing, side: EnumFacing) {
    this.facing = facing
    this.side = side
    // Slightly offset in robot's facing to avoid glitches (e.g. Portal Gun).
    val direction = new Vec3(
      facing.getFrontOffsetX + side.getFrontOffsetX + robot.facing.getFrontOffsetX * 0.01,
      facing.getFrontOffsetY + side.getFrontOffsetY + robot.facing.getFrontOffsetY * 0.01,
      facing.getFrontOffsetZ + side.getFrontOffsetZ + robot.facing.getFrontOffsetZ * 0.01).normalize()
    val yaw = Math.toDegrees(-Math.atan2(direction.xCoord, direction.zCoord)).toFloat
    val pitch = Math.toDegrees(-Math.atan2(direction.yCoord, Math.sqrt((direction.xCoord * direction.xCoord) + (direction.zCoord * direction.zCoord)))).toFloat * 0.99f
    setLocationAndAngles(robot.x + 0.5, robot.y + 0.5, robot.z + 0.5, yaw, pitch)
    prevRotationPitch = rotationPitch
    prevRotationYaw = rotationYaw
  }

  def closestEntity[Type <: Entity : ClassTag](side: EnumFacing = facing) = {
    val (x, y, z) = (robot.x + side.getFrontOffsetX, robot.y + side.getFrontOffsetY, robot.z + side.getFrontOffsetZ)
    val bounds = AxisAlignedBB.fromBounds(x, y, z, x + 1, y + 1, z + 1)
    Option(world.findNearestEntityWithinAABB(classTag[Type].runtimeClass, bounds, this)).map(_.asInstanceOf[Type])
  }

  def entitiesOnSide[Type <: Entity : ClassTag](side: EnumFacing) = {
    val (x, y, z) = (robot.x + side.getFrontOffsetX, robot.y + side.getFrontOffsetY, robot.z + side.getFrontOffsetZ)
    entitiesInBlock[Type](x, y, z)
  }

  def entitiesInBlock[Type <: Entity : ClassTag](x: Int, y: Int, z: Int) = {
    val bounds = AxisAlignedBB.fromBounds(x, y, z, x + 1, y + 1, z + 1)
    world.getEntitiesWithinAABB(classTag[Type].runtimeClass, bounds).map(_.asInstanceOf[Type])
  }

  private def adjacentItems = {
    val bounds = AxisAlignedBB.fromBounds(robot.x - 2, robot.y - 2, robot.z - 2, robot.x + 3, robot.y + 3, robot.z + 3)
    world.getEntitiesWithinAABB(classOf[EntityItem], bounds).map(_.asInstanceOf[EntityItem])
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
    callUsingItemInSlot(0, stack => entity match {
      case player: EntityPlayer if !canAttackPlayer(player) => // Avoid player damage.
      case _ =>
        val event = new RobotAttackEntityEvent.Pre(robot, entity)
        MinecraftForge.EVENT_BUS.post(event)
        if (!event.isCanceled) {
          super.attackTargetEntityWithCurrentItem(entity)
          MinecraftForge.EVENT_BUS.post(new RobotAttackEntityEvent.Post(robot, entity))
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
    !cancel && callUsingItemInSlot(0, stack => {
      val result = isItemUseAllowed(stack) && (entity.interactFirst(this) || (entity match {
        case living: EntityLivingBase if getCurrentEquippedItem != null => getCurrentEquippedItem.interactWithEntity(this, living)
        case _ => false
      }))
      if (getCurrentEquippedItem != null && getCurrentEquippedItem.stackSize <= 0) {
        destroyCurrentEquippedItem()
      }
      result
    })
  }

  def activateBlockOrUseItem(pos: BlockPos, side: EnumFacing, hitX: Float, hitY: Float, hitZ: Float, duration: Double): ActivationType.Value = {
    callUsingItemInSlot(0, stack => {
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
    callUsingItemInSlot(0, stack => {
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
      robot.machine.pause(heldTicks / 20.0)
      // These are functions to avoid null pointers if newStack is null.
      def sizeOrDamageChanged = newStack.stackSize != oldSize || newStack.getItemDamage != oldDamage
      def tagChanged = (oldData == null && newStack.hasTagCompound) || (oldData != null && !newStack.hasTagCompound) ||
        (oldData != null && newStack.hasTagCompound && !oldData.equals(newStack.getTagCompound))
      val stackChanged = newStack != stack || (newStack != null && (sizeOrDamageChanged || tagChanged || PortalGun.isStandardPortalGun(stack)))
      if (stackChanged) {
        robot.setInventorySlotContents(0, newStack)
      }
      stackChanged
    }
  }

  def placeBlock(slot: Int, pos: BlockPos, side: EnumFacing, hitX: Float, hitY: Float, hitZ: Float): Boolean = {
    callUsingItemInSlot(slot, stack => {
      if (shouldCancel(() => ForgeEventFactory.onPlayerInteract(this, Action.RIGHT_CLICK_BLOCK, world, pos, side))) {
        return false
      }

      tryPlaceBlockWhileHandlingFunnySpecialCases(stack, pos, side, hitX, hitY, hitZ)
    }, repair = false)
  }

  def clickBlock(pos: BlockPos, side: EnumFacing): Double = {
    callUsingItemInSlot(0, stack => {
      if (shouldCancel(() => ForgeEventFactory.onPlayerInteract(this, Action.LEFT_CLICK_BLOCK, world, pos, side))) {
        return 0
      }

      // TODO Is this already handled via the event?
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

      val isBlockUnbreakable = block.getBlockHardness(world, pos) < 0
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

      val hardness = block.getBlockHardness(world, pos)
      val strength = getBreakSpeed(state, pos)
      val breakTime =
        if (cobwebOverride) Settings.get.swingDelay
        else hardness * 1.5 / strength

      if (breakTime.isInfinity) return 0

      val preEvent = new RobotBreakBlockEvent.Pre(robot, world, pos, breakTime * Settings.get.harvestRatio)
      MinecraftForge.EVENT_BUS.post(preEvent)
      if (preEvent.isCanceled) return 0
      val adjustedBreakTime = math.max(0.05, preEvent.getBreakTime)

      // Special handling for Tinkers Construct - tools like the hammers do
      // their break logic in onBlockStartBreak but return true to cancel
      // further processing. We also need to adjust our offset for their ray-
      // tracing implementation.
      if (TinkersConstruct.isInfiTool(stack)) {
        posY -= 1.62
        prevPosY = posY
      }
      val cancel = stack != null && stack.getItem.onBlockStartBreak(stack, pos, this)
      if (cancel && TinkersConstruct.isInfiTool(stack)) {
        posY += 1.62
        prevPosY = posY
        return adjustedBreakTime
      }
      if (cancel) {
        return 0
      }

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
          MinecraftForge.EVENT_BUS.post(new RobotBreakBlockEvent.Post(robot, breakEvent.getExpToDrop))
        }
        else if (stack != null) {
          MinecraftForge.EVENT_BUS.post(new RobotBreakBlockEvent.Post(robot, 0))
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
    robot.spawnStackInWorld(stack, if (inPlace) None else Option(facing))

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

  private def callUsingItemInSlot[T](slot: Int, f: (ItemStack) => T, repair: Boolean = true) = {
    val itemsBefore = adjacentItems
    val stack = inventory.getStackInSlot(slot)
    val oldStack = if (stack != null) stack.copy() else null
    try {
      f(stack)
    }
    finally {
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
      val damageRate = new RobotUsedToolEvent.ComputeDamageRate(robot, oldStack, stack, Settings.get.itemDamageRate)
      MinecraftForge.EVENT_BUS.post(damageRate)
      if (damageRate.getDamageRate < 1) {
        MinecraftForge.EVENT_BUS.post(new RobotUsedToolEvent.ApplyDamageRate(robot, oldStack, stack, damageRate.getDamageRate))
      }
    }
  }

  private def tryPlaceBlockWhileHandlingFunnySpecialCases(stack: ItemStack, pos: BlockPos, side: EnumFacing, hitX: Float, hitY: Float, hitZ: Float) = {
    stack != null && stack.stackSize > 0 && {
      val event = new RobotPlaceBlockEvent.Pre(robot, stack, world, pos)
      MinecraftForge.EVENT_BUS.post(event)
      if (event.isCanceled) false
      else {
        val fakeEyeHeight = if (rotationPitch < 0 && isSomeKindOfPiston(stack)) 1.82 else 0
        setPosition(posX, posY - fakeEyeHeight, posZ)
        val didPlace = stack.onItemUse(this, world, pos, side, hitX, hitY, hitZ)
        setPosition(posX, posY + fakeEyeHeight, posZ)
        if (didPlace) {
          MinecraftForge.EVENT_BUS.post(new RobotPlaceBlockEvent.Post(robot, stack, world, pos))
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
      robot.bot.node.changeBuffer(-Settings.get.robotExhaustionCost * amount)
    }
    MinecraftForge.EVENT_BUS.post(new RobotExhaustionEvent(robot, amount))
  }

  override def closeScreen() {}

  override def swingItem() {}

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

  override def setCurrentItemOrArmor(slot: Int, stack: ItemStack) {}

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
}

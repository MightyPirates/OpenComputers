package li.cil.oc.util

import li.cil.oc.Config
import li.cil.oc.common.tileentity.Robot
import net.minecraft.block.{BlockFluid, Block}
import net.minecraft.entity.item.EntityItem
import net.minecraft.entity.player.{EnumStatus, EntityPlayer}
import net.minecraft.entity.{EntityLivingBase, Entity}
import net.minecraft.item.ItemStack
import net.minecraft.potion.PotionEffect
import net.minecraft.server.MinecraftServer
import net.minecraft.util.{AxisAlignedBB, DamageSource, ChunkCoordinates}
import net.minecraft.world.World
import net.minecraftforge.common.{ForgeDirection, FakePlayer}
import net.minecraftforge.event.entity.player.PlayerInteractEvent.Action
import net.minecraftforge.event.{Event, ForgeEventFactory}
import net.minecraftforge.fluids.FluidRegistry
import scala.Some
import scala.collection.convert.WrapAsScala._
import scala.reflect._

class RobotPlayer(val robot: Robot) extends FakePlayer(robot.world, "OpenComputers") {
  capabilities.allowFlying = true
  capabilities.disableDamage = true
  capabilities.isFlying = true
  yOffset = 0.5f
  eyeHeight = 0f
  setSize(1, 1)

  val robotInventory = new InventoryRobot(this)
  inventory = robotInventory

  def world = robot.worldObj

  override def getPlayerCoordinates = new ChunkCoordinates(robot.x, robot.y, robot.z)

  // ----------------------------------------------------------------------- //

  def updatePositionAndRotation(pitch: ForgeDirection) {
    val offsetToGetPistonsToBePlacedProperly = pitch.offsetY * 0.83
    setLocationAndAngles(
      robot.x + 0.5,
      robot.y - offsetToGetPistonsToBePlacedProperly,
      robot.z + 0.5,
      robot.yaw match {
        case ForgeDirection.WEST => 90
        case ForgeDirection.NORTH => 180
        case ForgeDirection.EAST => 270
        case _ => 0
      }, pitch.offsetY * -90)
  }

  def closestLivingEntity(side: ForgeDirection) = {
    entitiesOnSide[EntityLivingBase](side).
      foldLeft((Double.PositiveInfinity, None: Option[EntityLivingBase])) {
      case ((bestDistance, bestEntity), entity: EntityLivingBase) =>
        val distance = entity.getDistanceSqToEntity(this)
        if (distance < bestDistance) (distance, Some(entity))
        else (bestDistance, bestEntity)
      case (best, _) => best
    } match {
      case (_, Some(entity)) => Some(entity)
      case _ => None
    }
  }

  def entitiesOnSide[Type <: Entity : ClassTag](side: ForgeDirection) = {
    val (bx, by, bz) = (robot.x + side.offsetX, robot.y + side.offsetY, robot.z + side.offsetZ)
    entitiesInBlock[Type](bx, by, bz)
  }

  def entitiesInBlock[Type <: Entity : ClassTag](x: Int, y: Int, z: Int) = {
    val bounds = AxisAlignedBB.getAABBPool.getAABB(x, y, z, x + 1, y + 1, z + 1)
    world.getEntitiesWithinAABB(classTag[Type].runtimeClass, bounds).map(_.asInstanceOf[Type])
  }

  // ----------------------------------------------------------------------- //

  override def attackTargetEntityWithCurrentItem(entity: Entity) {
    val stack = getCurrentEquippedItem
    val oldDamage = getCurrentEquippedItem.getItemDamage
    super.attackTargetEntityWithCurrentItem(entity)
    if (stack.stackSize > 0 && stack.isItemStackDamageable && getRNG.nextDouble() >= Config.itemDamageRate) {
      val addedDamage = ((stack.getItemDamage - oldDamage) * Config.itemDamageRate).toInt
      stack.setItemDamage(oldDamage + addedDamage)
    }
  }

  def activateBlockOrUseItem(x: Int, y: Int, z: Int, side: Int, hitX: Float, hitY: Float, hitZ: Float): ActivationType.Value = {
    val event = ForgeEventFactory.onPlayerInteract(this, Action.RIGHT_CLICK_BLOCK, x, y, z, side)
    if (event.isCanceled || event.useBlock == Event.Result.DENY) {
      return ActivationType.None
    }

    val stack = inventory.getCurrentItem
    val item = if (stack != null) stack.getItem else null
    if (Config.callOnItemUseFirst) {
      if (item != null && item.onItemUseFirst(stack, this, world, x, y, z, side, hitX, hitY, hitZ)) {
        if (stack.stackSize <= 0) ForgeEventFactory.onPlayerDestroyItem(this, stack)
        if (stack.stackSize <= 0) inventory.setInventorySlotContents(0, null)
        return ActivationType.ItemUsed
      }
    }

    val blockId = world.getBlockId(x, y, z)
    val block = Block.blocksList(blockId)
    val canActivate = block != null && Config.allowActivateBlocks
    val shouldActivate = canActivate && (!isSneaking || (item == null || item.shouldPassSneakingClickToBlock(world, x, y, z)))
    if (shouldActivate && block.onBlockActivated(world, x, y, z, this, side, hitX, hitY, hitZ)) {
      return ActivationType.BlockActivated
    }

    if (stack != null) {
      val didPlace = stack.tryPlaceItemIntoWorld(this, world, x, y, z, side, hitX, hitY, hitZ)
      if (stack.stackSize <= 0) ForgeEventFactory.onPlayerDestroyItem(this, stack)
      if (stack.stackSize <= 0) inventory.setInventorySlotContents(0, null)
      if (didPlace) {
        return ActivationType.ItemPlaced
      }

      if (tryUseItem(stack)) {
        return ActivationType.ItemUsed
      }
    }

    ActivationType.None
  }

  def useEquippedItem() = {
    val event = ForgeEventFactory.onPlayerInteract(this, Action.RIGHT_CLICK_AIR, 0, 0, 0, -1)
    if (!event.isCanceled && event.useItem != Event.Result.DENY) {
      tryUseItem(getCurrentEquippedItem)
    }
    else false
  }

  private def tryUseItem(stack: ItemStack) =
    stack != null && stack.stackSize > 0 && stack.getMaxItemUseDuration <= 0 && {
      val oldSize = stack.stackSize
      val oldDamage = stack.getItemDamage
      val newStack = stack.useItemRightClick(world, this)
      val stackChanged = newStack != stack || (newStack != null && (newStack.stackSize != oldSize || newStack.getItemDamage != oldDamage))
      stackChanged && {
        if (newStack.stackSize <= 0) ForgeEventFactory.onPlayerDestroyItem(this, newStack)
        if (newStack.stackSize > 0) inventory.setInventorySlotContents(0, newStack)
        else inventory.setInventorySlotContents(0, null)
        true
      }
    }

  def placeBlock(stack: ItemStack, x: Int, y: Int, z: Int, side: Int, hitX: Float, hitY: Float, hitZ: Float): Boolean = {
    val event = ForgeEventFactory.onPlayerInteract(this, Action.RIGHT_CLICK_BLOCK, x, y, z, side)
    if (event.isCanceled) {
      return false
    }

    event.useBlock == Event.Result.DENY || {
      val direction = ForgeDirection.getOrientation(side).getOpposite
      val (onX, onY, onZ) = (x + direction.offsetX, y + direction.offsetY, z + direction.offsetZ)
      val result = stack.tryPlaceItemIntoWorld(this, world, onX, onY, onZ, side, hitX, hitY, hitZ)
      if (stack.stackSize <= 0) ForgeEventFactory.onPlayerDestroyItem(this, stack)
      result
    }
  }

  def clickBlock(x: Int, y: Int, z: Int, side: Int): Boolean = {
    val event = ForgeEventFactory.onPlayerInteract(this, Action.LEFT_CLICK_BLOCK, x, y, z, side)
    if (event.isCanceled) {
      return false
    }

    // TODO Is this already handled via the event?
    if (MinecraftServer.getServer.isBlockProtected(world, x, y, z, this)) {
      return false
    }

    val blockId = world.getBlockId(x, y, z)
    val block = Block.blocksList(blockId)
    val mayBreakBlock = event.useBlock != Event.Result.DENY && blockId > 0 && block != null
    val canBreakBlock = mayBreakBlock &&
      !block.isAirBlock(world, x, y, z) &&
      FluidRegistry.lookupFluidForBlock(block) == null &&
      !block.isInstanceOf[BlockFluid]
    if (canBreakBlock) {
      block.onBlockClicked(world, x, y, z, this)
      world.extinguishFire(this, x, y, z, side)
      if (block.canEntityDestroy(world, x, y, z, this)) {
        if (world.worldInfo.getGameType.isAdventure && !isCurrentToolAdventureModeExempt(x, y, z)) {
          return false
        }

        val stack = getCurrentEquippedItem
        if (stack != null && stack.getItem.onBlockStartBreak(stack, x, y, z, this)) {
          return false
        }

        val metadata = world.getBlockMetadata(x, y, z)
        world.playAuxSFXAtEntity(this, 2001, x, y, z, blockId + (metadata << 12))

        if (stack != null) {
          val oldDamage = stack.getItemDamage
          stack.onBlockDestroyed(world, blockId, x, y, z, this)
          if (stack.stackSize == 0) {
            destroyCurrentEquippedItem()
          }
          else if (stack.isItemStackDamageable && getRNG.nextDouble() >= Config.itemDamageRate) {
            val addedDamage = ((stack.getItemDamage - oldDamage) * Config.itemDamageRate).toInt
            stack.setItemDamage(oldDamage + addedDamage)
          }
        }

        val itemsBefore = entitiesInBlock[EntityItem](x, y, z)
        block.onBlockHarvested(world, x, y, z, metadata, this)
        if (block.removeBlockByPlayer(world, this, x, y, z)) {
          block.onBlockDestroyedByPlayer(world, x, y, z, metadata)
          if (block.canHarvestBlock(this, metadata)) {
            block.harvestBlock(world, this, x, y, z, metadata)
            val itemsAfter = entitiesInBlock[EntityItem](x, y, z)
            val itemsDropped = itemsAfter -- itemsBefore
            for (drop <- itemsDropped) {
              drop.delayBeforeCanPickup = 0
              drop.onCollideWithPlayer(this)
            }
          }
          return true
        }
      }
    }
    false
  }

  // ----------------------------------------------------------------------- //

  override def swingItem() {
    // TODO animation
  }

  override def canAttackPlayer(player: EntityPlayer) =
    Config.canAttackPlayers && super.canAttackPlayer(player)

  override def canEat(value: Boolean) = false

  override def isPotionApplicable(effect: PotionEffect) = false

  override def attackEntityAsMob(entity: Entity) = false

  override def attackEntityFrom(source: DamageSource, damage: Float) = false

  override def setItemInUse(stack: ItemStack, maxItemUseDuration: Int) {}

  override def openGui(mod: AnyRef, modGuiId: Int, world: World, x: Int, y: Int, z: Int) {}

  override def closeScreen() {}

  override def heal(amount: Float) {}

  override def setHealth(value: Float) {}

  override def setDead() = isDead = true

  override def onDeath(source: DamageSource) {}

  override def setCurrentItemOrArmor(slot: Int, stack: ItemStack) {}

  override def setRevengeTarget(entity: EntityLivingBase) {}

  override def setLastAttacker(entity: Entity) {}

  override def mountEntity(entity: Entity) {}

  override def travelToDimension(dimension: Int) {}

  override def sleepInBedAt(x: Int, y: Int, z: Int) = EnumStatus.OTHER_PROBLEM

  override def interactWith(entity: Entity) = false // TODO Or do we want this?
}

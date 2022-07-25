package li.cil.oc.common.item

import java.util

import com.mojang.blaze3d.matrix.MatrixStack
import li.cil.oc.CreativeTab
import li.cil.oc.OpenComputers
import li.cil.oc.api.driver
import li.cil.oc.api.driver.item.Chargeable
import li.cil.oc.api.event.RobotRenderEvent.MountPoint
import li.cil.oc.api.internal.Robot
import li.cil.oc.client.renderer.item.UpgradeRenderer
import li.cil.oc.common.item.traits.Delegate
import li.cil.oc.integration.opencomputers.{Item => OpenComputersItem}
import li.cil.oc.util.BlockPosition
import net.minecraft.client.util.ITooltipFlag
import net.minecraft.item.ItemGroup
import net.minecraft.entity.Entity
import net.minecraft.entity.LivingEntity
import net.minecraft.entity.player.PlayerEntity
import net.minecraft.item.BlockItemUseContext
import net.minecraft.item.Item
import net.minecraft.item.Item.Properties
import net.minecraft.item.ItemStack
import net.minecraft.item.ItemUseContext
import net.minecraft.item.Rarity
import net.minecraft.item.UseAction
import net.minecraft.util.ActionResult
import net.minecraft.util.ActionResultType
import net.minecraft.util.Direction
import net.minecraft.util.Hand
import net.minecraft.util.NonNullList
import net.minecraft.util.math.BlockPos
import net.minecraft.util.text.ITextComponent
import net.minecraft.util.text.StringTextComponent
import net.minecraft.world.IWorldReader
import net.minecraft.world.World
import net.minecraftforge.api.distmarker.Dist
import net.minecraftforge.api.distmarker.OnlyIn

import scala.collection.mutable
import scala.collection.mutable.ArrayBuffer

object Delegator {
  def subItem(stack: ItemStack): Option[Delegate] =
    if (!stack.isEmpty) stack.getItem match {
      case delegator: Delegator => delegator.subItem(stack.getDamageValue)
      case _ => None
    }
    else None
}

class Delegator extends Item(new Properties().tab(CreativeTab)) with driver.item.UpgradeRenderer with Chargeable {

  // ----------------------------------------------------------------------- //
  // SubItem
  // ----------------------------------------------------------------------- //

  @Deprecated
  override def getItemStackLimit(stack: ItemStack): Int =
    Delegator.subItem(stack) match {
      case Some(subItem) => OpenComputersItem.address(stack) match {
        case Some(address) => 1
        case _ => subItem.maxStackSize
      }
      case _ => super.getItemStackLimit(stack)
    }

  val subItems: ArrayBuffer[Delegate] = mutable.ArrayBuffer.empty[traits.Delegate]

  def add(subItem: traits.Delegate): Int = {
    val itemId = subItems.length
    subItems += subItem
    itemId
  }

  def subItem(damage: Int): Option[Delegate] =
    damage match {
      case itemId if itemId >= 0 && itemId < subItems.length => Some(subItems(itemId))
      case _ => None
    }

  override def fillItemCategory(tab: ItemGroup, list: NonNullList[ItemStack]) {
    // Workaround for MC's untyped lists...
    if(allowdedIn(tab)){
      subItems.indices.filter(subItems(_).showInItemList).
        map(subItems(_).createItemStack()).
        sortBy(_.getDescriptionId).
        foreach(list.add)
    }
  }

  // ----------------------------------------------------------------------- //
  // Item
  // ----------------------------------------------------------------------- //

  @Deprecated
  private var unlocalizedName = super.getDescriptionId()

  @Deprecated
  private[oc] def setUnlocalizedName(name: String): Unit = unlocalizedName = name

  @Deprecated
  override def getDescriptionId(stack: ItemStack): String =
    Delegator.subItem(stack) match {
      case Some(subItem) => "item.oc." + subItem.unlocalizedName
      case _ => unlocalizedName
    }

  override def getRarity(stack: ItemStack): Rarity =
    Delegator.subItem(stack) match {
      case Some(subItem) => subItem.rarity(stack)
      case _ => Rarity.COMMON
    }

//  override def getColorFromItemStack(stack: ItemStack, pass: Int) =
//    Delegator.subItem(stack) match {
//      case Some(subItem) => subItem.color(stack, pass)
//      case _ => super.getColorFromItemStack(stack, pass)
//    }

  // ----------------------------------------------------------------------- //

  override def doesSneakBypassUse(stack: ItemStack, world: IWorldReader, pos: BlockPos, player: PlayerEntity): Boolean =
    Delegator.subItem(stack) match {
      case Some(subItem) => subItem.doesSneakBypassUse(world, pos, player)
      case _ => super.doesSneakBypassUse(stack, world, pos, player)
    }

  @Deprecated
  override def onItemUseFirst(stack: ItemStack, ctx: ItemUseContext): ActionResultType =
    if (stack != null) {
      Delegator.subItem(stack) match {
        case Some(subItem) => {
          val pos = BlockPosition(ctx.getClickedPos, ctx.getLevel)
          val hitPos = ctx.getClickLocation
          subItem.onItemUseFirst(stack, ctx.getPlayer, pos, ctx.getClickedFace,
            (hitPos.x - pos.x).toFloat, (hitPos.y - pos.y).toFloat, (hitPos.z - pos.z).toFloat)
        }
        case _ => super.onItemUseFirst(stack, ctx)
      }
    }
    else super.onItemUseFirst(stack, ctx)

  @Deprecated
  def onItemUseFirst(player: PlayerEntity, world: World, pos: BlockPos, side: Direction, hitX: Float, hitY: Float, hitZ: Float, hand: Hand) = ActionResultType.PASS

  override def useOn(ctx: ItemUseContext): ActionResultType =
    ctx.getItemInHand match {
      case stack: ItemStack => Delegator.subItem(stack) match {
        case Some(subItem) => {
          val world = ctx.getLevel
          val pos = BlockPosition(ctx.getClickedPos, world)
          val hitPos = ctx.getClickLocation
          val success = subItem.onItemUse(ctx.getItemInHand, ctx.getPlayer, pos, ctx.getClickedFace,
            (hitPos.x - pos.x).toFloat, (hitPos.y - pos.y).toFloat, (hitPos.z - pos.z).toFloat)
          if (success) ActionResultType.sidedSuccess(world.isClientSide) else ActionResultType.PASS
        }
        case _ => super.useOn(ctx)
      }
      case _ => super.useOn(ctx)
    }

  override def use(world: World, player: PlayerEntity, hand: Hand): ActionResult[ItemStack] =
    player.getItemInHand(hand) match {
      case stack: ItemStack => Delegator.subItem(stack) match {
        case Some(subItem) => subItem.use(stack, world, player)
        case _ => super.use(world, player, hand)
      }
      case _ => super.use(world, player, hand)
    }

  // ----------------------------------------------------------------------- //

  override def finishUsingItem(stack: ItemStack, world: World, entity: LivingEntity): ItemStack =
    Delegator.subItem(stack) match {
      case Some(subItem) => subItem.finishUsingItem(stack, world, entity)
      case _ => super.finishUsingItem(stack, world, entity)
    }

  override def getUseAnimation(stack: ItemStack): UseAction =
    Delegator.subItem(stack) match {
      case Some(subItem) => subItem.getUseAnimation(stack)
      case _ => super.getUseAnimation(stack)
    }

  override def getUseDuration(stack: ItemStack): Int =
    Delegator.subItem(stack) match {
      case Some(subItem) => subItem.getMaxItemUseDuration(stack)
      case _ => super.getUseDuration(stack)
    }

  override def releaseUsing(stack: ItemStack, world: World, entity: LivingEntity, timeLeft: Int): Unit =
    Delegator.subItem(stack) match {
      case Some(subItem) => subItem.onPlayerStoppedUsing(stack, entity, timeLeft)
      case _ => super.releaseUsing(stack, world, entity, timeLeft)
    }

  @Deprecated
  def internalGetItemStackDisplayName(stack: ItemStack): String = super.getName(stack).getString

  @Deprecated
  override def getName(stack: ItemStack): ITextComponent =
    Delegator.subItem(stack) match {
      case Some(subItem) => subItem.displayName(stack) match {
        case Some(name) => new StringTextComponent(name)
        case _ => super.getName(stack)
      }
      case _ => super.getName(stack)
    }

  @OnlyIn(Dist.CLIENT)
  override def appendHoverText(stack: ItemStack, world: World, tooltip: util.List[ITextComponent], flag: ITooltipFlag) {
    super.appendHoverText(stack, world, tooltip, flag)
    Delegator.subItem(stack) match {
      case Some(subItem) => try subItem.tooltipLines(stack, world, tooltip, flag) catch {
        case t: Throwable => OpenComputers.log.warn("Error in item tooltip.", t)
      }
      case _ => // Nothing to add.
    }
  }

  override def getDurabilityForDisplay(stack: ItemStack): Double =
    Delegator.subItem(stack) match {
      case Some(subItem) => subItem.durability(stack)
      case _ => super.getDurabilityForDisplay(stack)
    }

  override def showDurabilityBar(stack: ItemStack): Boolean =
    Delegator.subItem(stack) match {
      case Some(subItem) => subItem.showDurabilityBar(stack)
      case _ => super.showDurabilityBar(stack)
    }

  override def inventoryTick(stack: ItemStack, world: World, player: Entity, slot: Int, selected: Boolean): Unit =
    Delegator.subItem(stack) match {
      case Some(subItem) => subItem.update(stack, world, player, slot, selected)
      case _ => super.inventoryTick(stack, world, player, slot, selected)
    }

  override def toString: String = getDescriptionId

  // ----------------------------------------------------------------------- //

  def canCharge(stack: ItemStack): Boolean =
    Delegator.subItem(stack) match {
      case Some(subItem: Chargeable) => true
      case _ => false
    }

  def charge(stack: ItemStack, amount: Double, simulate: Boolean): Double =
    Delegator.subItem(stack) match {
      case Some(subItem: Chargeable) => subItem.charge(stack, amount, simulate)
      case _ => amount
    }

  // ----------------------------------------------------------------------- //

  override def computePreferredMountPoint(stack: ItemStack, robot: Robot, availableMountPoints: util.Set[String]): String = UpgradeRenderer.preferredMountPoint(stack, availableMountPoints)

  override def render(matrix: MatrixStack, stack: ItemStack, mountPoint: MountPoint, robot: Robot, pt: Float): Unit = UpgradeRenderer.render(matrix, stack, mountPoint)
}

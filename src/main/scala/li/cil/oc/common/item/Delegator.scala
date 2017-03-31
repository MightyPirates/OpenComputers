package li.cil.oc.common.item

import java.util

import li.cil.oc.CreativeTab
import li.cil.oc.OpenComputers
import li.cil.oc.api.driver
import li.cil.oc.api.driver.item.Chargeable
import li.cil.oc.api.event.RobotRenderEvent.MountPoint
import li.cil.oc.api.internal.Robot
import li.cil.oc.client.renderer.item.UpgradeRenderer
import li.cil.oc.util.BlockPosition
import net.minecraft.creativetab.CreativeTabs
import net.minecraft.entity.Entity
import net.minecraft.entity.EntityLivingBase
import net.minecraft.entity.player.EntityPlayer
import net.minecraft.item.EnumAction
import net.minecraft.item.EnumRarity
import net.minecraft.item.Item
import net.minecraft.item.ItemStack
import net.minecraft.util.ActionResult
import net.minecraft.util.EnumActionResult
import net.minecraft.util.EnumFacing
import net.minecraft.util.EnumHand
import net.minecraft.util.NonNullList
import net.minecraft.util.math.BlockPos
import net.minecraft.world.IBlockAccess
import net.minecraft.world.World
import net.minecraftforge.fml.relauncher.Side
import net.minecraftforge.fml.relauncher.SideOnly

import scala.collection.mutable

object Delegator {
  def subItem(stack: ItemStack) =
    if (!stack.isEmpty) stack.getItem match {
      case delegator: Delegator => delegator.subItem(stack.getItemDamage)
      case _ => None
    }
    else None
}

class Delegator extends Item with driver.item.UpgradeRenderer with Chargeable {
  setHasSubtypes(true)
  setCreativeTab(CreativeTab)

  // ----------------------------------------------------------------------- //
  // SubItem
  // ----------------------------------------------------------------------- //

  override def getItemStackLimit(stack: ItemStack) =
    Delegator.subItem(stack) match {
      case Some(subItem) => subItem.maxStackSize
      case _ => maxStackSize
    }

  val subItems = mutable.ArrayBuffer.empty[traits.Delegate]

  def add(subItem: traits.Delegate) = {
    val itemId = subItems.length
    subItems += subItem
    itemId
  }

  def subItem(damage: Int) =
    damage match {
      case itemId if itemId >= 0 && itemId < subItems.length => Some(subItems(itemId))
      case _ => None
    }

  override def getSubItems(item: Item, tab: CreativeTabs, list: NonNullList[ItemStack]) {
    // Workaround for MC's untyped lists...
    subItems.indices.filter(subItems(_).showInItemList).
      map(subItems(_).createItemStack()).
      sortBy(_.getUnlocalizedName).
      foreach(list.add)
  }

  // ----------------------------------------------------------------------- //
  // Item
  // ----------------------------------------------------------------------- //

  override def getUnlocalizedName(stack: ItemStack): String =
    Delegator.subItem(stack) match {
      case Some(subItem) => "item.oc." + subItem.unlocalizedName
      case _ => getUnlocalizedName
    }

  override def isBookEnchantable(itemA: ItemStack, itemB: ItemStack): Boolean = false

  override def getRarity(stack: ItemStack) =
    Delegator.subItem(stack) match {
      case Some(subItem) => subItem.rarity(stack)
      case _ => EnumRarity.COMMON
    }

//  override def getColorFromItemStack(stack: ItemStack, pass: Int) =
//    Delegator.subItem(stack) match {
//      case Some(subItem) => subItem.color(stack, pass)
//      case _ => super.getColorFromItemStack(stack, pass)
//    }

  override def getContainerItem(stack: ItemStack): ItemStack =
    Delegator.subItem(stack) match {
      case Some(subItem) => subItem.getContainerItem(stack)
      case _ => super.getContainerItem(stack)
    }

  override def hasContainerItem(stack: ItemStack): Boolean =
    Delegator.subItem(stack) match {
      case Some(subItem) => subItem.hasContainerItem(stack)
      case _ => super.hasContainerItem(stack)
    }

  // ----------------------------------------------------------------------- //

  override def doesSneakBypassUse(stack: ItemStack, world: IBlockAccess, pos: BlockPos, player: EntityPlayer): Boolean =
    Delegator.subItem(stack) match {
      case Some(subItem) => subItem.doesSneakBypassUse(world, pos, player)
      case _ => super.doesSneakBypassUse(stack, world, pos, player)
    }

  override def onItemUseFirst(player: EntityPlayer, world: World, pos: BlockPos, side: EnumFacing, hitX: Float, hitY: Float, hitZ: Float, hand: EnumHand): EnumActionResult =
    player.getHeldItem(hand) match {
      case stack:ItemStack => Delegator.subItem(stack) match {
        case Some(subItem) => subItem.onItemUseFirst(stack, player, BlockPosition(pos, world), side, hitX, hitY, hitZ)
        case _ => super.onItemUseFirst(player, world, pos, side, hitX, hitY, hitZ, hand)
      }
      case _ => super.onItemUseFirst(player, world, pos, side, hitX, hitY, hitZ, hand)
  }

  override def onItemUse(player: EntityPlayer, world: World, pos: BlockPos, hand: EnumHand, side: EnumFacing, hitX: Float, hitY: Float, hitZ: Float): EnumActionResult =
    player.getHeldItem(hand) match {
      case stack: ItemStack => Delegator.subItem(stack) match {
        case Some(subItem) => if (subItem.onItemUse(stack, player, BlockPosition(pos, world), side, hitX, hitY, hitZ)) EnumActionResult.SUCCESS else EnumActionResult.PASS
        case _ => super.onItemUse(player, world, pos, hand, side, hitX, hitY, hitZ)
      }
      case _ => super.onItemUse(player, world, pos, hand, side, hitX, hitY, hitZ)
    }

  override def onItemRightClick(world: World, player: EntityPlayer, hand: EnumHand): ActionResult[ItemStack] =
    player.getHeldItem(hand) match {
      case stack: ItemStack => Delegator.subItem(stack) match {
        case Some(subItem) => subItem.onItemRightClick(stack, world, player)
        case _ => super.onItemRightClick(world, player, hand)
      }
      case _ => super.onItemRightClick(world, player, hand)
    }

  // ----------------------------------------------------------------------- //

  override def onItemUseFinish(stack: ItemStack, world: World, entity: EntityLivingBase): ItemStack =
    Delegator.subItem(stack) match {
      case Some(subItem) => subItem.onItemUseFinish(stack, world, entity)
      case _ => super.onItemUseFinish(stack, world, entity)
    }

  override def getItemUseAction(stack: ItemStack): EnumAction =
    Delegator.subItem(stack) match {
      case Some(subItem) => subItem.getItemUseAction(stack)
      case _ => super.getItemUseAction(stack)
    }

  override def getMaxItemUseDuration(stack: ItemStack): Int =
    Delegator.subItem(stack) match {
      case Some(subItem) => subItem.getMaxItemUseDuration(stack)
      case _ => super.getMaxItemUseDuration(stack)
    }

  override def onPlayerStoppedUsing(stack: ItemStack, world: World, entity: EntityLivingBase, timeLeft: Int): Unit =
    Delegator.subItem(stack) match {
      case Some(subItem) => subItem.onPlayerStoppedUsing(stack, entity, timeLeft)
      case _ => super.onPlayerStoppedUsing(stack, world, entity, timeLeft)
    }

  def internalGetItemStackDisplayName(stack: ItemStack) = super.getItemStackDisplayName(stack)

  override def getItemStackDisplayName(stack: ItemStack) =
    Delegator.subItem(stack) match {
      case Some(subItem) => subItem.displayName(stack) match {
        case Some(name) => name
        case _ => super.getItemStackDisplayName(stack)
      }
      case _ => super.getItemStackDisplayName(stack)
    }

  @SideOnly(Side.CLIENT)
  override def addInformation(stack: ItemStack, player: EntityPlayer, tooltip: util.List[String], advanced: Boolean) {
    super.addInformation(stack, player, tooltip, advanced)
    Delegator.subItem(stack) match {
      case Some(subItem) => try subItem.tooltipLines(stack, player, tooltip, advanced) catch {
        case t: Throwable => OpenComputers.log.warn("Error in item tooltip.", t)
      }
      case _ => // Nothing to add.
    }
  }

  override def getDurabilityForDisplay(stack: ItemStack) =
    Delegator.subItem(stack) match {
      case Some(subItem) => subItem.durability(stack)
      case _ => super.getDurabilityForDisplay(stack)
    }

  override def showDurabilityBar(stack: ItemStack) =
    Delegator.subItem(stack) match {
      case Some(subItem) => subItem.showDurabilityBar(stack)
      case _ => super.showDurabilityBar(stack)
    }

  override def onUpdate(stack: ItemStack, world: World, player: Entity, slot: Int, selected: Boolean) =
    Delegator.subItem(stack) match {
      case Some(subItem) => subItem.update(stack, world, player, slot, selected)
      case _ => super.onUpdate(stack, world, player, slot, selected)
    }

  override def toString = getUnlocalizedName

  // ----------------------------------------------------------------------- //

  def canCharge(stack: ItemStack): Boolean =
    Delegator.subItem(stack) match {
      case Some(subItem: Chargeable) => true
      case _ => false
    }

  def charge(stack: ItemStack, amount: Double, simulate: Boolean): Double =
    Delegator.subItem(stack) match {
      case Some(subItem: Chargeable) => subItem.charge(stack, amount, simulate)
      case _ => 0.0
    }

  // ----------------------------------------------------------------------- //

  override def computePreferredMountPoint(stack: ItemStack, robot: Robot, availableMountPoints: util.Set[String]): String = UpgradeRenderer.preferredMountPoint(stack, availableMountPoints)

  override def render(stack: ItemStack, mountPoint: MountPoint, robot: Robot, pt: Float): Unit = UpgradeRenderer.render(stack, mountPoint)
}

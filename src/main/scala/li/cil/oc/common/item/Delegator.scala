package li.cil.oc.common.item

import java.util
import java.util.Random

import cpw.mods.fml.relauncher.Side
import cpw.mods.fml.relauncher.SideOnly
import li.cil.oc.CreativeTab
import li.cil.oc.OpenComputers
import li.cil.oc.Settings
import li.cil.oc.api.driver
import li.cil.oc.api.driver.item.Chargeable
import li.cil.oc.api.event.RobotRenderEvent.MountPoint
import li.cil.oc.api.internal.Robot
import li.cil.oc.client.renderer.item.UpgradeRenderer
import li.cil.oc.util.BlockPosition
import net.minecraft.client.renderer.texture.IIconRegister
import net.minecraft.creativetab.CreativeTabs
import net.minecraft.entity.Entity
import net.minecraft.entity.player.EntityPlayer
import net.minecraft.item.EnumAction
import net.minecraft.item.EnumRarity
import net.minecraft.item.Item
import net.minecraft.item.ItemStack
import net.minecraft.util.IIcon
import net.minecraft.util.WeightedRandomChestContent
import net.minecraft.world.World
import net.minecraftforge.common.ChestGenHooks

import scala.collection.mutable

object Delegator {
  def subItem(stack: ItemStack) =
    if (stack != null) stack.getItem match {
      case delegator: Delegator => delegator.subItem(stack.getItemDamage)
      case _ => None
    }
    else None
}

class Delegator extends Item with driver.item.UpgradeRenderer with Chargeable {
  setHasSubtypes(true)
  setCreativeTab(CreativeTab)
  setUnlocalizedName("oc.multi")
  iconString = Settings.resourceDomain + ":Microchip0"

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

  def subItem(stack: ItemStack): Option[traits.Delegate] =
    if (stack != null) subItem(stack.getItemDamage) match {
      case Some(subItem) if stack.getItem == this => Some(subItem)
      case _ => None
    }
    else None

  def subItem(damage: Int) =
    damage match {
      case itemId if itemId >= 0 && itemId < subItems.length => Some(subItems(itemId))
      case _ => None
    }

  override def getSubItems(item: Item, tab: CreativeTabs, list: util.List[_]) {
    // Workaround for MC's untyped lists...
    def add[T](list: util.List[T], value: Any) = list.add(value.asInstanceOf[T])
    subItems.indices.filter(subItems(_).showInItemList).
      map(subItems(_).createItemStack()).
      sortBy(_.getUnlocalizedName).
      foreach(add(list, _))
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
      case _ => EnumRarity.common
    }

  override def getColorFromItemStack(stack: ItemStack, pass: Int) =
    Delegator.subItem(stack) match {
      case Some(subItem) => subItem.color(stack, pass)
      case _ => super.getColorFromItemStack(stack, pass)
    }

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

  override def getChestGenBase(chest: ChestGenHooks, rnd: Random, original: WeightedRandomChestContent) = original

  // ----------------------------------------------------------------------- //

  override def doesSneakBypassUse(world: World, x: Int, y: Int, z: Int, player: EntityPlayer) =
    Delegator.subItem(player.getHeldItem) match {
      case Some(subItem) => subItem.doesSneakBypassUse(BlockPosition(x, y, z, world), player)
      case _ => super.doesSneakBypassUse(world, x, y, z, player)
    }

  override def onItemUseFirst(stack: ItemStack, player: EntityPlayer, world: World, x: Int, y: Int, z: Int, side: Int, hitX: Float, hitY: Float, hitZ: Float): Boolean =
    Delegator.subItem(stack) match {
      case Some(subItem) => subItem.onItemUseFirst(stack, player, BlockPosition(x, y, z, world), side, hitX, hitY, hitZ)
      case _ => super.onItemUseFirst(stack, player, world, x, y, z, side, hitX, hitY, hitZ)
    }

  override def onItemUse(stack: ItemStack, player: EntityPlayer, world: World, x: Int, y: Int, z: Int, side: Int, hitX: Float, hitY: Float, hitZ: Float): Boolean =
    Delegator.subItem(stack) match {
      case Some(subItem) => subItem.onItemUse(stack, player, BlockPosition(x, y, z, world), side, hitX, hitY, hitZ)
      case _ => super.onItemUse(stack, player, world, x, y, z, side, hitX, hitY, hitZ)
    }

  override def onItemRightClick(stack: ItemStack, world: World, player: EntityPlayer): ItemStack =
    Delegator.subItem(stack) match {
      case Some(subItem) => subItem.onItemRightClick(stack, world, player)
      case _ => super.onItemRightClick(stack, world, player)
    }

  // ----------------------------------------------------------------------- //

  override def onEaten(stack: ItemStack, world: World, player: EntityPlayer): ItemStack =
    Delegator.subItem(stack) match {
      case Some(subItem) => subItem.onEaten(stack, world, player)
      case _ => super.onEaten(stack, world, player)
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

  override def onPlayerStoppedUsing(stack: ItemStack, world: World, player: EntityPlayer, duration: Int): Unit =
    Delegator.subItem(stack) match {
      case Some(subItem) => subItem.onPlayerStoppedUsing(stack, player, duration)
      case _ => super.onPlayerStoppedUsing(stack, world, player, duration)
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
  override def addInformation(stack: ItemStack, player: EntityPlayer, tooltip: util.List[_], advanced: Boolean) {
    super.addInformation(stack, player, tooltip, advanced)
    Delegator.subItem(stack) match {
      case Some(subItem) => try subItem.tooltipLines(stack, player, tooltip.asInstanceOf[util.List[String]], advanced) catch {
        case t: Throwable => OpenComputers.log.warn("Error in item tooltip.", t)
      }
      case _ => // Nothing to add.
    }
  }

  override def getDisplayDamage(stack: ItemStack) =
    Delegator.subItem(stack) match {
      case Some(subItem) if subItem.isDamageable => subItem.damage(stack)
      case _ => super.getDisplayDamage(stack)
    }

  override def getMaxDamage(stack: ItemStack) =
    Delegator.subItem(stack) match {
      case Some(subItem) if subItem.isDamageable => subItem.maxDamage(stack)
      case _ => super.getMaxDamage(stack)
    }

  override def isDamaged(stack: ItemStack) =
    Delegator.subItem(stack) match {
      case Some(subItem) if subItem.isDamageable => subItem.damage(stack) > 0
      case _ => false
    }

  override def onUpdate(stack: ItemStack, world: World, player: Entity, slot: Int, selected: Boolean) =
    Delegator.subItem(stack) match {
      case Some(subItem) => subItem.update(stack, world, player, slot, selected)
      case _ => super.onUpdate(stack, world, player, slot, selected)
    }

  @SideOnly(Side.CLIENT)
  override def getIcon(stack: ItemStack, pass: Int) =
    Delegator.subItem(stack) match {
      case Some(subItem) => subItem.icon(stack, pass) match {
        case Some(icon) => icon
        case _ => super.getIcon(stack, pass)
      }
      case _ => super.getIcon(stack, pass)
    }

  @SideOnly(Side.CLIENT)
  override def getIconIndex(stack: ItemStack) = getIcon(stack, 0)

  @SideOnly(Side.CLIENT)
  override def getIconFromDamage(damage: Int): IIcon =
    subItem(damage) match {
      case Some(subItem) => subItem.icon match {
        case Some(icon) => icon
        case _ => super.getIconFromDamage(damage)
      }
      case _ => super.getIconFromDamage(damage)
    }

  @SideOnly(Side.CLIENT)
  override def registerIcons(iconRegister: IIconRegister) {
    super.registerIcons(iconRegister)
    subItems.foreach(_.registerIcons(iconRegister))
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

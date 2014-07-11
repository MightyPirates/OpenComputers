package li.cil.oc.common.item

import java.util
import java.util.Random
import java.util.logging.Level

import cpw.mods.fml.relauncher.{Side, SideOnly}
import li.cil.oc.common.tileentity
import li.cil.oc.{CreativeTab, OpenComputers, Settings}
import net.minecraft.client.renderer.texture.IconRegister
import net.minecraft.creativetab.CreativeTabs
import net.minecraft.entity.Entity
import net.minecraft.entity.player.EntityPlayer
import net.minecraft.item.{EnumRarity, Item, ItemStack}
import net.minecraft.util.{Icon, WeightedRandomChestContent}
import net.minecraft.world.World
import net.minecraftforge.common.ChestGenHooks

import scala.collection.mutable

class Delegator(id: Int) extends Item(id) {
  setHasSubtypes(true)
  setCreativeTab(CreativeTab)

  // ----------------------------------------------------------------------- //
  // SubItem
  // ----------------------------------------------------------------------- //

  override def getItemStackLimit(stack: ItemStack) =
    subItem(stack) match {
      case Some(subItem) => subItem.maxStackSize
      case _ => maxStackSize
    }

  val subItems = mutable.ArrayBuffer.empty[Delegate]

  def add(subItem: Delegate) = {
    val itemId = subItems.length
    subItems += subItem
    itemId
  }

  def subItem(stack: ItemStack): Option[Delegate] =
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

  override def getSubItems(itemId: Int, tab: CreativeTabs, list: util.List[_]) {
    // Workaround for MC's untyped lists...
    def add[T](list: util.List[T], value: Any) = list.add(value.asInstanceOf[T])
    (0 until subItems.length).filter(subItems(_).showInItemList).
      map(subItems(_).createItemStack()).
      sortBy(_.getUnlocalizedName).
      foreach(add(list, _))
  }

  // ----------------------------------------------------------------------- //
  // Item
  // ----------------------------------------------------------------------- //

  override def getUnlocalizedName: String = Settings.namespace + "item"

  override def getUnlocalizedName(stack: ItemStack): String =
    subItem(stack) match {
      case Some(subItem) => Settings.namespace + "item." + subItem.unlocalizedName
      case _ => getUnlocalizedName
    }

  override def isBookEnchantable(itemA: ItemStack, itemB: ItemStack): Boolean = false

  override def getRarity(stack: ItemStack) = subItem(stack) match {
    case Some(subItem) => subItem.rarity
    case _ => EnumRarity.common
  }

  override def getColorFromItemStack(stack: ItemStack, pass: Int) =
    subItem(stack) match {
      case Some(subItem) => subItem.color(stack, pass)
      case _ => super.getColorFromItemStack(stack, pass)
    }

  override def getChestGenBase(chest: ChestGenHooks, rnd: Random, original: WeightedRandomChestContent) = original

  override def shouldPassSneakingClickToBlock(world: World, x: Int, y: Int, z: Int) = {
    world.getBlockTileEntity(x, y, z) match {
      case drive: tileentity.DiskDrive => true
      case _ => super.shouldPassSneakingClickToBlock(world, x, y, z)
    }
  }

  // ----------------------------------------------------------------------- //

  override def onItemUseFirst(stack: ItemStack, player: EntityPlayer, world: World, x: Int, y: Int, z: Int, side: Int, hitX: Float, hitY: Float, hitZ: Float): Boolean =
    subItem(stack) match {
      case Some(subItem) => subItem.onItemUseFirst(stack, player, world, x, y, z, side, hitX, hitY, hitZ)
      case _ => super.onItemUseFirst(stack, player, world, x, y, z, side, hitX, hitY, hitZ)
    }

  override def onItemUse(stack: ItemStack, player: EntityPlayer, world: World, x: Int, y: Int, z: Int, side: Int, hitX: Float, hitY: Float, hitZ: Float): Boolean =
    subItem(stack) match {
      case Some(subItem) => subItem.onItemUse(stack, player, world, x, y, z, side, hitX, hitY, hitZ)
      case _ => super.onItemUse(stack, player, world, x, y, z, side, hitX, hitY, hitZ)
    }

  override def onItemRightClick(stack: ItemStack, world: World, player: EntityPlayer): ItemStack =
    subItem(stack) match {
      case Some(subItem) => subItem.onItemRightClick(stack, world, player)
      case _ => super.onItemRightClick(stack, world, player)
    }

  // ----------------------------------------------------------------------- //

  override def getItemDisplayName(stack: ItemStack) =
    subItem(stack) match {
      case Some(subItem) => subItem.displayName(stack) match {
        case Some(name) => name
        case _ => super.getItemDisplayName(stack)
      }
      case _ => super.getItemDisplayName(stack)
    }

  @SideOnly(Side.CLIENT)
  override def addInformation(stack: ItemStack, player: EntityPlayer, tooltip: util.List[_], advanced: Boolean) {
    super.addInformation(stack, player, tooltip, advanced)
    subItem(stack) match {
      case Some(subItem) => try subItem.tooltipLines(stack, player, tooltip.asInstanceOf[util.List[String]], advanced) catch {
        case t: Throwable => OpenComputers.log.log(Level.WARNING, "Error in item tooltip.", t)
      }
      case _ => // Nothing to add.
    }
  }

  override def getDisplayDamage(stack: ItemStack) =
    subItem(stack) match {
      case Some(subItem) if subItem.isDamageable => subItem.damage(stack)
      case _ => super.getDisplayDamage(stack)
    }

  override def getMaxDamage(stack: ItemStack) =
    subItem(stack) match {
      case Some(subItem) if subItem.isDamageable => subItem.maxDamage(stack)
      case _ => super.getMaxDamage(stack)
    }

  override def isDamaged(stack: ItemStack) =
    subItem(stack) match {
      case Some(subItem) if subItem.isDamageable => subItem.damage(stack) > 0
      case _ => false
    }

  override def onUpdate(stack: ItemStack, world: World, player: Entity, slot: Int, selected: Boolean) =
    subItem(stack) match {
      case Some(subItem) => subItem.update(stack, world, player, slot, selected)
      case _ => super.onUpdate(stack, world, player, slot, selected)
    }

  @SideOnly(Side.CLIENT)
  override def getIcon(stack: ItemStack, pass: Int) =
    subItem(stack) match {
      case Some(subItem) => subItem.icon(stack, pass) match {
        case Some(icon) => icon
        case _ => super.getIcon(stack, pass)
      }
      case _ => super.getIcon(stack, pass)
    }

  @SideOnly(Side.CLIENT)
  override def getIconIndex(stack: ItemStack) = getIcon(stack, 0)

  @SideOnly(Side.CLIENT)
  override def getIconFromDamage(damage: Int): Icon =
    subItem(damage) match {
      case Some(subItem) => subItem.icon match {
        case Some(icon) => icon
        case _ => super.getIconFromDamage(damage)
      }
      case _ => super.getIconFromDamage(damage)
    }

  @SideOnly(Side.CLIENT)
  override def registerIcons(iconRegister: IconRegister) {
    super.registerIcons(iconRegister)
    subItems.foreach(_.registerIcons(iconRegister))
  }

  override def toString = getUnlocalizedName
}

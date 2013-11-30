package li.cil.oc.common.item

import cpw.mods.fml.relauncher.{Side, SideOnly}
import java.util
import li.cil.oc.api.driver.Slot
import li.cil.oc.server.driver.Registry
import li.cil.oc.{Settings, CreativeTab}
import net.minecraft.client.renderer.texture.IconRegister
import net.minecraft.creativetab.CreativeTabs
import net.minecraft.entity.player.EntityPlayer
import net.minecraft.item.{EnumRarity, ItemStack, Item}
import net.minecraft.util.Icon
import net.minecraft.world.World
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
    subItem(stack.getItemDamage) match {
      case Some(subItem) if stack.getItem == this => Some(subItem)
      case _ => None
    }

  def subItem(damage: Int) =
    damage match {
      case itemId if itemId >= 0 && itemId < subItems.length => Some(subItems(itemId))
      case _ => None
    }

  override def getSubItems(itemId: Int, tab: CreativeTabs, list: util.List[_]) {
    // Workaround for MC's untyped lists...
    def add[T](list: util.List[T], value: Any) = list.add(value.asInstanceOf[T])
    (0 until subItems.length).
      foreach(id => add(list, new ItemStack(this, 1, id)))
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

  override def getRarity(stack: ItemStack): EnumRarity = {
    Registry.driverFor(stack) match {
      case Some(driver) => driver.slot(stack) match {
        case Slot.Card => return EnumRarity.uncommon
        case Slot.HardDiskDrive => return EnumRarity.rare
        case Slot.Upgrade => return EnumRarity.epic
        case _ =>
      }
      case _ =>
    }
    EnumRarity.common
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
      case Some(subItem) => subItem.tooltipLines(stack, player, tooltip.asInstanceOf[util.List[String]], advanced)
      case _ => // Nothing to add.
    }
  }

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

  override def toString: String = {
    getUnlocalizedName
  }

}

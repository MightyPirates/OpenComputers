package li.cil.oc.common.items

import cpw.mods.fml.common.registry.GameRegistry
import java.util
import li.cil.oc.CreativeTab
import net.minecraft.client.renderer.texture.IconRegister
import net.minecraft.creativetab.CreativeTabs
import net.minecraft.entity.player.EntityPlayer
import net.minecraft.item.{EnumRarity, ItemStack, Item}
import net.minecraft.util.Icon
import net.minecraft.world.World
import scala.collection.mutable

class ItemMulti(id: Int) extends Item(id) {
  setMaxStackSize(1)
  setHasSubtypes(true)
  setCreativeTab(CreativeTab)
  GameRegistry.registerItem(this, "oc.item." + id)

  // ----------------------------------------------------------------------- //
  // SubItem
  // ----------------------------------------------------------------------- //

  val subItems = mutable.ArrayBuffer.empty[SubItem]

  def add(subItem: SubItem) = {
    val itemId = subItems.length
    subItems += subItem
    itemId
  }

  def subItem(item: ItemStack): Option[SubItem] =
    subItem(item.getItemDamage) match {
      case Some(subItem) if item.itemID == this.itemID => Some(subItem)
      case _ => None
    }

  def subItem(damage: Int) =
    damage match {
      case itemId if itemId >= 0 && itemId < subItems.length => Some(subItems(itemId))
      case _ => None
    }

  // ----------------------------------------------------------------------- //
  // Item
  // ----------------------------------------------------------------------- //

  override def getIconFromDamage(damage: Int): Icon =
    subItem(damage) match {
      case None => super.getIconFromDamage(damage)
      case Some(subItem) => subItem.icon match {
        case null => super.getIconFromDamage(damage)
        case icon => icon
      }
    }

  override def isBookEnchantable(itemA: ItemStack, itemB: ItemStack): Boolean = false

  override def getUnlocalizedName(item: ItemStack): String =
    subItem(item) match {
      case None => getUnlocalizedName
      case Some(subItem) => "oc.item." + subItem.unlocalizedName
    }

  override def getUnlocalizedName: String = "oc.item"

  override def getRarity(item: ItemStack) = EnumRarity.epic

  override def getSubItems(par1: Int, par2CreativeTabs: CreativeTabs, par3List: util.List[_]) {
    super.getSubItems(par1, par2CreativeTabs, par3List)
  }

  override def onItemRightClick(item: ItemStack, world: World, player: EntityPlayer): ItemStack =
    subItem(item) match {
      case None => super.onItemRightClick(item, world, player)
      case Some(subItem) => subItem.onItemRightClick(item, world, player)
    }

  override def onItemUse(item: ItemStack, player: EntityPlayer, world: World, x: Int, y: Int, z: Int, side: Int, hitX: Float, hitY: Float, hitZ: Float): Boolean =
    subItem(item) match {
      case None => super.onItemUse(item, player, world, x, y, z, side, hitX, hitY, hitZ)
      case Some(subItem) => subItem.onItemUse(item, player, world, x, y, z, side, hitX, hitY, hitZ)
    }

  override def registerIcons(iconRegister: IconRegister) {
    super.registerIcons(iconRegister)
    subItems.foreach(_.registerIcons(iconRegister))
  }
}

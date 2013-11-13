package li.cil.oc.common.item

import cpw.mods.fml.common.registry.GameRegistry
import java.util
import li.cil.oc.{Config, CreativeTab}
import net.minecraft.client.renderer.texture.IconRegister
import net.minecraft.creativetab.CreativeTabs
import net.minecraft.entity.player.EntityPlayer
import net.minecraft.item.{EnumRarity, ItemStack, Item}
import net.minecraft.util.Icon
import net.minecraft.world.World
import scala.collection.mutable

class Delegator(id: Int) extends Item(id) {
  setMaxStackSize(1)
  setHasSubtypes(true)
  setCreativeTab(CreativeTab)
  GameRegistry.registerItem(this, Config.namespace + "item")

  // ----------------------------------------------------------------------- //
  // SubItem
  // ----------------------------------------------------------------------- //

  val subItems = mutable.ArrayBuffer.empty[Delegate]

  def add(subItem: Delegate) = {
    val itemId = subItems.length
    subItems += subItem
    itemId
  }

  def subItem(item: ItemStack): Option[Delegate] =
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

  override def addInformation(item: ItemStack, player: EntityPlayer, tooltip: util.List[_], advanced: Boolean) {
    super.addInformation(item, player, tooltip, advanced)
    subItem(item) match {
      case Some(subItem) => subItem.addInformation(item, player, tooltip.asInstanceOf[util.List[String]], advanced)
      case _ => // Nothing to add.
    }
  }

  override def getIconFromDamage(damage: Int): Icon =
    subItem(damage) match {
      case Some(subItem) => subItem.icon match {
        case Some(icon) => icon
        case _ => super.getIconFromDamage(damage)
      }
      case _ => super.getIconFromDamage(damage)
    }

  override def getRarity(item: ItemStack) = EnumRarity.uncommon

  override def getShareTag = false

  override def getSubItems(itemId: Int, tab: CreativeTabs, list: util.List[_]) {
    // Workaround for MC's untyped lists...
    def add[T](list: util.List[T], value: Any) = list.add(value.asInstanceOf[T])
    (0 until subItems.length).
      foreach(id => add(list, new ItemStack(this, 1, id)))
  }

  override def getUnlocalizedName(item: ItemStack): String =
    subItem(item) match {
      case Some(subItem) => Config.namespace + "item." + subItem.unlocalizedName
      case _ => getUnlocalizedName
    }

  override def getUnlocalizedName: String = Config.namespace + "item"

  override def isBookEnchantable(itemA: ItemStack, itemB: ItemStack): Boolean = false

  override def onItemRightClick(item: ItemStack, world: World, player: EntityPlayer): ItemStack =
    subItem(item) match {
      case Some(subItem) => subItem.onItemRightClick(item, world, player)
      case _ => super.onItemRightClick(item, world, player)
    }

  override def onItemUse(item: ItemStack, player: EntityPlayer, world: World, x: Int, y: Int, z: Int, side: Int, hitX: Float, hitY: Float, hitZ: Float): Boolean =
    subItem(item) match {
      case Some(subItem) => subItem.onItemUse(item, player, world, x, y, z, side, hitX, hitY, hitZ)
      case _ => super.onItemUse(item, player, world, x, y, z, side, hitX, hitY, hitZ)
    }

  override def registerIcons(iconRegister: IconRegister) {
    super.registerIcons(iconRegister)
    subItems.foreach(_.registerIcons(iconRegister))
  }
}

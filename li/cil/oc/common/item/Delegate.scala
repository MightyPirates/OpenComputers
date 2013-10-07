package li.cil.oc.common.item

import net.minecraft.client.renderer.texture.IconRegister
import net.minecraft.entity.player.EntityPlayer
import net.minecraft.item.ItemStack
import net.minecraft.util.Icon
import net.minecraft.world.World

trait Delegate {
  def parent: Delegator

  def unlocalizedName: String

  val itemId = parent.add(this)

  private var _icon: Option[Icon] = None

  // ----------------------------------------------------------------------- //
  // Item
  // ----------------------------------------------------------------------- //

  def addInformation(item: ItemStack, player: EntityPlayer, tooltip: java.util.List[String], advanced: Boolean) {}

  def icon: Option[Icon] = _icon

  protected def icon_=(value: Icon) = _icon = Some(value)

  def onItemRightClick(item: ItemStack, world: World, player: EntityPlayer): ItemStack = item

  def onItemUse(item: ItemStack, player: EntityPlayer, world: World, x: Int, y: Int, z: Int, side: Int, hitX: Float, hitY: Float, hitZ: Float): Boolean = false

  def registerIcons(iconRegister: IconRegister) {}
}

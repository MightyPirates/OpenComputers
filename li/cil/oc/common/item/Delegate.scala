package li.cil.oc.common.item

import li.cil.oc.Config
import net.minecraft.client.renderer.texture.IconRegister
import net.minecraft.entity.player.EntityPlayer
import net.minecraft.item.ItemStack
import net.minecraft.util.Icon
import net.minecraft.world.World

trait Delegate {
  val parent: Delegator

  val unlocalizedName: String

  val itemId = parent.add(this)

  private var _icon: Option[Icon] = None

  def maxStackSize = 64

  def createItemStack(amount: Int = 1) = new ItemStack(parent, amount, itemId)

  // ----------------------------------------------------------------------- //

  def addInformation(stack: ItemStack, player: EntityPlayer, tooltip: java.util.List[String], advanced: Boolean) {
    if (stack.hasTagCompound && stack.getTagCompound.hasKey(Config.namespace + "data")) {
      val data = stack.getTagCompound.getCompoundTag(Config.namespace + "data")
      if (data.hasKey("node") && data.getCompoundTag("node").hasKey("address")) {
        tooltip.add("§8" + data.getCompoundTag("node").getString("address").substring(0, 13) + "...§7")
      }
    }
  }

  def getItemDisplayName(stack: ItemStack): Option[String] = None

  def icon: Option[Icon] = _icon

  protected def icon_=(value: Icon) = _icon = Option(value)

  def onItemRightClick(stack: ItemStack, world: World, player: EntityPlayer): ItemStack = {
    if (player.isSneaking) {
      if (stack.hasTagCompound && stack.getTagCompound.hasKey(Config.namespace + "data")) {
        stack.setTagCompound(null)
        player.swingItem()
      }
    }
    stack
  }

  def onItemUse(stack: ItemStack, player: EntityPlayer, world: World, x: Int, y: Int, z: Int, side: Int, hitX: Float, hitY: Float, hitZ: Float): Boolean = false

  def onItemUseFirst(stack: ItemStack, player: EntityPlayer, world: World, x: Int, y: Int, z: Int, side: Int, hitX: Float, hitY: Float, hitZ: Float): Boolean = false

  def registerIcons(iconRegister: IconRegister) {}

  def equals(stack: ItemStack) =
    stack != null && stack.getItem == parent && parent.subItem(stack).exists(_ == this)
}

package li.cil.oc.common.item

import java.util

import cpw.mods.fml.relauncher.{Side, SideOnly}
import li.cil.oc.{Settings, api}
import li.cil.oc.client.KeyBindings
import li.cil.oc.util.{ItemCosts, Rarity}
import net.minecraft.client.renderer.texture.IIconRegister
import net.minecraft.entity.Entity
import net.minecraft.entity.player.EntityPlayer
import net.minecraft.item.ItemStack
import net.minecraft.util.{IIcon, StatCollector}
import net.minecraft.world.World
import org.lwjgl.input

trait Delegate {
  val parent: Delegator

  val unlocalizedName: String

  var showInItemList = true

  val itemId = parent.add(this)

  private var _icon: Option[IIcon] = None

  def maxStackSize = 64

  def createItemStack(amount: Int = 1) = new ItemStack(parent, amount, itemId)

  // ----------------------------------------------------------------------- //

  def onItemUseFirst(stack: ItemStack, player: EntityPlayer, world: World, x: Int, y: Int, z: Int, side: Int, hitX: Float, hitY: Float, hitZ: Float): Boolean = false

  def onItemUse(stack: ItemStack, player: EntityPlayer, world: World, x: Int, y: Int, z: Int, side: Int, hitX: Float, hitY: Float, hitZ: Float): Boolean = false

  def onItemRightClick(stack: ItemStack, world: World, player: EntityPlayer): ItemStack = {
    if (player.isSneaking) {
      if (stack.hasTagCompound && stack.getTagCompound.hasKey(Settings.namespace + "data")) {
        stack.setTagCompound(null)
        player.swingItem()
      }
    }
    stack
  }

  def update(stack: ItemStack, world: World, player: Entity, slot: Int, selected: Boolean) {}

  // ----------------------------------------------------------------------- //

  def rarity = Rarity.byTier(tierFromDriver)

  protected def tierFromDriver = {
    val stack = createItemStack()
    api.Driver.driverFor(stack) match {
      case driver: api.driver.Item => driver.tier(stack)
      case _ => 0
    }
  }

  def displayName(stack: ItemStack): Option[String] = None

  @SideOnly(Side.CLIENT)
  def tooltipLines(stack: ItemStack, player: EntityPlayer, tooltip: java.util.List[String], advanced: Boolean) {
    if (ItemCosts.hasCosts(stack)) {
      if (KeyBindings.showMaterialCosts) {
        ItemCosts.addTooltip(stack, tooltip.asInstanceOf[util.List[String]])
      }
      else {
        tooltip.add(StatCollector.translateToLocalFormatted(
          Settings.namespace + "tooltip.MaterialCosts",
          input.Keyboard.getKeyName(KeyBindings.materialCosts.getKeyCode)))
      }
    }
    if (stack.hasTagCompound && stack.getTagCompound.hasKey(Settings.namespace + "data")) {
      val data = stack.getTagCompound.getCompoundTag(Settings.namespace + "data")
      if (data.hasKey("node") && data.getCompoundTag("node").hasKey("address")) {
        tooltip.add("§8" + data.getCompoundTag("node").getString("address").substring(0, 13) + "...§7")
      }
    }
  }

  def isDamageable = false

  def damage(stack: ItemStack) = 0

  def maxDamage(stack: ItemStack) = 0

  @SideOnly(Side.CLIENT)
  def icon: Option[IIcon] = _icon

  @SideOnly(Side.CLIENT)
  protected def icon_=(value: IIcon) = _icon = Option(value)

  @SideOnly(Side.CLIENT)
  def icon(stack: ItemStack, pass: Int): Option[IIcon] = icon

  @SideOnly(Side.CLIENT)
  def registerIcons(iconRegister: IIconRegister) {}

  // ----------------------------------------------------------------------- //

  def equals(stack: ItemStack) =
    stack != null && stack.getItem == parent && parent.subItem(stack).exists(_ == this)
}

package li.cil.oc.common.item

import net.minecraft.item.{EnumRarity, ItemStack}
import net.minecraft.entity.player.EntityPlayer
import li.cil.oc.util.Tooltip
import net.minecraft.client.renderer.texture.IconRegister
import li.cil.oc.Settings
import java.util

class EntangledCard(val parent: Delegator) extends Delegate {
  val unlocalizedName = "EntangledCard"

  override def rarity = EnumRarity.rare

  override def tooltipLines(stack: ItemStack, player: EntityPlayer, tooltip: util.List[String], advanced: Boolean) {
    tooltip.addAll(Tooltip.get(unlocalizedName))
    super.tooltipLines(stack, player, tooltip, advanced)
  }

  override def registerIcons(iconRegister: IconRegister) = {
    super.registerIcons(iconRegister)

    icon = iconRegister.registerIcon(Settings.resourceDomain + ":card_entangled")
  }
}

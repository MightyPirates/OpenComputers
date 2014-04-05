package li.cil.oc.common.item

import java.util
import net.minecraft.item.{ItemStack, EnumRarity}
import net.minecraft.entity.player.EntityPlayer
import li.cil.oc.Settings
import li.cil.oc.util.Tooltip
import net.minecraft.client.renderer.texture.IconRegister

class UpgradeAngel(val parent: Delegator) extends Delegate {
  val unlocalizedName = "UpgradeAngel"

  override def rarity = EnumRarity.epic

  override def tooltipLines(stack: ItemStack, player: EntityPlayer, tooltip: util.List[String], advanced: Boolean) {
    tooltip.addAll(Tooltip.get(unlocalizedName))
    super.tooltipLines(stack, player, tooltip, advanced)
  }

  override def registerIcons(iconRegister: IconRegister) = {
    super.registerIcons(iconRegister)

    icon = iconRegister.registerIcon(Settings.resourceDomain + ":upgrade_angel")
  }
}

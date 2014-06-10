package li.cil.oc.common.item

import java.util
import li.cil.oc.Settings
import li.cil.oc.util.{Tooltip, PackedColor}
import net.minecraft.client.renderer.texture.IIconRegister
import net.minecraft.entity.player.EntityPlayer
import net.minecraft.item.ItemStack

class GraphicsCard(val parent: Delegator, val tier: Int) extends Delegate {
  val baseName = "GraphicsCard"
  val unlocalizedName = baseName + tier

  override def tooltipLines(stack: ItemStack, player: EntityPlayer, tooltip: util.List[String], advanced: Boolean) {
    val (w, h) = Settings.screenResolutionsByTier(tier)
    val depth = PackedColor.Depth.bits(Settings.screenDepthsByTier(tier))
    tooltip.addAll(Tooltip.get(baseName,
      w, h, depth,
      tier match {
        case 0 => "1/1/4/2/2"
        case 1 => "2/4/8/4/4"
        case 2 => "4/8/16/8/8"
      }))
    super.tooltipLines(stack, player, tooltip, advanced)
  }

  override def registerIcons(iconRegister: IIconRegister) {
    super.registerIcons(iconRegister)

    icon = iconRegister.registerIcon(Settings.resourceDomain + ":card_graphics" + tier)
  }
}
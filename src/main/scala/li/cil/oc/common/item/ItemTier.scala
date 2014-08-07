package li.cil.oc.common.item

import java.util

import cpw.mods.fml.relauncher.{Side, SideOnly}
import li.cil.oc.Localization
import net.minecraft.entity.player.EntityPlayer
import net.minecraft.item.ItemStack

trait ItemTier extends Delegate { self: Delegate =>
  @SideOnly(Side.CLIENT)
  override def tooltipLines(stack: ItemStack, player: EntityPlayer, tooltip: util.List[String], advanced: Boolean) {
    super.tooltipLines(stack, player, tooltip, advanced)
    if (advanced) {
      tooltip.add(Localization.Tooltip.Tier(tierFromDriver + 1))
    }
  }
}

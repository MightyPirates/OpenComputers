package li.cil.oc.common.item.traits

import java.util

import li.cil.oc.Localization
import net.minecraft.entity.player.EntityPlayer
import net.minecraft.item.ItemStack
import net.minecraftforge.fml.relauncher.Side
import net.minecraftforge.fml.relauncher.SideOnly

trait ItemTier extends Delegate {
  self: Delegate =>
  @SideOnly(Side.CLIENT)
  override def tooltipLines(stack: ItemStack, player: EntityPlayer, tooltip: util.List[String], advanced: Boolean) {
    super.tooltipLines(stack, player, tooltip, advanced)
    if (advanced) {
      tooltip.add(Localization.Tooltip.Tier(tierFromDriver(stack) + 1))
    }
  }
}

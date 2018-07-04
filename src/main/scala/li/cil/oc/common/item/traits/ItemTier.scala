package li.cil.oc.common.item.traits

import java.util

import li.cil.oc.Localization
import net.minecraft.client.util.ITooltipFlag
import net.minecraft.entity.player.EntityPlayer
import net.minecraft.item.ItemStack
import net.minecraft.world.World
import net.minecraftforge.fml.relauncher.Side
import net.minecraftforge.fml.relauncher.SideOnly

trait ItemTier extends Delegate {
  self: Delegate =>
  @SideOnly(Side.CLIENT)
  override def tooltipLines(stack: ItemStack, world: World, tooltip: util.List[String], flag: ITooltipFlag) {
    super.tooltipLines(stack, world, tooltip, flag)
    if (flag.isAdvanced) {
      tooltip.add(Localization.Tooltip.Tier(tierFromDriver(stack) + 1))
    }
  }
}

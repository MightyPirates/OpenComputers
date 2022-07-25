package li.cil.oc.common.item

import java.util

import li.cil.oc.util.UpgradeExperience
import net.minecraft.client.util.ITooltipFlag
import net.minecraft.item.ItemStack
import net.minecraft.util.text.ITextComponent
import net.minecraft.util.text.StringTextComponent
import net.minecraft.world.World
import net.minecraftforge.api.distmarker.{Dist, OnlyIn}
import li.cil.oc.Localization;

class UpgradeExperience(val parent: Delegator) extends traits.Delegate with traits.ItemTier {

  @OnlyIn(Dist.CLIENT) override
  def tooltipLines(stack: ItemStack, world: World, tooltip: util.List[ITextComponent], flag: ITooltipFlag): Unit = {
    if (stack.hasTag) {
      val nbt = li.cil.oc.integration.opencomputers.Item.dataTag(stack)
      val experience = UpgradeExperience.getExperience(nbt)
      val level = UpgradeExperience.calculateLevelFromExperience(experience)
      val reportedLevel = UpgradeExperience.calculateExperienceLevel(level, experience)
      tooltip.add(new StringTextComponent(Localization.Tooltip.ExperienceLevel(reportedLevel)))
    }
    super.tooltipLines(stack, world, tooltip, flag)
  }
}

package li.cil.oc.common.item

import java.util

import li.cil.oc.Localization
import li.cil.oc.util.Tooltip
import li.cil.oc.util.{UpgradeExperience => ExperienceUtil}
import net.minecraft.client.util.ITooltipFlag
import net.minecraft.item.Item
import net.minecraft.item.Item.Properties
import net.minecraft.item.ItemStack
import net.minecraft.util.text.ITextComponent
import net.minecraft.util.text.StringTextComponent
import net.minecraft.world.World
import net.minecraftforge.api.distmarker.{Dist, OnlyIn}
import net.minecraftforge.common.extensions.IForgeItem

class UpgradeExperience(props: Properties) extends Item(props) with IForgeItem with traits.SimpleItem with traits.ItemTier {
  @OnlyIn(Dist.CLIENT)
  override def appendHoverText(stack: ItemStack, world: World, tooltip: util.List[ITextComponent], flag: ITooltipFlag) {
    super.appendHoverText(stack, world, tooltip, flag)
    if (stack.hasTag) {
      val nbt = li.cil.oc.integration.opencomputers.Item.dataTag(stack)
      val experience = ExperienceUtil.getExperience(nbt)
      val level = ExperienceUtil.calculateLevelFromExperience(experience)
      val reportedLevel = ExperienceUtil.calculateExperienceLevel(level, experience)
      tooltip.add(new StringTextComponent(Localization.Tooltip.ExperienceLevel(reportedLevel)).setStyle(Tooltip.DefaultStyle))
    }
  }
}

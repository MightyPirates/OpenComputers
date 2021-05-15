package li.cil.oc.common.item

import java.util

import li.cil.oc.util.UpgradeExperience
import net.minecraft.client.util.ITooltipFlag
import net.minecraft.item.ItemStack
import net.minecraft.world.World
import net.minecraftforge.fml.relauncher.{Side, SideOnly}
import li.cil.oc.Localization;

class UpgradeExperience(val parent: Delegator) extends traits.Delegate with traits.ItemTier {

  @SideOnly(Side.CLIENT) override
  def tooltipLines(stack: ItemStack, world: World, tooltip: util.List[String], flag: ITooltipFlag): Unit = {
    if (stack.hasTagCompound) {
      val nbt = li.cil.oc.integration.opencomputers.Item.dataTag(stack)
      val experience = UpgradeExperience.getExperience(nbt)
      val level = UpgradeExperience.calculateLevelFromExperience(experience)
      val reportedLevel = UpgradeExperience.calculateExperienceLevel(level, experience)
      tooltip.add(Localization.Tooltip.ExperienceLevel(reportedLevel))
    }
    super.tooltipLines(stack, world, tooltip, flag)
  }
}

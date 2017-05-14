package li.cil.oc.common.item

import java.util

import li.cil.oc.common.Tier
import li.cil.oc.integration.Mods
import li.cil.oc.util.Tooltip
import net.minecraft.item.ItemStack

class RedstoneCard(val parent: Delegator, val tier: Int) extends traits.Delegate with traits.ItemTier {
  override val unlocalizedName = super.unlocalizedName + tier

  override protected def tooltipName = Option(super.unlocalizedName)

  // Note: T2 is enabled in mod integration, if it makes sense.
  showInItemList = tier == Tier.One

  override protected def tooltipExtended(stack: ItemStack, tooltip: util.List[String]) {
    super.tooltipExtended(stack, tooltip)
    if (tier == Tier.Two) {
      if (Mods.ProjectRedTransmission.isModAvailable) {
        tooltip.addAll(Tooltip.get(super.unlocalizedName + ".ProjectRed"))
      }
      if (Mods.RedLogic.isModAvailable) {
        tooltip.addAll(Tooltip.get(super.unlocalizedName + ".RedLogic"))
      }
      if (Mods.MineFactoryReloaded.isModAvailable) {
        tooltip.addAll(Tooltip.get(super.unlocalizedName + ".RedNet"))
      }
      if (Mods.WirelessRedstoneCBE.isModAvailable) {
        tooltip.addAll(Tooltip.get(super.unlocalizedName + ".WirelessCBE"))
      }
      if (Mods.WirelessRedstoneSVE.isModAvailable) {
        tooltip.addAll(Tooltip.get(super.unlocalizedName + ".WirelessSV"))
      }
    }
  }
}

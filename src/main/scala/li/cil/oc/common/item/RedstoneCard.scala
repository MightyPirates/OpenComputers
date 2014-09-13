package li.cil.oc.common.item

import java.util

import li.cil.oc.common.Tier
import li.cil.oc.util.Tooltip
import li.cil.oc.util.mods.{BundledRedstone, Mods, WirelessRedstone}
import net.minecraft.item.ItemStack

class RedstoneCard(val parent: Delegator, val tier: Int) extends Delegate with ItemTier {
  override val unlocalizedName = super.unlocalizedName + tier

  override protected def tooltipName = Option(super.unlocalizedName)

  showInItemList = tier == Tier.One || BundledRedstone.isAvailable || WirelessRedstone.isAvailable

  override protected def tooltipExtended(stack: ItemStack, tooltip: util.List[String]) {
    super.tooltipExtended(stack, tooltip)
    if (tier == Tier.Two) {
      if (Mods.ProjectRedTransmission.isAvailable) {
        tooltip.addAll(Tooltip.get(super.unlocalizedName + ".ProjectRed"))
      }
      if (Mods.RedLogic.isAvailable) {
        tooltip.addAll(Tooltip.get(super.unlocalizedName + ".RedLogic"))
      }
      if (Mods.MineFactoryReloaded.isAvailable) {
        tooltip.addAll(Tooltip.get(super.unlocalizedName + ".RedNet"))
      }
      if (Mods.WirelessRedstoneCBE.isAvailable) {
        tooltip.addAll(Tooltip.get(super.unlocalizedName + ".WirelessCBE"))
      }
      if (Mods.WirelessRedstoneSV.isAvailable) {
        tooltip.addAll(Tooltip.get(super.unlocalizedName + ".WirelessSV"))
      }
    }
  }
}

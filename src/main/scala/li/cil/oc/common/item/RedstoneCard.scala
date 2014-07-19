package li.cil.oc.common.item

import java.util

import li.cil.oc.common.InventorySlots.Tier
import li.cil.oc.util.Tooltip
import li.cil.oc.util.mods.{BundledRedstone, Mods, WirelessRedstone}
import net.minecraft.entity.player.EntityPlayer
import net.minecraft.item.ItemStack

class RedstoneCard(val parent: Delegator, val tier: Int) extends Delegate {
  override val unlocalizedName = super.unlocalizedName + tier

  showInItemList = tier == Tier.One || BundledRedstone.isAvailable || WirelessRedstone.isAvailable

  override def tooltipLines(stack: ItemStack, player: EntityPlayer, tooltip: util.List[String], advanced: Boolean) {
    tooltip.addAll(Tooltip.get(super.unlocalizedName))
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
    tooltipCosts(stack, tooltip)
  }
}

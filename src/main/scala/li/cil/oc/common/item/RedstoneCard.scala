package li.cil.oc.common.item

import java.util

import li.cil.oc.Settings
import li.cil.oc.common.InventorySlots.Tier
import li.cil.oc.util.Tooltip
import li.cil.oc.util.mods.{BundledRedstone, Mods, WirelessRedstone}
import net.minecraft.entity.player.EntityPlayer
import net.minecraft.item.ItemStack

class RedstoneCard(val parent: Delegator, val tier: Int) extends Delegate {
  val baseName = "RedstoneCard"
  val unlocalizedName = baseName + tier

  showInItemList = tier == Tier.One || BundledRedstone.isAvailable || WirelessRedstone.isAvailable

  override def tooltipLines(stack: ItemStack, player: EntityPlayer, tooltip: util.List[String], advanced: Boolean) {
    tooltip.addAll(Tooltip.get(baseName))
    if (tier == Tier.Two) {
      if (Mods.ProjectRed.isAvailable) {
        tooltip.addAll(Tooltip.get(baseName + ".ProjectRed"))
      }
    if (Mods.RedLogic.isAvailable) {
        tooltip.addAll(Tooltip.get(baseName + ".RedLogic"))
    }
    if (Mods.MineFactoryReloaded.isAvailable) {
        tooltip.addAll(Tooltip.get(baseName + ".RedNet"))
    }
      if (Mods.WirelessRedstoneCBE.isAvailable) {
        tooltip.addAll(Tooltip.get(baseName + ".WirelessCBE"))
      }
      if (Mods.WirelessRedstoneSV.isAvailable) {
        tooltip.addAll(Tooltip.get(baseName + ".WirelessSV"))
      }
    }
    super.tooltipLines(stack, player, tooltip, advanced)
  }

  override def registerIcons(iconRegister: IconRegister) {
    super.registerIcons(iconRegister)

    icon = iconRegister.registerIcon(Settings.resourceDomain + ":card_redstone" + tier)
  }
}

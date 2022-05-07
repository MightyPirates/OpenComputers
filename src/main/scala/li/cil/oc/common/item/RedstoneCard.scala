package li.cil.oc.common.item

import java.util

import li.cil.oc.common.Tier
import net.minecraft.item.ItemStack

class RedstoneCard(val parent: Delegator, val tier: Int) extends traits.Delegate with traits.ItemTier {
  override val unlocalizedName = super.unlocalizedName + tier

  override protected def tooltipName = Option(super.unlocalizedName)

  // Note: T2 is enabled in mod integration, if it makes sense.
  showInItemList = tier == Tier.One
}

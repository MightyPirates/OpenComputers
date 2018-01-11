package li.cil.oc.common.item

import li.cil.oc.Settings
import li.cil.oc.common.Tier
import li.cil.oc.util.Rarity
import net.minecraft.item.EnumRarity
import net.minecraft.item.ItemStack

class ComponentBus(val parent: Delegator, val tier: Int) extends traits.Delegate with traits.ItemTier {
  override val unlocalizedName = super.unlocalizedName + tier

  // Because the driver considers the creative bus to be tier 3, the superclass
  // will believe it has T3 rarity. We override that here.
  override def rarity(stack: ItemStack): EnumRarity =
    if (tier == Tier.Four) Rarity.byTier(Tier.Four)
    else super.rarity(stack)

  override protected def tooltipName = Option(super.unlocalizedName)

  override protected def tooltipData = Seq(Settings.get.cpuComponentSupport(tier))
}

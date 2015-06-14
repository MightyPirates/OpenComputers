package li.cil.oc.common.item

import li.cil.oc.util.Rarity
import net.minecraft.item.ItemStack

class Microchip(val parent: Delegator, val tier: Int) extends traits.Delegate {
  override val unlocalizedName = super.unlocalizedName + tier

  override protected def tooltipName = Option(super.unlocalizedName)

  override def rarity(stack: ItemStack) = Rarity.byTier(tier)
}

package li.cil.oc.common.item

import li.cil.oc.util.Rarity

class Microchip(val parent: Delegator, val tier: Int) extends Delegate {
  override val unlocalizedName = super.unlocalizedName + tier

  override protected def tooltipName = Option(super.unlocalizedName)

  override def rarity = Rarity.byTier(tier)
}

package li.cil.oc.common.item

import li.cil.oc.common.Tier

class WirelessNetworkCard(val parent: Delegator, var tier: Int) extends traits.Delegate with traits.ItemTier {
  override val unlocalizedName = super.unlocalizedName + tier
  
  override protected def tooltipName = Option(super.unlocalizedName)
}

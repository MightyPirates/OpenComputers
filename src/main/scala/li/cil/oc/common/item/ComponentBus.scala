package li.cil.oc.common.item

import li.cil.oc.Settings

class ComponentBus(val parent: Delegator, val tier: Int) extends Delegate with ItemTier {
  override val unlocalizedName = super.unlocalizedName + tier

  override protected def tooltipName = Option(super.unlocalizedName)

  override protected def tooltipData = Seq(Settings.get.cpuComponentSupport(tier))
}

package li.cil.oc.common.item

import li.cil.oc.Settings

class UpgradeRITEG(val parent: Delegator) extends traits.Delegate with traits.ItemTier {
  override protected def tooltipData = Seq((Settings.get.ritegUpgradeEfficiency * 100).toInt)
}

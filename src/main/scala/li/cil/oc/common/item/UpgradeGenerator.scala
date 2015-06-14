package li.cil.oc.common.item

import li.cil.oc.Settings

class UpgradeGenerator(val parent: Delegator) extends traits.Delegate with traits.ItemTier {
  override protected def tooltipData = Seq((Settings.get.generatorEfficiency * 100).toInt)
}

package li.cil.oc.common.item

import li.cil.oc.Settings

class UpgradeSolarGenerator(val parent: Delegator) extends traits.Delegate with traits.ItemTier {
  override protected def tooltipData = Seq((Settings.Power.solarGeneratorEfficiency * 100).toInt)
}

package li.cil.oc.common.item

class UpgradeSkin(val parent: Delegator, val tier: Int) extends traits.Delegate with traits.ItemTier {
  override val unlocalizedName = super.unlocalizedName + tier

  override protected def tooltipName = Option(unlocalizedName)

  override protected def tooltipData = Seq(tier + 1)
}

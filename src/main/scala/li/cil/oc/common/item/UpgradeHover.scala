package li.cil.oc.common.item

class UpgradeHover(val parent: Delegator, val heightLimit: Int, val tier: Int) extends traits.Delegate with traits.ItemTier {
  override val unlocalizedName = super.unlocalizedName + tier

  override protected def tooltipName = Option(super.unlocalizedName)

  override protected def tooltipData = Seq(heightLimit)
}

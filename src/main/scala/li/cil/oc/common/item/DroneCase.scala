package li.cil.oc.common.item

class DroneCase(val parent: Delegator, val tier: Int) extends Delegate with ItemTier {
  override val unlocalizedName = super.unlocalizedName + tier

  override protected def tierFromDriver = tier

  override protected def tooltipName = Option(super.unlocalizedName)
}
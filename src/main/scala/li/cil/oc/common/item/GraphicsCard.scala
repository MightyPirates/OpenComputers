package li.cil.oc.common.item

import li.cil.oc.Settings
import li.cil.oc.util.PackedColor

class GraphicsCard(val parent: Delegator, val tier: Int) extends Delegate with ItemTier {
  override val unlocalizedName = super.unlocalizedName + tier

  override protected def tooltipName = Option(super.unlocalizedName)

  override protected def tooltipData = {
    val (w, h) = Settings.screenResolutionsByTier(tier)
    val depth = PackedColor.Depth.bits(Settings.screenDepthsByTier(tier))
    Seq(w, h, depth,
      tier match {
        case 0 => "1/1/4/2/2"
        case 1 => "2/4/8/4/4"
        case 2 => "4/8/16/8/8"
      })
  }
}
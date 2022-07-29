package li.cil.oc.common.item.traits

import li.cil.oc.Settings
import li.cil.oc.util.PackedColor

trait GPULike extends SimpleItem {
  def gpuTier: Int

  override protected def tooltipData: Seq[Any] = {
    val (w, h) = Settings.screenResolutionsByTier(gpuTier)
    val depth = PackedColor.Depth.bits(Settings.screenDepthsByTier(gpuTier))
    Seq(w, h, depth,
      gpuTier match {
        case 0 => "1/1/4/2/2"
        case 1 => "2/4/8/4/4"
        case 2 => "4/8/16/8/8"
      })
  }
}

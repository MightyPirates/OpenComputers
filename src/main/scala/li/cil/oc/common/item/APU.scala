package li.cil.oc.common.item

import scala.language.existentials

class APU(val parent: Delegator, val tier: Int) extends Delegate with traits.ItemTier with traits.CPULike with traits.GPULike {
  override val unlocalizedName = super[Delegate].unlocalizedName + tier

  override def cpuTier = tier + 1

  override def gpuTier = tier

  override protected def tooltipName = Option(super[Delegate].unlocalizedName)

  override protected def tooltipData: Seq[Any] = {
    super[CPULike].tooltipData ++ super[GPULike].tooltipData
  }
}

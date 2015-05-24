package li.cil.oc.common.item

import li.cil.oc.common.Tier

import scala.language.existentials

class APU(val parent: Delegator, val tier: Int) extends traits.Delegate with traits.ItemTier with traits.CPULike with traits.GPULike {
  override val unlocalizedName = super[Delegate].unlocalizedName + tier

  override def cpuTier = math.min(Tier.Three, tier + 1)

  override def gpuTier = tier

  override protected def tooltipName = Option(super[Delegate].unlocalizedName)

  override protected def tooltipData: Seq[Any] = {
    super[CPULike].tooltipData ++ super[GPULike].tooltipData
  }
}

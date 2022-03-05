package li.cil.oc.common.item

import scala.language.existentials

class CPU(val parent: Delegator, val tier: Int) extends traits.Delegate with traits.ItemTier with traits.CPULike {
  override val unlocalizedName = super.unlocalizedName + tier

  override def cpuTier = tier
  override def cpuTierForComponents = tier

  override protected def tooltipName = Option(super.unlocalizedName)
}

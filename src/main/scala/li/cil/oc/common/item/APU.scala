package li.cil.oc.common.item

import li.cil.oc.CreativeTab
import li.cil.oc.common.Tier
import li.cil.oc.util.Rarity
import net.minecraft.item // Rarity
import net.minecraft.item.Item
import net.minecraft.item.Item.Properties
import net.minecraft.item.ItemStack
import net.minecraftforge.common.extensions.IForgeItem

import scala.language.existentials

class APU(val tier: Int, props: Properties = new Properties().tab(CreativeTab)) extends Item(props) with IForgeItem with traits.SimpleItem with traits.ItemTier with traits.CPULike with traits.GPULike {
  @Deprecated
  override def getDescriptionId = super.getDescriptionId + tier

  @Deprecated
  override def getRarity(stack: ItemStack): item.Rarity =
    if (tier == Tier.Three) Rarity.byTier(Tier.Four)
    else super.getRarity(stack)

  override def cpuTier = math.min(Tier.Three, tier + 1)

  override def gpuTier = tier

  override protected def tooltipName = Option(unlocalizedName)

  override protected def tooltipData: Seq[Any] = {
    super[CPULike].tooltipData ++ super[GPULike].tooltipData
  }
}

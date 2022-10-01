package li.cil.oc.common.item

import li.cil.oc.Settings
import li.cil.oc.common.Tier
import li.cil.oc.util.Rarity
import net.minecraft.item // Rarity
import net.minecraft.item.Item
import net.minecraft.item.Item.Properties
import net.minecraft.item.ItemStack
import net.minecraftforge.common.extensions.IForgeItem

class ComponentBus(props: Properties, val tier: Int) extends Item(props) with IForgeItem with traits.SimpleItem with traits.ItemTier {
  @Deprecated
  override def getDescriptionId = super.getDescriptionId + tier

  // Because the driver considers the creative bus to be tier 3, the superclass
  // will believe it has T3 rarity. We override that here.
  @Deprecated
  override def getRarity(stack: ItemStack): item.Rarity =
    if (tier == Tier.Four) Rarity.byTier(Tier.Four)
    else super.getRarity(stack)

  override protected def tooltipName = Option(unlocalizedName)

  override protected def tooltipData = Seq(Settings.get.cpuComponentSupport(tier))
}

package li.cil.oc.common.item

import li.cil.oc.CreativeTab
import li.cil.oc.util.Rarity
import net.minecraft.item.Item
import net.minecraft.item.Item.Properties
import net.minecraft.item.ItemStack
import net.minecraftforge.common.extensions.IForgeItem

class Microchip(val tier: Int, props: Properties = new Properties().tab(CreativeTab)) extends Item(props) with IForgeItem with traits.SimpleItem {
  @Deprecated
  override def getDescriptionId = super.getDescriptionId + tier

  override protected def tooltipName = Option(unlocalizedName)

  @Deprecated
  override def getRarity(stack: ItemStack) = Rarity.byTier(tier)
}

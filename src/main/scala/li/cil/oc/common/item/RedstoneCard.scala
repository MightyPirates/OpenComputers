package li.cil.oc.common.item

import java.util

import li.cil.oc.common.Tier
import li.cil.oc.integration.opencomputers.ModOpenComputers
import net.minecraft.item.Item
import net.minecraft.item.Item.Properties
import net.minecraft.item.ItemGroup
import net.minecraft.item.ItemStack
import net.minecraft.util.NonNullList
import net.minecraftforge.common.extensions.IForgeItem

class RedstoneCard(props: Properties, val tier: Int) extends Item(props) with IForgeItem with traits.SimpleItem with traits.ItemTier {
  @Deprecated
  override def getDescriptionId = super.getDescriptionId + tier

  override protected def tooltipName = Option(unlocalizedName)

  override def fillItemCategory(tab: ItemGroup, list: NonNullList[ItemStack]) {
    if (tier == Tier.One || ModOpenComputers.hasRedstoneCardT2) super.fillItemCategory(tab, list)
  }
}

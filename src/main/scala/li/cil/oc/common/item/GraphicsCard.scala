package li.cil.oc.common.item

import net.minecraft.item.Item
import net.minecraft.item.Item.Properties
import net.minecraftforge.common.extensions.IForgeItem

class GraphicsCard(props: Properties, val tier: Int) extends Item(props) with IForgeItem with traits.SimpleItem with traits.ItemTier with traits.GPULike {
  @Deprecated
  override def getDescriptionId = super.getDescriptionId + tier

  override def gpuTier = tier

  override protected def tooltipName = Option(unlocalizedName)
}
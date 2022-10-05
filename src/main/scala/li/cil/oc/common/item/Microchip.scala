package li.cil.oc.common.item

import net.minecraft.item.Item
import net.minecraft.item.Item.Properties
import net.minecraft.item.ItemStack
import net.minecraftforge.common.extensions.IForgeItem

class Microchip(props: Properties, val tier: Int) extends Item(props) with IForgeItem with traits.SimpleItem {
  @Deprecated
  override def getDescriptionId = super.getDescriptionId + tier

  override protected def tooltipName = Option(unlocalizedName)
}

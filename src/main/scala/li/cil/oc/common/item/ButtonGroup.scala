package li.cil.oc.common.item

import net.minecraft.item.Item
import net.minecraft.item.Item.Properties
import net.minecraftforge.common.extensions.IForgeItem

class ButtonGroup(props: Properties) extends Item(props) with IForgeItem with traits.SimpleItem {
  override protected def tooltipName = None
}

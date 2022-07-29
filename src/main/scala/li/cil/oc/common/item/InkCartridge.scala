package li.cil.oc.common.item

import li.cil.oc.Constants
import li.cil.oc.CreativeTab
import li.cil.oc.api
import net.minecraft.item.Item
import net.minecraft.item.Item.Properties
import net.minecraft.item.ItemStack
import net.minecraftforge.common.extensions.IForgeItem

class InkCartridge(props: Properties = new Properties().tab(CreativeTab).craftRemainder(api.Items.get(Constants.ItemName.InkCartridgeEmpty).item)) extends Item(props) with IForgeItem with traits.SimpleItem {
  override def maxStackSize = 1
}

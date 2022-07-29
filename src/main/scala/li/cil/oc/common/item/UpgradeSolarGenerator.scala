package li.cil.oc.common.item

import li.cil.oc.CreativeTab
import li.cil.oc.Settings
import net.minecraft.item.Item
import net.minecraft.item.Item.Properties
import net.minecraftforge.common.extensions.IForgeItem

class UpgradeSolarGenerator(props: Properties = new Properties().tab(CreativeTab)) extends Item(props) with IForgeItem with traits.SimpleItem with traits.ItemTier {
  override protected def tooltipData = Seq((Settings.get.solarGeneratorEfficiency * 100).toInt)
}

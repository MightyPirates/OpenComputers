package li.cil.oc.common.item

import li.cil.oc.CreativeTab
import net.minecraft.item.Item
import net.minecraft.item.Item.Properties
import net.minecraftforge.common.extensions.IForgeItem

import scala.language.existentials

class CPU(val tier: Int, props: Properties = new Properties().tab(CreativeTab)) extends Item(props) with IForgeItem with traits.SimpleItem with traits.ItemTier with traits.CPULike {
  @Deprecated
  override def getDescriptionId = super.getDescriptionId + tier

  override def cpuTier = tier

  override protected def tooltipName = Option(unlocalizedName)
}

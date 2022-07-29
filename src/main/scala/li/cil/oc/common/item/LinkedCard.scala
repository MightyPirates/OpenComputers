package li.cil.oc.common.item

import java.util

import li.cil.oc.CreativeTab
import li.cil.oc.Settings
import li.cil.oc.util.Tooltip
import net.minecraft.client.util.ITooltipFlag
import net.minecraft.entity.player.PlayerEntity
import net.minecraft.item.Item
import net.minecraft.item.Item.Properties
import net.minecraft.item.ItemStack
import net.minecraft.util.text.ITextComponent
import net.minecraft.util.text.StringTextComponent
import net.minecraft.world.World
import net.minecraftforge.api.distmarker.Dist
import net.minecraftforge.api.distmarker.OnlyIn
import net.minecraftforge.common.extensions.IForgeItem

import scala.collection.convert.ImplicitConversionsToScala._

class LinkedCard(props: Properties = new Properties().tab(CreativeTab)) extends Item(props) with IForgeItem with traits.SimpleItem with traits.ItemTier {
  @OnlyIn(Dist.CLIENT)
  override def appendHoverText(stack: ItemStack, world: World, tooltip: util.List[ITextComponent], flag: ITooltipFlag) {
    super.appendHoverText(stack, world, tooltip, flag)
    if (stack.hasTag && stack.getTag.contains(Settings.namespace + "data")) {
      val data = stack.getTag.getCompound(Settings.namespace + "data")
      if (data.contains(Settings.namespace + "tunnel")) {
        val channel = data.getString(Settings.namespace + "tunnel")
        if (channel.length > 13) {
          for (curr <- Tooltip.get(unlocalizedName + "_channel", channel.substring(0, 13) + "...")) {
            tooltip.add(new StringTextComponent(curr))
          }
        }
        else {
          for (curr <- Tooltip.get(unlocalizedName + "_channel", channel)) {
            tooltip.add(new StringTextComponent(curr))
          }
        }
      }
    }
  }
}

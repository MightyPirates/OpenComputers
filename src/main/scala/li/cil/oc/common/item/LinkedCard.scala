package li.cil.oc.common.item

import java.util

import li.cil.oc.Settings
import li.cil.oc.util.Tooltip
import net.minecraft.client.util.ITooltipFlag
import net.minecraft.entity.player.EntityPlayer
import net.minecraft.item.ItemStack
import net.minecraft.world.World

class LinkedCard(val parent: Delegator) extends traits.Delegate with traits.ItemTier {
  override def tooltipLines(stack: ItemStack, world: World, tooltip: util.List[String], flag: ITooltipFlag) {
    if (stack.hasTagCompound && stack.getTagCompound.hasKey(Settings.namespace + "data")) {
      val data = stack.getTagCompound.getCompoundTag(Settings.namespace + "data")
      if (data.hasKey(Settings.namespace + "tunnel")) {
        val channel = data.getString(Settings.namespace + "tunnel")
        if (channel.length > 13) {
          tooltip.addAll(Tooltip.get(unlocalizedName + "_channel", channel.substring(0, 13) + "..."))
        }
        else {
          tooltip.addAll(Tooltip.get(unlocalizedName + "_channel", channel))
        }
      }
    }
    super.tooltipLines(stack, world, tooltip, flag)
  }
}

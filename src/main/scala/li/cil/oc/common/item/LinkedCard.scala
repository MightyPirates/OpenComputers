package li.cil.oc.common.item

import java.util

import li.cil.oc.Settings
import li.cil.oc.util.Tooltip
import net.minecraft.entity.player.EntityPlayer
import net.minecraft.item.ItemStack

class LinkedCard(val parent: Delegator) extends traits.Delegate with traits.ItemTier {
  override def tooltipLines(stack: ItemStack, player: EntityPlayer, tooltip: util.List[String], advanced: Boolean) {
    if (stack.hasTagCompound && stack.getTagCompound.hasKey(Constants.namespace + "data")) {
      val data = stack.getTagCompound.getCompoundTag(Constants.namespace + "data")
      if (data.hasKey(Constants.namespace + "tunnel")) {
        val channel = data.getString(Constants.namespace + "tunnel")
        if (channel.length > 13) {
          tooltip.addAll(Tooltip.get(unlocalizedName + "_Channel", channel.substring(0, 13) + "..."))
        }
        else {
          tooltip.addAll(Tooltip.get(unlocalizedName + "_Channel", channel))
        }
      }
    }
    super.tooltipLines(stack, player, tooltip, advanced)
  }
}

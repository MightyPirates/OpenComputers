package li.cil.oc.common.item

import java.util

import li.cil.oc.Settings
import li.cil.oc.util.Tooltip
import net.minecraft.client.renderer.texture.IconRegister
import net.minecraft.entity.player.EntityPlayer
import net.minecraft.item.ItemStack

class LinkedCard(val parent: Delegator) extends Delegate {
  val unlocalizedName = "LinkedCard"

  override def tooltipLines(stack: ItemStack, player: EntityPlayer, tooltip: util.List[String], advanced: Boolean) {
    if (stack.hasTagCompound && stack.getTagCompound.hasKey(Settings.namespace + "data")) {
      val data = stack.getTagCompound.getCompoundTag(Settings.namespace + "data")
      if (data.hasKey(Settings.namespace + "tunnel")) {
        val channel = data.getString(Settings.namespace + "tunnel")
        if (channel.length > 13) {
          tooltip.addAll(Tooltip.get(unlocalizedName + "_Channel", channel.substring(0, 13) + "..."))
        }
        else {
          tooltip.addAll(Tooltip.get(unlocalizedName + "_Channel", channel))
        }
      }
    }
    tooltip.addAll(Tooltip.get(unlocalizedName))
    super.tooltipLines(stack, player, tooltip, advanced)
  }

  override def registerIcons(iconRegister: IconRegister) = {
    super.registerIcons(iconRegister)

    icon = iconRegister.registerIcon(Settings.resourceDomain + ":card_linked")
  }
}

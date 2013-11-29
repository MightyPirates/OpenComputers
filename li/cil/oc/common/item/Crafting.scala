package li.cil.oc.common.item

import java.util
import li.cil.oc.Settings
import li.cil.oc.util.Tooltip
import net.minecraft.client.renderer.texture.IconRegister
import net.minecraft.entity.player.EntityPlayer
import net.minecraft.item.ItemStack

class Crafting(val parent: Delegator) extends Delegate {
  val unlocalizedName = "Crafting"

  override def addInformation(stack: ItemStack, player: EntityPlayer, tooltip: util.List[String], advanced: Boolean) {
    tooltip.addAll(Tooltip.get(unlocalizedName))
    super.addInformation(stack, player, tooltip, advanced)
  }

  override def registerIcons(iconRegister: IconRegister) = {
    super.registerIcons(iconRegister)

    icon = iconRegister.registerIcon(Settings.resourceDomain + ":crafting")
  }
}

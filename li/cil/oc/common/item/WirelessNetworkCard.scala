package li.cil.oc.common.item

import java.util
import li.cil.oc.Config
import li.cil.oc.util.Tooltip
import net.minecraft.client.renderer.texture.IconRegister
import net.minecraft.entity.player.EntityPlayer
import net.minecraft.item.ItemStack

class WirelessNetworkCard(val parent: Delegator) extends Delegate {
  val unlocalizedName = "WirelessNetworkCard"

  override def addInformation(item: ItemStack, player: EntityPlayer, tooltip: util.List[String], advanced: Boolean) {
    tooltip.addAll(Tooltip.get(unlocalizedName))
  }

  override def registerIcons(iconRegister: IconRegister) = {
    super.registerIcons(iconRegister)

    icon = iconRegister.registerIcon(Config.resourceDomain + ":wlancard")
  }
}

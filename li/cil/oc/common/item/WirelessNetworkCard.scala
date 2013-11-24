package li.cil.oc.common.item

import li.cil.oc.Config
import net.minecraft.client.renderer.texture.IconRegister
import net.minecraft.item.ItemStack
import net.minecraft.entity.player.EntityPlayer
import java.util
import li.cil.oc.util.Tooltip

class WirelessNetworkCard(val parent: Delegator) extends Delegate {
  val unlocalizedName = "WirelessNetworkCard"

  override def addInformation(item: ItemStack, player: EntityPlayer, tooltip: util.List[String], advanced: Boolean) {
    tooltip.add("Allows sending wireless network messages in addition")
    tooltip.add("to normal ones. Make sure to set the " + Tooltip.format("signal strength", Tooltip.Color.White))
    tooltip.add("or no wireless packet will be sent!")
  }

  override def registerIcons(iconRegister: IconRegister) = {
    super.registerIcons(iconRegister)

    icon = iconRegister.registerIcon(Config.resourceDomain + ":wlancard")
  }
}

package li.cil.oc.common.item

import li.cil.oc.Config
import net.minecraft.client.renderer.texture.IconRegister

class WirelessNetworkCard(val parent: Delegator) extends Delegate {
  val unlocalizedName = "WirelessNetworkCard"

  override def registerIcons(iconRegister: IconRegister) = {
    super.registerIcons(iconRegister)

    icon = iconRegister.registerIcon(Config.resourceDomain + ":wlancard")
  }
}

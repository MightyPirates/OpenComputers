package li.cil.oc.common.item

import li.cil.oc.Config
import net.minecraft.client.renderer.texture.IconRegister

class NetworkCard(val parent: Delegator) extends Delegate {
  def unlocalizedName = "NetworkCard"

  override def registerIcons(iconRegister: IconRegister) = {
    super.registerIcons(iconRegister)

    icon = iconRegister.registerIcon(Config.resourceDomain + ":lancard")
  }
}

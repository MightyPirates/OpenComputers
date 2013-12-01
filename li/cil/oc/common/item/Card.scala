package li.cil.oc.common.item

import li.cil.oc.Settings
import net.minecraft.client.renderer.texture.IconRegister

class Card(val parent: Delegator) extends Delegate {
  val unlocalizedName = "Card"

  override def registerIcons(iconRegister: IconRegister) {
    super.registerIcons(iconRegister)

    icon = iconRegister.registerIcon(Settings.resourceDomain + ":card")
  }
}

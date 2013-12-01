package li.cil.oc.common.item

import li.cil.oc.Settings
import net.minecraft.client.renderer.texture.IconRegister

class Card(val parent: Delegator, val tier: Int) extends Delegate {
  val baseName = "Card"
  val unlocalizedName = baseName + Array("Basic", "Advanced", "Professional").apply(tier)

  override def registerIcons(iconRegister: IconRegister) {
    super.registerIcons(iconRegister)

    icon = iconRegister.registerIcon(Settings.resourceDomain + ":card")
  }
}

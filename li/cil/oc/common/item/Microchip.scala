package li.cil.oc.common.item

import li.cil.oc.Settings
import net.minecraft.client.renderer.texture.IconRegister

class Microchip(val parent: Delegator, val tier: Int) extends Delegate {
  val baseName = "Microchip"
  val unlocalizedName = baseName + tier

  override def registerIcons(iconRegister: IconRegister) = {
    super.registerIcons(iconRegister)

    icon = iconRegister.registerIcon(Settings.resourceDomain + ":microchip" + tier)
  }
}

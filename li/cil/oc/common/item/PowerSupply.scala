package li.cil.oc.common.item

import li.cil.oc.Settings
import net.minecraft.client.renderer.texture.IconRegister

class PowerSupply(val parent: Delegator) extends Delegate {
  val unlocalizedName = "PowerSupply"

  override def registerIcons(iconRegister: IconRegister) = {
    super.registerIcons(iconRegister)

    icon = iconRegister.registerIcon(Settings.resourceDomain + ":psu")
  }
}

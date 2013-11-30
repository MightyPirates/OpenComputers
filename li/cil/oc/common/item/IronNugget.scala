package li.cil.oc.common.item

import li.cil.oc.Settings
import net.minecraft.client.renderer.texture.IconRegister

class IronNugget(val parent: Delegator) extends Delegate {
  val unlocalizedName = "IronNugget"

  override def registerIcons(iconRegister: IconRegister) = {
    super.registerIcons(iconRegister)

    icon = iconRegister.registerIcon(Settings.resourceDomain + ":iron_nugget")
  }
}

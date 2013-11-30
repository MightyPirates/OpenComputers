package li.cil.oc.common.item

import net.minecraft.client.renderer.texture.IconRegister
import li.cil.oc.Settings


class IronNugget (val parent: Delegator) extends Delegate {
  val unlocalizedName = "IronNugget"

  override def registerIcons(iconRegister: IconRegister) = {
    super.registerIcons(iconRegister)

    icon = iconRegister.registerIcon(Settings.resourceDomain + ":iron_nugget" )
  }
}

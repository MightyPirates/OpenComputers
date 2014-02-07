package li.cil.oc.common.item

import li.cil.oc.Settings
import net.minecraft.client.renderer.texture.IIconRegister

class ButtonGroup(val parent: Delegator) extends Delegate {
  val unlocalizedName = "ButtonGroup"

  override def registerIcons(iconRegister: IIconRegister) {
    super.registerIcons(iconRegister)

    icon = iconRegister.registerIcon(Settings.resourceDomain + ":keys_group")
  }
}

package li.cil.oc.common.item

import li.cil.oc.Settings

class ButtonGroup(val parent: Delegator) extends Delegate {
  val unlocalizedName = "ButtonGroup"

  override def registerIcons(iconRegister: IconRegister) {
    super.registerIcons(iconRegister)

    icon = iconRegister.registerIcon(Settings.resourceDomain + ":keys_group")
  }
}

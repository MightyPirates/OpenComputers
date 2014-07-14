package li.cil.oc.common.item

import li.cil.oc.Settings

class ArrowKeys(val parent: Delegator) extends Delegate {
  val unlocalizedName = "ArrowKeys"

  override def registerIcons(iconRegister: IconRegister) {
    super.registerIcons(iconRegister)

    icon = iconRegister.registerIcon(Settings.resourceDomain + ":keys_arrow")
  }
}

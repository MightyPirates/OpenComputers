package li.cil.oc.common.item

import li.cil.oc.Config
import net.minecraft.client.renderer.texture.IconRegister

class Hdd(val parent: Delegator, val megaBytes: Int) extends Delegate {
  def unlocalizedName = "HardDiskDrive" + megaBytes + "m"

  override def registerIcons(iconRegister: IconRegister) {
    super.registerIcons(iconRegister)

    icon = iconRegister.registerIcon(Config.resourceDomain + ":hdd" + megaBytes)
  }
}
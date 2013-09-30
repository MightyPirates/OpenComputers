package li.cil.oc.common.item

import li.cil.oc.Config
import net.minecraft.client.renderer.texture.IconRegister

class Memory(val parent: Delegator, val kiloBytes: Int) extends Delegate {
  def unlocalizedName = "Memory" + kiloBytes + "k"

  override def registerIcons(iconRegister: IconRegister) {
    super.registerIcons(iconRegister)

    icon = iconRegister.registerIcon(Config.resourcePack + ":ram" + kiloBytes)
  }
}

package li.cil.oc.common.item

import li.cil.oc.Settings
import net.minecraft.client.renderer.texture.IconRegister
import net.minecraft.item.ItemStack

class Chip(val parent: Delegator, val tier: Int) extends Delegate {
  val baseName = "Chip"
  val unlocalizedName = baseName + Array("Basic", "Advanced", "Professional").apply(tier)



  override def registerIcons(iconRegister: IconRegister) = {
    super.registerIcons(iconRegister)

    icon = iconRegister.registerIcon(Settings.resourceDomain + ":chipset" + tier)
  }
}

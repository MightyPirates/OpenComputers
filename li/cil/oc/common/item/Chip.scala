package li.cil.oc.common.item

import li.cil.oc.Settings
import net.minecraft.client.renderer.texture.IconRegister
import net.minecraft.item.ItemStack

class Chip(val parent: Delegator, val tier: Int) extends Delegate {
  val unlocalizedName = "Chip"

  override def displayName(stack: ItemStack) = {
    if (tier == 0) {
      Option("Redstone Chip")
    }
    else if (tier == 1) {
      Option("Golden Chip")
    }
    else if (tier == 2) {
      Option("Diamond Chip")
    }
    else {
      Option(unlocalizedName)
    }
  }

  override def registerIcons(iconRegister: IconRegister) = {
    super.registerIcons(iconRegister)

    icon = iconRegister.registerIcon(Settings.resourceDomain + ":chipset" + tier)
  }
}

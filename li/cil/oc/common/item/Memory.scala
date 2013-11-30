package li.cil.oc.common.item

import li.cil.oc.Settings
import net.minecraft.client.renderer.texture.IconRegister
import net.minecraft.item.ItemStack

class Memory(val parent: Delegator, val tier: Int) extends Delegate {
  val unlocalizedName = "Memory"

  val kiloBytes = Settings.get.ramSizes(tier)

  override def displayName(stack: ItemStack) =
    Some(parent.getItemStackDisplayName(stack) + " (%dKB)".format(kiloBytes))

  override def registerIcons(iconRegister: IconRegister) {
    super.registerIcons(iconRegister)

    icon = iconRegister.registerIcon(Settings.resourceDomain + ":ram" + tier)
  }
}

package li.cil.oc.common.item

import java.util
import li.cil.oc.Settings
import li.cil.oc.util.Tooltip
import net.minecraft.client.renderer.texture.IIconRegister
import net.minecraft.entity.player.EntityPlayer
import net.minecraft.item.ItemStack

class Memory(val parent: Delegator, val tier: Int) extends Delegate {
  val baseName = "Memory"
  val unlocalizedName = baseName + tier

  val kiloBytes = Settings.get.ramSizes(tier)

  override def displayName(stack: ItemStack) =
    Some(parent.internalGetItemStackDisplayName(stack) + " (%dKB)".format(kiloBytes))

  override def tooltipLines(stack: ItemStack, player: EntityPlayer, tooltip: util.List[String], advanced: Boolean) {
    tooltip.addAll(Tooltip.get(baseName))
    super.tooltipLines(stack, player, tooltip, advanced)
  }

  override def registerIcons(iconRegister: IIconRegister) {
    super.registerIcons(iconRegister)

    icon = iconRegister.registerIcon(Settings.resourceDomain + ":memory" + tier)
  }
}

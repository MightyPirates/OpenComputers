package li.cil.oc.common.item

import java.util
import li.cil.oc.{Settings, server}
import li.cil.oc.util.{Tooltip, Rarity}
import net.minecraft.entity.player.EntityPlayer
import net.minecraft.item.ItemStack
import net.minecraft.client.renderer.texture.IIconRegister

class UpgradeContainerUpgrade(val parent: Delegator, val tier: Int) extends Delegate {
  val baseName = "UpgradeContainerUpgrade"
  val unlocalizedName = baseName + tier

  override def rarity = Rarity.byTier(server.driver.item.UpgradeContainerUpgrade.tier(createItemStack()))

  override def tooltipLines(stack: ItemStack, player: EntityPlayer, tooltip: util.List[String], advanced: Boolean) {
    tooltip.addAll(Tooltip.get(baseName, tier + 1))
    super.tooltipLines(stack, player, tooltip, advanced)
  }

  override def registerIcons(iconRegister: IIconRegister) = {
    super.registerIcons(iconRegister)

    icon = iconRegister.registerIcon(Settings.resourceDomain + ":container_upgrade" + tier)
  }
}

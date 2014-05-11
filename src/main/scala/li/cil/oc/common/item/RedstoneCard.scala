package li.cil.oc.common.item

import java.util
import li.cil.oc.{server, Settings}
import li.cil.oc.util.mods.Mods
import li.cil.oc.util.{Rarity, Tooltip}
import net.minecraft.client.renderer.texture.IconRegister
import net.minecraft.entity.player.EntityPlayer
import net.minecraft.item.ItemStack

class RedstoneCard(val parent: Delegator) extends Delegate {
  val unlocalizedName = "RedstoneCard"

  override def rarity = Rarity.byTier(server.driver.item.RedstoneCard.tier(createItemStack()))

  override def tooltipLines(stack: ItemStack, player: EntityPlayer, tooltip: util.List[String], advanced: Boolean) {
    tooltip.addAll(Tooltip.get(unlocalizedName))
    if (Mods.RedLogic.isAvailable) {
      tooltip.addAll(Tooltip.get(unlocalizedName + ".RedLogic"))
    }
    if (Mods.MineFactoryReloaded.isAvailable) {
      tooltip.addAll(Tooltip.get(unlocalizedName + ".RedNet"))
    }
    super.tooltipLines(stack, player, tooltip, advanced)
  }

  override def registerIcons(iconRegister: IconRegister) {
    super.registerIcons(iconRegister)

    icon = iconRegister.registerIcon(Settings.resourceDomain + ":card_redstone")
  }
}

package li.cil.oc.common.item

import java.util
import li.cil.oc.{server, Settings}
import li.cil.oc.util.{Rarity, Tooltip}
import net.minecraft.client.renderer.texture.IconRegister
import net.minecraft.entity.player.EntityPlayer
import net.minecraft.item.ItemStack

class UpgradeInventory(val parent: Delegator) extends Delegate {
  val unlocalizedName = "UpgradeInventory"

  override def rarity = Rarity.byTier(server.driver.item.UpgradeInventory.tier(createItemStack()))

  override def tooltipLines(stack: ItemStack, player: EntityPlayer, tooltip: util.List[String], advanced: Boolean) {
    tooltip.addAll(Tooltip.get(unlocalizedName))
    super.tooltipLines(stack, player, tooltip, advanced)
  }

  override def registerIcons(iconRegister: IconRegister) = {
    super.registerIcons(iconRegister)

    icon = iconRegister.registerIcon(Settings.resourceDomain + ":upgrade_inventory")
  }
}

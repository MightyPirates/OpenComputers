package li.cil.oc.common.item

import java.util
import li.cil.oc.{Settings, server}
import li.cil.oc.util.{Tooltip, Rarity}
import net.minecraft.entity.player.EntityPlayer
import net.minecraft.item.ItemStack
import net.minecraft.client.renderer.texture.IconRegister

class UpgradeCapacitor(val parent: Delegator) extends Delegate {
  val unlocalizedName = "UpgradeCapacitor"

  override def rarity = Rarity.byTier(server.driver.item.UpgradeCrafting.tier(createItemStack()))

  override def tooltipLines(stack: ItemStack, player: EntityPlayer, tooltip: util.List[String], advanced: Boolean) {
    tooltip.addAll(Tooltip.get(unlocalizedName))
    super.tooltipLines(stack, player, tooltip, advanced)
  }

  override def isDamageable = true

  override def damage(stack: ItemStack) = {
    val nbt = stack.getTagCompound
    if (nbt != null) {
      val stored = nbt.getCompoundTag(Settings.namespace + "data").getCompoundTag("node").getDouble("buffer")
      ((1 - stored / Settings.get.bufferCapacitorUpgrade) * 100).toInt
    }
    else 100
  }

  override def maxDamage(stack: ItemStack) = 100

  override def registerIcons(iconRegister: IconRegister) = {
    super.registerIcons(iconRegister)

    icon = iconRegister.registerIcon(Settings.resourceDomain + ":upgrade_capacitor")
  }
}

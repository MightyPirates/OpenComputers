package li.cil.oc.common.item

import java.util

import li.cil.oc.Settings
import li.cil.oc.util.Tooltip
import net.minecraft.client.renderer.texture.IconRegister
import net.minecraft.entity.player.EntityPlayer
import net.minecraft.item.ItemStack

class UpgradeBattery(val parent: Delegator, val tier: Int) extends Delegate {
  val baseName = "UpgradeBattery"
  val unlocalizedName = baseName + tier

  override def tooltipLines(stack: ItemStack, player: EntityPlayer, tooltip: util.List[String], advanced: Boolean) {
    tooltip.addAll(Tooltip.get(baseName, Settings.get.bufferCapacitorUpgrades(tier).toInt))
    super.tooltipLines(stack, player, tooltip, advanced)
  }

  override def isDamageable = true

  override def damage(stack: ItemStack) = {
    val nbt = stack.getTagCompound
    if (nbt != null) {
      val stored = nbt.getCompoundTag(Settings.namespace + "data").getCompoundTag("node").getDouble("buffer")
      ((1 - stored / Settings.get.bufferCapacitorUpgrades(tier)) * 100).toInt
    }
    else 100
  }

  override def maxDamage(stack: ItemStack) = 100

  override def registerIcons(iconRegister: IconRegister) = {
    super.registerIcons(iconRegister)

    icon = iconRegister.registerIcon(Settings.resourceDomain + ":upgrade_battery" + tier)
  }
}

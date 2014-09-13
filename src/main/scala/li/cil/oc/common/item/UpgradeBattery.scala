package li.cil.oc.common.item

import li.cil.oc.Settings
import net.minecraft.item.ItemStack

class UpgradeBattery(val parent: Delegator, val tier: Int) extends Delegate with ItemTier {
  override val unlocalizedName = super.unlocalizedName + tier

  override protected def tooltipName = Option(super.unlocalizedName)

  override protected def tooltipData = Seq(Settings.get.bufferCapacitorUpgrades(tier).toInt)

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
}

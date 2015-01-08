package li.cil.oc.common.item

import li.cil.oc.Settings
import net.minecraft.item.ItemStack

class UpgradeBattery(val parent: Delegator, val tier: Int) extends Delegate with ItemTier {
  override val unlocalizedName = super.unlocalizedName + tier

  override protected def tooltipName = Option(super.unlocalizedName)

  override protected def tooltipData = Seq(Settings.get.bufferCapacitorUpgrades(tier).toInt)

  override def showDurabilityBar(stack: ItemStack) = true

  override def durability(stack: ItemStack) = {
    if (stack.hasTagCompound) {
      val stored = stack.getTagCompound.getCompoundTag(Settings.namespace + "data").getCompoundTag("node").getDouble("buffer")
      1 - stored / Settings.get.bufferCapacitorUpgrades(tier)
    }
    else 1.0
  }
}

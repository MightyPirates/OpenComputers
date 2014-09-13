package li.cil.oc.common.item

import li.cil.oc.Settings
import net.minecraft.item.ItemStack

class Memory(val parent: Delegator, val tier: Int) extends Delegate with ItemTier {
  override val unlocalizedName = super.unlocalizedName + tier
  val kiloBytes = Settings.get.ramSizes(tier)

  override protected def tooltipName = Option(super.unlocalizedName)

  override def displayName(stack: ItemStack) =
    Some(parent.getItemStackDisplayName(stack) + " (%dKB)".format(kiloBytes))
}

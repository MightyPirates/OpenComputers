package li.cil.oc.common.item

import net.minecraft.item.ItemStack


class Card(val parent: Delegator, val tier: Int) extends Delegate {
  val baseName = "Card"
  val unlocalizedName = baseName + Array("Basic", "Advanced", "Professional").apply(tier)

}

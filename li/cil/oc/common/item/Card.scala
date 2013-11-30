package li.cil.oc.common.item

import net.minecraft.item.ItemStack

class Card(val parent: Delegator, val tier: Int) extends Delegate {
  val unlocalizedName = "Card"

  override def displayName(stack: ItemStack) = {
    if (tier == 0) {
      Option("Redstone Card")
    }
    else if (tier == 1) {
      Option("Golden Card")
    }
    else if (tier == 2) {
      Option("Diamond Card")
    }
    else {
      Option(unlocalizedName)
    }
  }
}

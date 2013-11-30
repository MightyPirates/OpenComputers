package li.cil.oc.common.item

import net.minecraft.item.ItemStack
import scala.Some


class Chip(val parent: Delegator, val tier: Int) extends Delegate {
  val unlocalizedName = "Chip"

  override def displayName(stack: ItemStack) = {
    if (tier == 0) {
      Option("Redstone Chip")
    }
    else if (tier == 1) {
      Option("Golden Chip")
    }
    else if (tier == 2) {
      Option("Diamond Chip")
    }
    else{
      Option(unlocalizedName)
    }
  }
}

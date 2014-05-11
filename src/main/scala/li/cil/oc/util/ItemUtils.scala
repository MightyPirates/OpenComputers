package li.cil.oc.util

import net.minecraft.item.ItemStack
import li.cil.oc.api
import li.cil.oc.common.InventorySlots.Tier

object ItemUtils {
  def caseTier(stack: ItemStack) = {
    val descriptor = api.Items.get(stack)
    if (descriptor == api.Items.get("case1")) Tier.One
    else if (descriptor == api.Items.get("case2")) Tier.Two
    else if (descriptor == api.Items.get("case3")) Tier.Three
    else Tier.None
  }
}

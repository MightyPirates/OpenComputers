package li.cil.oc.common.item

import li.cil.oc.Constants
import li.cil.oc.api
import net.minecraft.item.Item
import net.minecraft.item.ItemStack

class InkCartridge(val parent: Delegator) extends traits.Delegate {
  override def maxStackSize = 1

  override def getCraftingRemainingItem(): Item = api.Items.get(Constants.ItemName.InkCartridgeEmpty).item

  override def hasCraftingRemainingItem(): Boolean = true
}

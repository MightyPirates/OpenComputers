package li.cil.oc.common.item

import li.cil.oc.api
import net.minecraft.item.ItemStack

class InkCartridge(val parent: Delegator) extends Delegate {
  override def maxStackSize = 1

  override def getContainerItem(stack: ItemStack): ItemStack = {
    if (api.Items.get(stack) == api.Items.get("inkCartridge"))
      api.Items.get("inkCartridgeEmpty").createItemStack(1)
    else
      super.getContainerItem(stack)
  }

  override def hasContainerItem(stack: ItemStack): Boolean = true
}

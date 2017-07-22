package li.cil.oc.integration.magtools

import net.minecraft.item.ItemStack

object ModMagnanimousTools {
  def isMagTool(stack: ItemStack) = stack != null && stack.getItem.getClass.getName.startsWith("com.vapourdrive.magtools.items.tools.")
}

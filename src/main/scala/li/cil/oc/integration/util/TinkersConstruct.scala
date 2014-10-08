package li.cil.oc.integration.util

import net.minecraft.item.ItemStack

object TinkersConstruct {
  def isInfiTool(stack: ItemStack) = stack != null && stack.getItem.getClass.getName.startsWith( """tconstruct.""")
}

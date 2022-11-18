package li.cil.oc.util

import net.minecraft.item.ItemStack
import net.minecraft.nbt.CompoundNBT

/**
  * @author asie, Vexatos
  */
object ItemColorizer {
  /**
    * Return whether the specified armor ItemStack has a color.
    */
  def hasColor(stack: ItemStack): Boolean = stack.hasTag && stack.getTag.contains("display") && stack.getTag.getCompound("display").contains("color")

  /**
    * Return the color for the specified armor ItemStack.
    */
  def getColor(stack: ItemStack): Int = {
    val tag = stack.getTag
    if (tag != null) {
      if (tag.contains("display")) {
        val displayTag = tag.getCompound("display")
        if (displayTag.contains("color")) displayTag.getInt("color") else -1
      }
      else -1
    }
    else -1
  }

  def removeColor(stack: ItemStack) {
    val tag = stack.getTag
    if (tag != null) {
      val displayTag = tag.getCompound("display")
      if (displayTag.contains("color")) displayTag.remove("color")
      if (displayTag.isEmpty) tag.remove("display")
      if (tag.isEmpty) stack.setTag(null)
    }
  }

  def setColor(stack: ItemStack, color: Int) {
    stack.getOrCreateTagElement("display").putInt("color", color)
  }
}

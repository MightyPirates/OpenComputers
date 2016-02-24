package li.cil.oc.util

import net.minecraft.item.ItemStack
import net.minecraft.nbt.NBTTagCompound

/**
  * @author asie, Vexatos
  */
object ItemColorizer {
  /**
    * Return whether the specified armor ItemStack has a color.
    */
  def hasColor(stack: ItemStack): Boolean = stack.hasTagCompound && stack.getTagCompound.hasKey("display") && stack.getTagCompound.getCompoundTag("display").hasKey("color")

  /**
    * Return the color for the specified armor ItemStack.
    */
  def getColor(stack: ItemStack): Int = {
    val tag = stack.getTagCompound
    if (tag != null) {
      val displayTag = tag.getCompoundTag("display")
      if (displayTag == null) -1 else if (displayTag.hasKey("color")) displayTag.getInteger("color") else -1
    }
    else -1
  }

  def removeColor(stack: ItemStack) {
    val tag = stack.getTagCompound
    if (tag != null) {
      val displayTag = tag.getCompoundTag("display")
      if (displayTag.hasKey("color")) displayTag.removeTag("color")
    }
  }

  def setColor(stack: ItemStack, color: Int) {
    var tag = stack.getTagCompound
    if (tag == null) {
      tag = new NBTTagCompound
      stack.setTagCompound(tag)
    }
    val displayTag = tag.getCompoundTag("display")
    if (!tag.hasKey("display")) {
      tag.setTag("display", displayTag)
    }
    displayTag.setInteger("color", color)
  }
}

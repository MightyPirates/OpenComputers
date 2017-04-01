package li.cil.oc.integration.util

import net.minecraft.client.gui.inventory.GuiContainer
import net.minecraft.item.ItemStack

import scala.collection.mutable

object ItemSearch {

  val focusedInput = mutable.Set.empty[() => Boolean]
  val stackFocusing = mutable.Set.empty[(GuiContainer, Int, Int) => Option[ItemStack]]

  def isInputFocused: Boolean = {
    for (f <- focusedInput) {
      if (f()) return true
    }
    false
  }

  def hoveredStack(container: GuiContainer, mouseX: Int, mouseY: Int): Option[ItemStack] = {
    for (f <- stackFocusing) {
      f(container, mouseX, mouseY).foreach(stack => return Option(stack))
    }
    None
  }
}

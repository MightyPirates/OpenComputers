package li.cil.oc.integration.util

import li.cil.oc.util.StackOption
import li.cil.oc.util.StackOption._
import net.minecraft.client.gui.inventory.GuiContainer

import scala.collection.mutable

object ItemSearch {

  val focusedInput = mutable.Set.empty[() => Boolean]
  val stackFocusing = mutable.Set.empty[(GuiContainer, Int, Int) => StackOption]

  def isInputFocused: Boolean = {
    for (f <- focusedInput) {
      if (f()) return true
    }
    false
  }

  def hoveredStack(container: GuiContainer, mouseX: Int, mouseY: Int): StackOption = {
    for (f <- stackFocusing) {
      f(container, mouseX, mouseY).foreach(stack => return StackOption(stack))
    }
    EmptyStack
  }
}

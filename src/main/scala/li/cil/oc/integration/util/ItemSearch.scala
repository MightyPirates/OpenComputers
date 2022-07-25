package li.cil.oc.integration.util

import li.cil.oc.util.StackOption
import li.cil.oc.util.StackOption._
import net.minecraft.client.gui.screen.inventory.ContainerScreen

import scala.collection.mutable

object ItemSearch {

  val focusedInput = mutable.Set.empty[() => Boolean]
  val stackFocusing = mutable.Set.empty[(ContainerScreen[_], Int, Int) => StackOption]

  def isInputFocused: Boolean = {
    for (f <- focusedInput) {
      if (f()) return true
    }
    false
  }

  def hoveredStack(container: ContainerScreen[_], mouseX: Int, mouseY: Int): StackOption = {
    for (f <- stackFocusing) {
      f(container, mouseX, mouseY).foreach(stack => return StackOption(stack))
    }
    EmptyStack
  }
}

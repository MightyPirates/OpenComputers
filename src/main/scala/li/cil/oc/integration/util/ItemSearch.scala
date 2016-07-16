package li.cil.oc.integration.util

/* TODO NEI
import codechicken.nei.LayoutManager
*/

import li.cil.oc.integration.Mods
import net.minecraft.client.gui.inventory.GuiContainer
import net.minecraft.item.ItemStack
import net.minecraftforge.fml.common.Optional

import scala.collection.mutable

object ItemSearch {

  val focusedInput = mutable.Set.empty[() => Boolean]
  val stackFocusing = mutable.Set.empty[(GuiContainer, Int, Int) => Option[ItemStack]]

  def isInputFocused: Boolean = {
    if (Mods.NotEnoughItems.isAvailable) {
      initNEI()
    }
    for (f <- focusedInput) {
      if (f()) return true
    }
    false
  }

  def hoveredStack(container: GuiContainer, mouseX: Int, mouseY: Int): Option[ItemStack] = {
    if (Mods.NotEnoughItems.isAvailable) {
      initNEI()
    }
    for (f <- stackFocusing) {
      f(container, mouseX, mouseY).foreach(stack => return Option(stack))
    }
    None
  }

  // NEI

  private var neiHoveredStack: (GuiContainer, Int, Int) => Option[ItemStack] = null
  private var neiInputFocused: () => Boolean = null

  @Optional.Method(modid = Mods.IDs.NotEnoughItems)
  private def initNEI() {
    if (neiInputFocused == null) {
      neiInputFocused = () => try isInputFocused0 catch {
        case _: Throwable => false
      }
      focusedInput += neiInputFocused
    }
    if (neiHoveredStack == null) {
      neiHoveredStack = (container, mouseX, mouseY) =>
        try Option(hoveredStack0(container, mouseX, mouseY)) catch {
          case t: Throwable => None
        }
      stackFocusing += neiHoveredStack
    }
  }

  @Optional.Method(modid = Mods.IDs.NotEnoughItems)
  private def isInputFocused0 = false // TODO NEI LayoutManager.getInputFocused != null

  @Optional.Method(modid = Mods.IDs.NotEnoughItems)
  private def hoveredStack0(container: GuiContainer, mouseX: Int, mouseY: Int) = null: ItemStack // TODO NEI LayoutManager.instance.getStackUnderMouse(container, mouseX, mouseY)
}

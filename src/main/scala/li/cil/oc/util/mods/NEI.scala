package li.cil.oc.util.mods

import codechicken.nei.LayoutManager
import net.minecraft.client.gui.inventory.GuiContainer
import net.minecraft.item.ItemStack

object NEI {
  def isInputFocused = Mods.NotEnoughItems.isAvailable && (try isInputFocused0 catch {
    case _: Throwable => false
  })

  private def isInputFocused0 = LayoutManager.getInputFocused != null

  def hoveredStack(container: GuiContainer, mouseX: Int, mouseY: Int): Option[ItemStack] =
    if (Mods.NotEnoughItems.isAvailable) try Option(hoveredStack0(container, mouseX, mouseY)) catch {
      case t: Throwable => None
    }
    else None

  private def hoveredStack0(container: GuiContainer, mouseX: Int, mouseY: Int) = LayoutManager.instance.getStackUnderMouse(container, mouseX, mouseY)
}

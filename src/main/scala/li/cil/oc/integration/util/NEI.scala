package li.cil.oc.integration.util

import codechicken.nei.LayoutManager
import li.cil.oc.common.item.traits.Delegate
import li.cil.oc.integration.Mods
import net.minecraft.block.Block
import net.minecraft.client.gui.inventory.GuiContainer
import net.minecraft.item.ItemStack

import scala.collection.mutable

object NEI {
  // Lazily evaluated stacks to avoid creating stacks with unregistered items/blocks.
  val hiddenItems = mutable.Set.empty[() => ItemStack]

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

  def hide(block: Block): Unit = if (Mods.NotEnoughItems.isAvailable) hiddenItems += (() => new ItemStack(block))

  def hide(item: Delegate): Unit = if (Mods.NotEnoughItems.isAvailable) hiddenItems += (() => item.createItemStack())
}

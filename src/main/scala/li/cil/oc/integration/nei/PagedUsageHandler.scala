package li.cil.oc.integration.nei

import java.util

import codechicken.lib.gui.GuiDraw
import codechicken.nei.api.stack.PositionedStack
import codechicken.nei.recipe.GuiRecipe
import codechicken.nei.recipe.IUsageHandler
import net.minecraft.client.gui.inventory.GuiContainer
import net.minecraft.inventory.Container
import net.minecraft.item.ItemStack

abstract class PagedUsageHandler(val pages: Option[Array[String]]) extends IUsageHandler {
  protected def wrap(line: String, width: Int) = GuiDraw.fontRenderer.listFormattedStringToWidth(line, width)

  override def recipiesPerPage = 1

  override def numRecipes = pages.fold(0)(_.length)

  override def drawForeground(recipe: Int) {
    pages match {
      case Some(data) =>
        for ((text, line) <- data(recipe).lines.zipWithIndex) {
          GuiDraw.drawString(text, 4, 4 + line * 10, 0x333333, false)
        }
      case _ =>
    }
  }

  override def drawBackground(recipe: Int) {}

  override def getIngredientStacks(recipe: Int) = new util.ArrayList[PositionedStack]()

  override def getOtherStacks(recipe: Int) = new util.ArrayList[PositionedStack]()

  override def getResultStack(recipe: Int) = null

  override def onUpdate() {}

  override def hasOverlay(gui: GuiContainer, container: Container, recipe: Int) = false

  override def getOverlayHandler(gui: GuiContainer, recipe: Int) = null

  override def getOverlayRenderer(gui: GuiContainer, recipe: Int) = null

  override def handleTooltip(gui: GuiRecipe, tooltip: util.List[String], recipe: Int) = tooltip

  override def handleItemTooltip(gui: GuiRecipe, stack: ItemStack, tooltip: util.List[String], recipe: Int) = tooltip

  override def keyTyped(gui: GuiRecipe, char: Char, code: Int, recipe: Int) = false

  override def mouseClicked(gui: GuiRecipe, x: Int, y: Int) = false
}

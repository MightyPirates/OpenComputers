package li.cil.oc.integration.nei

import java.util

import codechicken.lib.gui.GuiDraw
import codechicken.nei.api.IOverlayHandler
import codechicken.nei.api.IRecipeOverlayRenderer
import codechicken.nei.api.stack.PositionedStack
import codechicken.nei.recipe.GuiRecipe
import codechicken.nei.recipe.IUsageHandler
import li.cil.oc.Localization
import li.cil.oc.api
import net.minecraft.client.Minecraft
import net.minecraft.client.gui.GuiButton
import net.minecraft.client.gui.inventory.GuiContainer
import net.minecraft.inventory.Container
import net.minecraft.item.ItemStack

class ManualUsageHandler(path: Option[String]) extends IUsageHandler {
  def this() = this(None)

  var lastMouseX = 0
  var lastMouseY = 0
  val button = new GuiButton(0, 34, 20, 100, 20, Localization.localizeImmediately("nei.usage.oc.Manual"))

  override def getRecipeName = "Manual"

  override def getUsageHandler(input: String, ingredients: AnyRef*): IUsageHandler = {
    if (input == "item") {
      ingredients.collectFirst {
        case stack: ItemStack if api.Manual.pathFor(stack) != null =>
          new ManualUsageHandler(Option(api.Manual.pathFor(stack)))
      }.getOrElse(this)
    }
    else this
  }

  override def recipiesPerPage = 1

  override def numRecipes = if (path.isDefined) 1 else 0

  override def drawForeground(recipe: Int): Unit = if (path.isDefined) Minecraft.getMinecraft.currentScreen match {
    case container: GuiContainer =>
      val pos = GuiDraw.getMousePosition
      button.drawButton(Minecraft.getMinecraft, pos.x - container.guiLeft - 5, pos.y - container.guiTop - 16)
    case _ =>
  }

  override def drawBackground(i: Int): Unit = {}

  override def getIngredientStacks(i: Int) = new util.ArrayList[PositionedStack]()

  override def getOtherStacks(i: Int) = new util.ArrayList[PositionedStack]()

  override def getResultStack(i: Int) = null

  override def onUpdate(): Unit = {}

  override def hasOverlay(gui: GuiContainer, container: Container, i: Int): Boolean = false

  override def getOverlayHandler(gui: GuiContainer, i: Int): IOverlayHandler = null

  override def getOverlayRenderer(gui: GuiContainer, i: Int): IRecipeOverlayRenderer = null

  override def handleTooltip(gui: GuiRecipe, tooltip: util.List[String], i: Int): util.List[String] = tooltip

  override def handleItemTooltip(gui: GuiRecipe, stack: ItemStack, tooltip: util.List[String], i: Int): util.List[String] = tooltip

  override def keyTyped(gui: GuiRecipe, char: Char, code: Int, recipe: Int): Boolean = false

  override def mouseClicked(container: GuiRecipe, btn: Int, recipe: Int): Boolean = path.isDefined && (container match {
    case container: GuiContainer =>
      val pos = GuiDraw.getMousePosition
      val mc = Minecraft.getMinecraft
      if (button.mousePressed(mc, pos.x - container.guiLeft - 5, pos.y - container.guiTop - 16)) {
        mc.thePlayer.closeScreen()
        api.Manual.openFor(mc.thePlayer)
        path.foreach(api.Manual.navigate)
        true
      }
      else false
    case _ => false
  })
}

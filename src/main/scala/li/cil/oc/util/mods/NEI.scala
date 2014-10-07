package li.cil.oc.util.mods

import java.util

import codechicken.lib.gui.GuiDraw
import codechicken.nei.LayoutManager
import codechicken.nei.PositionedStack
import codechicken.nei.api.API
import codechicken.nei.api.IConfigureNEI
import codechicken.nei.recipe.GuiRecipe
import codechicken.nei.recipe.IUsageHandler
import com.google.common.base.Strings
import cpw.mods.fml.relauncher.Side
import cpw.mods.fml.relauncher.SideOnly
import li.cil.oc.api.driver.EnvironmentAware
import li.cil.oc.server.driver.Registry
import li.cil.oc.server.machine.Callbacks
import net.minecraft.client.gui.inventory.GuiContainer
import net.minecraft.inventory.Container
import net.minecraft.item.ItemBlock
import net.minecraft.item.ItemStack
import net.minecraft.util.EnumChatFormatting

import scala.collection.convert.WrapAsScala._

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

@SideOnly(Side.CLIENT)
class NEIOpenComputersConfig extends IConfigureNEI {
  override def getName = "OpenComputers"

  override def getVersion = "1.0.0"

  override def loadConfig() {
    API.registerUsageHandler(new DocumentationHandler())
  }
}

class DocumentationHandler(val pages: Option[Array[String]]) extends IUsageHandler {
  def this() = this(None)

  private val DocPattern = """^function(\([^)]*\)[^-]*) -- (.*)$""".r

  private val VexPattern = """^function(\([^)]*\)[^-]*); (.*)$""".r

  private def wrap(line: String, width: Int) = GuiDraw.fontRenderer.listFormattedStringToWidth(line, width)

  override def getUsageHandler(input: String, ingredients: AnyRef*): IUsageHandler = {
    if (input == "item") {
      ingredients.collect {
        case stack: ItemStack if stack.getItem != null =>
          def getCallbacks(env: Class[_]) = if (env != null) {
            Callbacks.fromClass(env).map {
              case (name, callback) =>
                val doc = callback.annotation.doc
                if (Strings.isNullOrEmpty(doc)) name
                else {
                  val (signature, documentation) = doc match {
                    case DocPattern(head, tail) => (name + head, tail)
                    case VexPattern(head, tail) => (name + head, tail)
                    case _ => (name, doc)
                  }
                  wrap(signature, 160).map(EnumChatFormatting.BLACK.toString + _).mkString("\n") +
                    EnumChatFormatting.RESET + "\n" +
                    wrap(documentation, 152).map("  " + _).mkString("\n")
                }
            }
          }
          else Seq.empty

          val callbacks = Option(Registry.driverFor(stack)) match {
            case Some(driver: EnvironmentAware) =>
              getCallbacks(driver.providedEnvironment(stack))
            case _ => stack.getItem match {
              case block: ItemBlock =>
                Registry.blocks.collect {
                  case driver: EnvironmentAware => driver.providedEnvironment(stack)
                }.filter(_ != null).map(getCallbacks).flatten
              case _ => Seq.empty // No driver for this item.
            }
          }

          if (callbacks.size > 0) {
            val fullDocumentation = callbacks.toArray.sorted.mkString("\n\n")
            val pages = fullDocumentation.lines.grouped(12).map(_.mkString("\n")).toArray
            return new DocumentationHandler(Option(pages))
          }
      }
    }
    this
  }

  override def getRecipeName = "OpenComputers"

  override def recipiesPerPage = 1

  override def numRecipes = pages.fold(0)(_.length)

  override def drawBackground(recipe: Int) {}

  override def drawForeground(recipe: Int) {
    pages match {
      case Some(data) =>
        for ((text, line) <- data(recipe).lines.zipWithIndex) {
          GuiDraw.drawString(text, 4, 4 + line * 10, 0x333333, false)
        }
      case _ =>
    }
  }

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
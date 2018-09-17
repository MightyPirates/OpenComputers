package li.cil.oc.integration.jei

import java.util

import javax.annotation.Nonnull
import com.google.common.base.Strings
import li.cil.oc.OpenComputers
import li.cil.oc.Settings
import li.cil.oc.api
import li.cil.oc.server.machine.Callbacks
import mezz.jei.api.IGuiHelper
import mezz.jei.api.IModRegistry
import mezz.jei.api.gui.IDrawable
import mezz.jei.api.gui.IRecipeLayout
import mezz.jei.api.ingredients.IIngredients
import mezz.jei.api.recipe._
import net.minecraft.client.Minecraft
import net.minecraft.item.ItemStack
import net.minecraft.util.ResourceLocation
import net.minecraft.util.text.TextFormatting

import scala.collection.convert.WrapAsJava._
import scala.collection.convert.WrapAsScala._
import scala.collection.mutable

object CallbackDocHandler {

  private val DocPattern = """(?s)^function(\(.*?\).*?) -- (.*)$""".r

  private val VexPattern = """(?s)^function(\(.*?\).*?); (.*)$""".r

  def getRecipes(registry: IModRegistry): util.List[CallbackDocRecipe] = registry.getIngredientRegistry.getIngredients(classOf[ItemStack]).collect {
    case stack: ItemStack =>
      val callbacks = api.Driver.environmentsFor(stack).flatMap(getCallbacks).toBuffer

      if (callbacks.nonEmpty) {
        val pages = mutable.Buffer.empty[String]
        val lastPage = callbacks.toArray.sorted.foldLeft("") {
          (last, doc) =>
            if (last.lines.length + 2 + doc.lines.length > 12) {
              // We've potentially got some pretty long documentation here, split it up first
              last.lines.grouped(12).map(_.mkString("\n")).foreach(pages += _)
              doc
            }
            else if (last.nonEmpty) last + "\n\n" + doc
            else doc
        }
        // The last page may be too long as well.
        lastPage.lines.grouped(12).map(_.mkString("\n")).foreach(pages += _)

        Option(pages.map(page => new CallbackDocRecipe(stack, page)))
      }
      else None
  }.flatten.flatten.toList

  private def getCallbacks(env: Class[_]) = if (env != null) {

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
          wrap(signature, 160).map(TextFormatting.BLACK.toString + _).mkString("\n") +
            TextFormatting.RESET + "\n" +
            wrap(documentation, 152).map("  " + _).mkString("\n")
        }
    }
  }
  else Seq.empty

  protected def wrap(line: String, width: Int): util.List[String] = Minecraft.getMinecraft.fontRenderer.listFormattedStringToWidth(line, width)

  object CallbackDocRecipeHandler extends IRecipeWrapperFactory[CallbackDocRecipe] {
    override def getRecipeWrapper(recipe: CallbackDocRecipe): CallbackDocRecipe = recipe
  }

  class CallbackDocRecipe(val stack: ItemStack, val page: String) extends BlankRecipeWrapper {

    override def getIngredients(ingredients: IIngredients): Unit = ingredients.setInputs(classOf[ItemStack], List(stack))

    override def drawInfo(@Nonnull minecraft: Minecraft, recipeWidth: Int, recipeHeight: Int, mouseX: Int, mouseY: Int): Unit = {
      for ((text, line) <- page.lines.zipWithIndex) {
        minecraft.fontRenderer.drawString(text, 4, 4 + line * (minecraft.fontRenderer.FONT_HEIGHT + 1), 0x333333, false)
      }
    }
  }

  object CallbackDocRecipeCategory extends IRecipeCategory[CallbackDocRecipe] {
    val recipeWidth: Int = 160
    val recipeHeight: Int = 125
    private var background: IDrawable = _
    private var icon: IDrawable = _

    def initialize(guiHelper: IGuiHelper) {
      background = guiHelper.createBlankDrawable(recipeWidth, recipeHeight)
      icon = new DrawableAnimatedIcon(new ResourceLocation(Settings.resourceDomain, "textures/items/tablet_on.png"), 0, 0, 16, 16, 16, 32,
        guiHelper.createTickTimer(20, 1, true), 0, 16)
    }

    override def getIcon: IDrawable = icon

    override def getBackground: IDrawable = background

    override def setRecipe(recipeLayout: IRecipeLayout, recipeWrapper: CallbackDocRecipe, ingredients: IIngredients) {
    }

    override def getTitle = "OpenComputers API"

    override def getUid = "oc.api"

    override def getModName: String = OpenComputers.Name
  }

}

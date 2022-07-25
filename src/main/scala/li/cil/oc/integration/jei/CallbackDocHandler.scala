package li.cil.oc.integration.jei

import java.util

import com.google.common.base.Strings
import com.mojang.blaze3d.matrix.MatrixStack
import li.cil.oc.OpenComputers
import li.cil.oc.Settings
import li.cil.oc.api
import li.cil.oc.server.machine.Callbacks
import mezz.jei.api.constants.VanillaTypes
import mezz.jei.api.gui.IRecipeLayout
import mezz.jei.api.gui.drawable.IDrawable
import mezz.jei.api.gui.drawable.IDrawableAnimated.StartDirection
import mezz.jei.api.helpers.IGuiHelper
import mezz.jei.api.ingredients.IIngredients
import mezz.jei.api.recipe.category.IRecipeCategory
import mezz.jei.api.registration.IRecipeRegistration
import net.minecraft.client.Minecraft
import net.minecraft.item.ItemStack
import net.minecraft.util.ICharacterConsumer
import net.minecraft.util.ResourceLocation
import net.minecraft.util.text.CharacterManager.ISliceAcceptor
import net.minecraft.util.text.Style
import net.minecraft.util.text.TextFormatting

import scala.collection.convert.WrapAsJava._
import scala.collection.convert.WrapAsScala._
import scala.collection.mutable

object CallbackDocHandler {

  private val DocPattern = """(?s)^function(\(.*?\).*?) -- (.*)$""".r

  private val VexPattern = """(?s)^function(\(.*?\).*?); (.*)$""".r

  def getRecipes(registration: IRecipeRegistration): util.List[CallbackDocRecipe] = registration.getIngredientManager.getAllIngredients(VanillaTypes.ITEM).collect {
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

  protected def wrap(line: String, width: Int): util.List[String] = {
    val list = new util.ArrayList[String]
    Minecraft.getInstance.font.getSplitter.splitLines(line, width, Style.EMPTY, true, new ISliceAcceptor {
      override def accept(style: Style, start: Int, end: Int) = list.add(line.substring(start, end))
    })
    list
  }

  class CallbackDocRecipe(val stack: ItemStack, val page: String)

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

    override def getRecipeClass = classOf[CallbackDocRecipe]

    override def getIcon: IDrawable = icon

    override def getBackground: IDrawable = background

    override def setIngredients(recipeWrapper: CallbackDocRecipe, ingredients: IIngredients) {
      ingredients.setInput(VanillaTypes.ITEM, recipeWrapper.stack)
    }

    override def setRecipe(recipeLayout: IRecipeLayout, recipeWrapper: CallbackDocRecipe, ingredients: IIngredients) {
    }

    override def draw(recipeWrapper: CallbackDocRecipe, stack: MatrixStack, mouseX: Double, mouseY: Double): Unit = {
      val minecraft = Minecraft.getInstance
      for ((text, line) <- recipeWrapper.page.lines.zipWithIndex) {
        minecraft.font.drawShadow(stack, text, 4, 4 + line * (minecraft.font.lineHeight + 1), 0x333333, false)
      }
    }

    @Deprecated
    override def getTitle = "OpenComputers API"

    override def getUid = new ResourceLocation(OpenComputers.ID, "api")
  }

}

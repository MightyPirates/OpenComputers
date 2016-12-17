package li.cil.oc.integration.jei

import java.util
import javax.annotation.Nonnull

import com.google.common.base.Strings
import com.mojang.realmsclient.gui.ChatFormatting
import li.cil.oc.api
import li.cil.oc.api.driver.EnvironmentAware
import li.cil.oc.api.prefab.DriverTileEntity
import li.cil.oc.server.driver.Registry
import li.cil.oc.server.machine.Callbacks
import mezz.jei.api.IGuiHelper
import mezz.jei.api.IModRegistry
import mezz.jei.api.gui.IDrawable
import mezz.jei.api.gui.IRecipeLayout
import mezz.jei.api.ingredients.IIngredients
import mezz.jei.api.recipe.BlankRecipeCategory
import mezz.jei.api.recipe.BlankRecipeWrapper
import mezz.jei.api.recipe.IRecipeHandler
import net.minecraft.client.Minecraft
import net.minecraft.item.ItemStack

import scala.collection.convert.WrapAsJava._
import scala.collection.convert.WrapAsScala._
import scala.collection.mutable

object CallbackDocHandler {

  private val DocPattern = """(?s)^function(\(.*?\).*?) -- (.*)$""".r

  private val VexPattern = """(?s)^function(\(.*?\).*?); (.*)$""".r

  def getRecipes(registry: IModRegistry): util.List[_] = registry.getItemRegistry.getItemList.collect {
    case stack: ItemStack =>
      val callbacks = api.Driver.environmentsFor(stack).flatMap(getCallbacks).toBuffer

      // TODO remove in OC 1.7
      if (callbacks.isEmpty) {
        callbacks ++= (Option(Registry.driverFor(stack)) match {
          case Some(driver: EnvironmentAware) =>
            getCallbacks(driver.providedEnvironment(stack))
          case _ => Registry.blocks.collect {
            case driver: DriverTileEntity with EnvironmentAware =>
              if (driver.getTileEntityClass != null && !driver.getTileEntityClass.isInterface)
                driver.providedEnvironment(stack)
              else null
            case driver: EnvironmentAware => driver.providedEnvironment(stack)
          }.filter(_ != null).flatMap(getCallbacks)
        })
      }

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
  }.collect {
    case Some(handler) => handler
  }.flatten.toList

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
          wrap(signature, 160).map(ChatFormatting.BLACK.toString + _).mkString("\n") +
            ChatFormatting.RESET + "\n" +
            wrap(documentation, 152).map("  " + _).mkString("\n")
        }
    }
  }
  else Seq.empty

  protected def wrap(line: String, width: Int) = Minecraft.getMinecraft.fontRendererObj.listFormattedStringToWidth(line, width)

  object CallbackDocRecipeHandler extends IRecipeHandler[CallbackDocRecipe] {
    override def getRecipeWrapper(recipe: CallbackDocRecipe) = recipe

    override def getRecipeCategoryUid = CallbackDocRecipeCategory.getUid

    override def getRecipeCategoryUid(recipe: CallbackDocRecipe): String = getRecipeCategoryUid

    override def isRecipeValid(recipe: CallbackDocRecipe) = true

    override def getRecipeClass = classOf[CallbackDocRecipe]
  }

  class CallbackDocRecipe(val stack: ItemStack, val page: String) extends BlankRecipeWrapper {

    override def getInputs: util.List[ItemStack] = List(stack)

    override def getIngredients(ingredients: IIngredients): Unit = ingredients.setInputs(classOf[ItemStack], getInputs)

    override def drawInfo(@Nonnull minecraft: Minecraft, recipeWidth: Int, recipeHeight: Int, mouseX: Int, mouseY: Int): Unit = {
      for ((text, line) <- page.lines.zipWithIndex) {
        minecraft.fontRendererObj.drawString(text, 4, 4 + line * (minecraft.fontRendererObj.FONT_HEIGHT + 1), 0x333333, false)
      }
    }
  }

  object CallbackDocRecipeCategory extends BlankRecipeCategory[CallbackDocRecipe] {
    val recipeWidth: Int = 160
    val recipeHeight: Int = 125
    private var background: IDrawable = _

    def initialize(guiHelper: IGuiHelper) {
      background = guiHelper.createBlankDrawable(recipeWidth, recipeHeight)
    }

    override def getBackground: IDrawable = background

    override def setRecipe(recipeLayout: IRecipeLayout, recipeWrapper: CallbackDocRecipe) {
    }

    override def setRecipe(recipeLayout: IRecipeLayout, recipeWrapper: CallbackDocRecipe, ingredients: IIngredients) {
    }

    override def getTitle = "OpenComputers API"

    override def getUid = "oc.api"
  }

}

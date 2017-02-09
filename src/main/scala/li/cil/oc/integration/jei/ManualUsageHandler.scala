package li.cil.oc.integration.jei

import java.util
import javax.annotation.Nonnull

import li.cil.oc.Localization
import li.cil.oc.api
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
import net.minecraftforge.fml.client.config.GuiButtonExt

import scala.collection.convert.WrapAsJava._
import scala.collection.convert.WrapAsScala._

object ManualUsageHandler {

  def getRecipes(registry: IModRegistry): util.List[_] = registry.getIngredientRegistry.getIngredients(classOf[ItemStack]).collect {
    case stack: ItemStack => api.Manual.pathFor(stack) match {
      case s: String => new ManualUsageRecipe(stack, s)
      case _ =>
    }
  }

  object ManualUsageRecipeHandler extends IRecipeHandler[ManualUsageRecipe] {
    override def getRecipeWrapper(recipe: ManualUsageRecipe) = recipe

    override def getRecipeCategoryUid(recipe: ManualUsageRecipe): String = ManualUsageRecipeCategory.getUid

    override def isRecipeValid(recipe: ManualUsageRecipe) = true

    override def getRecipeClass = classOf[ManualUsageRecipe]
  }

  class ManualUsageRecipe(val stack: ItemStack, val path: String) extends BlankRecipeWrapper {
    lazy val button = new GuiButtonExt(0, (160 - 100) / 2, 10, 100, 20, Localization.localizeImmediately("nei.usage.oc.Manual"))

    override def getIngredients(ingredients: IIngredients): Unit = ingredients.setInputs(classOf[ItemStack], List(stack))

    override def drawInfo(@Nonnull minecraft: Minecraft, recipeWidth: Int, recipeHeight: Int, mouseX: Int, mouseY: Int): Unit = {
      button.displayString = Localization.localizeImmediately("nei.usage.oc.Manual")
      button.xPosition = (recipeWidth - button.width) / 2
      button.yPosition = button.height / 2
      button.drawButton(minecraft, mouseX, mouseY)
    }

    override def handleClick(@Nonnull minecraft: Minecraft, mouseX: Int, mouseY: Int, mouseButton: Int): Boolean = {
      if (button.mousePressed(minecraft, mouseX, mouseY)) {
        minecraft.player.closeScreen()
        api.Manual.openFor(minecraft.player)
        api.Manual.navigate(path)
        true
      }
      else false
    }
  }

  object ManualUsageRecipeCategory extends BlankRecipeCategory[ManualUsageRecipe] {
    val recipeWidth: Int = 160
    val recipeHeight: Int = 125
    private var background: IDrawable = null

    def initialize(guiHelper: IGuiHelper) {
      background = guiHelper.createBlankDrawable(recipeWidth, recipeHeight)
    }

    override def getBackground: IDrawable = background

    override def setRecipe(recipeLayout: IRecipeLayout, recipeWrapper: ManualUsageRecipe, ingredients: IIngredients) {
    }

    override def getTitle = "OpenComputers Manual"

    override def getUid = "oc.manual"
  }

}

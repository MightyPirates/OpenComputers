package li.cil.oc.integration.jei

import java.util
import javax.annotation.Nonnull

import li.cil.oc.Localization
import li.cil.oc.Settings
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
import net.minecraft.util.ResourceLocation
import net.minecraftforge.fml.client.config.GuiButtonExt

import scala.collection.convert.WrapAsJava._
import scala.collection.convert.WrapAsScala._

object ManualUsageHandler {

  def getRecipes(registry: IModRegistry): util.List[ManualUsageRecipe] = registry.getItemRegistry.getItemList.collect {
    case stack: ItemStack => api.Manual.pathFor(stack) match {
      case s: String => Option(new ManualUsageRecipe(stack, s))
      case _ => None
    }
  }.flatten.toList

  object ManualUsageRecipeHandler extends IRecipeHandler[ManualUsageRecipe] {
    override def getRecipeWrapper(recipe: ManualUsageRecipe) = recipe

    override def getRecipeCategoryUid = ManualUsageRecipeCategory.getUid

    override def getRecipeCategoryUid(recipe: ManualUsageRecipe): String = getRecipeCategoryUid

    override def isRecipeValid(recipe: ManualUsageRecipe) = true

    override def getRecipeClass = classOf[ManualUsageRecipe]
  }

  class ManualUsageRecipe(val stack: ItemStack, val path: String) extends BlankRecipeWrapper {
    lazy val button = new GuiButtonExt(0, (160 - 100) / 2, 10, 100, 20, Localization.localizeImmediately("nei.usage.oc.Manual"))

    override def getInputs: util.List[ItemStack] = List(stack)

    override def getIngredients(ingredients: IIngredients): Unit = ingredients.setInputs(classOf[ItemStack], getInputs)

    override def drawInfo(@Nonnull minecraft: Minecraft, recipeWidth: Int, recipeHeight: Int, mouseX: Int, mouseY: Int): Unit = {
      button.displayString = Localization.localizeImmediately("nei.usage.oc.Manual")
      button.xPosition = (recipeWidth - button.width) / 2
      button.yPosition = button.height / 2
      button.drawButton(minecraft, mouseX, mouseY)
    }

    override def handleClick(@Nonnull minecraft: Minecraft, mouseX: Int, mouseY: Int, mouseButton: Int): Boolean = {
      if (button.mousePressed(minecraft, mouseX, mouseY)) {
        minecraft.thePlayer.closeScreen()
        api.Manual.openFor(minecraft.thePlayer)
        api.Manual.navigate(path)
        true
      }
      else false
    }
  }

  object ManualUsageRecipeCategory extends BlankRecipeCategory[ManualUsageRecipe] {
    val recipeWidth: Int = 160
    val recipeHeight: Int = 125
    private var background: IDrawable = _
    private var icon: IDrawable = _

    def initialize(guiHelper: IGuiHelper) {
      background = guiHelper.createBlankDrawable(recipeWidth, recipeHeight)
      icon = new DrawableIcon(new ResourceLocation(Settings.resourceDomain, "textures/items/manual.png"), 0, 0, 16, 16, 16, 16)
    }

    override def getBackground: IDrawable = background

    override def getIcon: IDrawable = icon

    override def setRecipe(recipeLayout: IRecipeLayout, recipeWrapper: ManualUsageRecipe) {
    }

    override def setRecipe(recipeLayout: IRecipeLayout, recipeWrapper: ManualUsageRecipe, ingredients: IIngredients) {
    }

    override def getTitle = "OpenComputers Manual"

    override def getUid = "oc.manual"
  }

}

package li.cil.oc.integration.jei

import java.util

import javax.annotation.Nonnull
import li.cil.oc.Localization
import li.cil.oc.OpenComputers
import li.cil.oc.Settings
import li.cil.oc.api
import mezz.jei.api.IGuiHelper
import mezz.jei.api.IModRegistry
import mezz.jei.api.gui.IDrawable
import mezz.jei.api.gui.IRecipeLayout
import mezz.jei.api.ingredients.IIngredients
import mezz.jei.api.recipe._
import net.minecraft.client.Minecraft
import net.minecraft.item.ItemStack
import net.minecraft.util.ResourceLocation
import net.minecraftforge.fml.client.config.GuiButtonExt

import scala.collection.convert.WrapAsJava._
import scala.collection.convert.WrapAsScala._

object ManualUsageHandler {

  def getRecipes(registry: IModRegistry): util.List[ManualUsageRecipe] = registry.getIngredientRegistry.getIngredients(classOf[ItemStack]).collect {
    case stack: ItemStack => api.Manual.pathFor(stack) match {
      case s: String => Option(new ManualUsageRecipe(stack, s))
      case _ => None
    }
  }.flatten.toList

  object ManualUsageRecipeHandler extends IRecipeWrapperFactory[ManualUsageRecipe] {
    override def getRecipeWrapper(recipe: ManualUsageRecipe): ManualUsageRecipe = recipe
  }

  class ManualUsageRecipe(val stack: ItemStack, val path: String) extends BlankRecipeWrapper {
    lazy val button = new GuiButtonExt(0, (160 - 100) / 2, 10, 100, 20, Localization.localizeImmediately("nei.usage.oc.Manual"))

    override def getIngredients(ingredients: IIngredients): Unit = ingredients.setInputs(classOf[ItemStack], List(stack))

    override def drawInfo(@Nonnull minecraft: Minecraft, recipeWidth: Int, recipeHeight: Int, mouseX: Int, mouseY: Int): Unit = {
      button.displayString = Localization.localizeImmediately("nei.usage.oc.Manual")
      button.x = (recipeWidth - button.width) / 2
      button.y = button.height / 2
      button.drawButton(minecraft, mouseX, mouseY, 1)
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

  object ManualUsageRecipeCategory extends IRecipeCategory[ManualUsageRecipe] {
    val recipeWidth: Int = 160
    val recipeHeight: Int = 125
    private var background: IDrawable = _
    private var icon: IDrawable = _

    def initialize(guiHelper: IGuiHelper) {
      background = guiHelper.createBlankDrawable(recipeWidth, recipeHeight)
      icon = guiHelper.createDrawable(new ResourceLocation(Settings.resourceDomain, "textures/items/manual.png"), 0, 0, 16, 16, 16, 16)
    }

    override def getBackground: IDrawable = background

    override def getIcon: IDrawable = icon

    override def setRecipe(recipeLayout: IRecipeLayout, recipeWrapper: ManualUsageRecipe, ingredients: IIngredients) {
    }

    override def getTitle = "OpenComputers Manual"

    override def getUid = "oc.manual"

    override def getModName: String = OpenComputers.Name
  }

}

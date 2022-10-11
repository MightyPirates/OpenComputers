package li.cil.oc.integration.jei

import java.util

import com.mojang.blaze3d.matrix.MatrixStack
import li.cil.oc.Localization
import li.cil.oc.OpenComputers
import li.cil.oc.Settings
import li.cil.oc.api
import mezz.jei.api.constants.VanillaTypes
import mezz.jei.api.gui.IRecipeLayout
import mezz.jei.api.gui.drawable.IDrawable
import mezz.jei.api.helpers.IGuiHelper
import mezz.jei.api.ingredients.IIngredients
import mezz.jei.api.recipe.category.IRecipeCategory
import mezz.jei.api.registration.IRecipeRegistration
import net.minecraft.client.Minecraft
import net.minecraft.item.ItemStack
import net.minecraft.util.ResourceLocation
import net.minecraft.client.gui.widget.button.Button
import org.lwjgl.glfw.GLFW

import scala.collection.convert.ImplicitConversionsToJava._
import scala.collection.convert.ImplicitConversionsToScala._

object ManualUsageHandler {

  def getRecipes(registration: IRecipeRegistration): util.List[ManualUsageRecipe] = registration.getIngredientManager.getAllIngredients(VanillaTypes.ITEM).collect {
    case stack: ItemStack => api.Manual.pathFor(stack) match {
      case s: String => Option(new ManualUsageRecipe(stack, s))
      case _ => None
    }
  }.flatten.toList

  class ManualUsageRecipe(val stack: ItemStack, val path: String)

  object ManualUsageRecipeCategory extends IRecipeCategory[ManualUsageRecipe] {
    val recipeWidth: Int = 160
    val recipeHeight: Int = 125
    private var background: IDrawable = _
    private var icon: IDrawable = _
    private val button = new Button((160 - 100) / 2, 10, 100, 20, Localization.localizeLater("nei.usage.oc.Manual"), new Button.IPressable {
      override def onPress(b: Button) = Unit
    })

    def initialize(guiHelper: IGuiHelper) {
      background = guiHelper.createBlankDrawable(recipeWidth, recipeHeight)
      icon = guiHelper.drawableBuilder(new ResourceLocation(Settings.resourceDomain, "textures/items/manual.png"), 0, 0, 16, 16).setTextureSize(16, 16).build()
    }

    override def getRecipeClass = classOf[ManualUsageRecipe]

    override def getBackground: IDrawable = background

    override def getIcon: IDrawable = icon

    override def setIngredients(recipeWrapper: ManualUsageRecipe, ingredients: IIngredients) {
      ingredients.setInput(VanillaTypes.ITEM, recipeWrapper.stack)
    }

    override def setRecipe(recipeLayout: IRecipeLayout, recipeWrapper: ManualUsageRecipe, ingredients: IIngredients) {
    }

    override def draw(recipeWrapper: ManualUsageRecipe, stack: MatrixStack, mouseX: Double, mouseY: Double) {
      button.render(stack, mouseX.toInt, mouseY.toInt, 0)
    }

    override def handleClick(recipeWrapper: ManualUsageRecipe, mouseX: Double, mouseY: Double, mouseButton: Int): Boolean = {
      if (mouseButton == GLFW.GLFW_MOUSE_BUTTON_LEFT || button.isMouseOver(mouseX, mouseY)) {
        val minecraft = Minecraft.getInstance
        minecraft.player.closeContainer()
        api.Manual.openFor(minecraft.player)
        api.Manual.navigate(recipeWrapper.path)
        true
      }
      else false
    }

    @Deprecated
    override def getTitle = "OpenComputers Manual"

    override def getUid = new ResourceLocation(OpenComputers.ID, "manual")
  }

}

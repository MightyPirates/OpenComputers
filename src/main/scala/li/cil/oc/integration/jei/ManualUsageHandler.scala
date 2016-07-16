package li.cil.oc.integration.jei

import java.util
import javax.annotation.Nonnull

import li.cil.oc.Localization
import li.cil.oc.api
import mezz.jei.api.IItemRegistry
import mezz.jei.api.recipe.BlankRecipeWrapper
import mezz.jei.api.recipe.IRecipeHandler
import mezz.jei.api.recipe.VanillaRecipeCategoryUid
import net.minecraft.client.Minecraft
import net.minecraft.item.ItemStack
import net.minecraftforge.fml.client.config.GuiButtonExt

import scala.collection.convert.WrapAsJava._
import scala.collection.convert.WrapAsScala._

object ManualUsageHandler {

  def getRecipes(registry: IItemRegistry): util.List[_] = registry.getItemList.collect {
    case stack: ItemStack => api.Manual.pathFor(stack) match {
      case s: String => new ManualUsageRecipe(stack, s)
      case _ =>
    }
  }

  object ManualUsageRecipeHandler extends IRecipeHandler[ManualUsageRecipe] {
    override def getRecipeWrapper(recipe: ManualUsageRecipe) = recipe

    override def getRecipeCategoryUid = VanillaRecipeCategoryUid.DESCRIPTION

    override def isRecipeValid(recipe: ManualUsageRecipe) = true

    override def getRecipeClass = classOf[ManualUsageRecipe]
  }

  class ManualUsageRecipe(stack: ItemStack, path: String) extends BlankRecipeWrapper {
    lazy val button = new GuiButtonExt(0, (160 - 100) / 2, 10, 100, 20, Localization.localizeImmediately("nei.usage.oc.Manual"))

    override def getInputs: util.List[_] = List(stack)

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

}

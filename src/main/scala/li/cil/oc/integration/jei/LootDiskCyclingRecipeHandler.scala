package li.cil.oc.integration.jei

import java.util

import li.cil.oc.Constants
import li.cil.oc.api
import li.cil.oc.common.Loot
import li.cil.oc.common.recipe.LootDiskCyclingRecipe
import mezz.jei.api.ingredients.IIngredients
import mezz.jei.api.recipe._
import net.minecraft.item.ItemStack

import scala.collection.convert.WrapAsJava._

object LootDiskCyclingRecipeHandler extends IRecipeWrapperFactory[LootDiskCyclingRecipe] {
  override def getRecipeWrapper(recipe: LootDiskCyclingRecipe): IRecipeWrapper = new LootDiskCyclingRecipeWrapper(recipe)

  class LootDiskCyclingRecipeWrapper(val recipe: LootDiskCyclingRecipe) extends BlankRecipeWrapper {

    def getInputs: util.List[util.List[ItemStack]] = List(seqAsJavaList(Loot.disksForCycling), seqAsJavaList(List(api.Items.get(Constants.ItemName.Wrench).createItemStack(1))))

    def getOutputs: util.List[ItemStack] = Loot.disksForCycling.toList

    override def getIngredients(ingredients: IIngredients): Unit = {
      ingredients.setInputLists(classOf[ItemStack], getInputs)
      ingredients.setOutputs(classOf[ItemStack], getOutputs)
    }
  }

}




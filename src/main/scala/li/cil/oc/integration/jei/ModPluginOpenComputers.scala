package li.cil.oc.integration.jei

import java.util

import li.cil.oc.Constants
import li.cil.oc.api
import li.cil.oc.common.Loot
import li.cil.oc.common.recipe.LootDiskCyclingRecipe
import li.cil.oc.integration.util.ItemBlacklist
import mezz.jei.api.IItemRegistry
import mezz.jei.api.IJeiHelpers
import mezz.jei.api.IJeiRuntime
import mezz.jei.api.IModPlugin
import mezz.jei.api.IModRegistry
import mezz.jei.api.IRecipeRegistry
import mezz.jei.api.JEIPlugin
import mezz.jei.api.recipe.BlankRecipeWrapper
import mezz.jei.api.recipe.IRecipeHandler
import mezz.jei.api.recipe.IRecipeWrapper
import mezz.jei.api.recipe.VanillaRecipeCategoryUid
import mezz.jei.api.recipe.wrapper.ICraftingRecipeWrapper
import net.minecraft.item.ItemStack

import scala.collection.convert.WrapAsJava._

@JEIPlugin
class ModPluginOpenComputers extends IModPlugin {
  override def onJeiHelpersAvailable(jeiHelpers: IJeiHelpers): Unit = {
    ItemBlacklist.consumers += jeiHelpers.getItemBlacklist.addItemToBlacklist
  }

  override def onItemRegistryAvailable(itemRegistry: IItemRegistry): Unit = {
  }

  override def register(registry: IModRegistry): Unit = {
    registry.addRecipeHandlers(LootDiskCyclingRecipeHandler)
  }

  override def onRecipeRegistryAvailable(recipeRegistry: IRecipeRegistry): Unit = {
  }

  override def onRuntimeAvailable(jeiRuntime: IJeiRuntime): Unit = {
  }

  object LootDiskCyclingRecipeHandler extends IRecipeHandler[LootDiskCyclingRecipe] {
    override def getRecipeClass: Class[LootDiskCyclingRecipe] = classOf[LootDiskCyclingRecipe]

    override def getRecipeCategoryUid: String = VanillaRecipeCategoryUid.CRAFTING

    override def getRecipeWrapper(recipe: LootDiskCyclingRecipe): IRecipeWrapper = new LootDiskCyclingRecipeWrapper(recipe)

    override def isRecipeValid(recipe: LootDiskCyclingRecipe): Boolean = true
  }

  class LootDiskCyclingRecipeWrapper(val recipe: LootDiskCyclingRecipe) extends BlankRecipeWrapper with ICraftingRecipeWrapper {
    override def getInputs: util.List[_] = List(seqAsJavaList(Loot.worldDisks.map(_._1)), api.Items.get(Constants.ItemName.Wrench).createItemStack(1))

    override def getOutputs: util.List[ItemStack] = Loot.worldDisks.map(_._1).toList
  }

}

package li.cil.oc.util

import java.util.Random

import li.cil.oc.OpenComputers
import li.cil.oc.api
import li.cil.oc.common.Tier
import li.cil.oc.common.block.DelegatorConverter
import net.minecraft.item.ItemBucket
import net.minecraft.item.ItemStack
import net.minecraft.item.crafting.CraftingManager
import net.minecraft.item.crafting.IRecipe
import net.minecraft.item.crafting.ShapedRecipes
import net.minecraft.item.crafting.ShapelessRecipes
import net.minecraft.nbt.NBTTagCompound
import net.minecraftforge.oredict.ShapedOreRecipe
import net.minecraftforge.oredict.ShapelessOreRecipe

import scala.collection.convert.WrapAsScala._
import scala.collection.mutable

object ItemUtils {
  def caseTier(stack: ItemStack) = {
    val descriptor = api.Items.get(stack)
    if (descriptor == api.Items.get("case1")) Tier.One
    else if (descriptor == api.Items.get("case2")) Tier.Two
    else if (descriptor == api.Items.get("case3")) Tier.Three
    else if (descriptor == api.Items.get("caseCreative")) Tier.Four
    else if (descriptor == api.Items.get("microcontrollerCase1")) Tier.One
    else if (descriptor == api.Items.get("microcontrollerCase2")) Tier.Two
    else if (descriptor == api.Items.get("microcontrollerCaseCreative")) Tier.Four
    else if (descriptor == api.Items.get("droneCase1")) Tier.One
    else if (descriptor == api.Items.get("droneCase2")) Tier.Two
    else if (descriptor == api.Items.get("droneCaseCreative")) Tier.Four
    else if (descriptor == api.Items.get("server1")) Tier.One
    else if (descriptor == api.Items.get("server2")) Tier.Two
    else if (descriptor == api.Items.get("server3")) Tier.Three
    else if (descriptor == api.Items.get("serverCreative")) Tier.Four
    else if (descriptor == api.Items.get("tabletCase1")) Tier.One
    else if (descriptor == api.Items.get("tabletCase2")) Tier.Two
    else if (descriptor == api.Items.get("tabletCaseCreative")) Tier.Four
    else Tier.None
  }

  def caseNameWithTierSuffix(name: String, tier: Int) = name + (if (tier == Tier.Four) "Creative" else (tier + 1).toString)

  def loadStack(nbt: NBTTagCompound) = DelegatorConverter.convert(ItemStack.loadItemStackFromNBT(nbt))

  def getIngredients(stack: ItemStack): Array[ItemStack] = try {
    val recipes = CraftingManager.getInstance.getRecipeList.map(_.asInstanceOf[IRecipe])
    val recipe = recipes.find(recipe => recipe.getRecipeOutput != null && recipe.getRecipeOutput.isItemEqual(stack))
    val count = recipe.fold(0)(_.getRecipeOutput.stackSize)
    val ingredients = (recipe match {
      case Some(recipe: ShapedRecipes) => recipe.recipeItems.toIterable
      case Some(recipe: ShapelessRecipes) => recipe.recipeItems.map(_.asInstanceOf[ItemStack])
      case Some(recipe: ShapedOreRecipe) => resolveOreDictEntries(recipe.getInput)
      case Some(recipe: ShapelessOreRecipe) => resolveOreDictEntries(recipe.getInput)
      case _ => Iterable.empty
    }).filter(ingredient => ingredient != null &&
      // Strip out buckets, because those are returned when crafting, and
      // we have no way of returning the fluid only (and I can't be arsed
      // to make it output fluids into fluiducts or such, sorry).
      !ingredient.getItem.isInstanceOf[ItemBucket]).toArray
    // Avoid positive feedback loops.
    if (ingredients.exists(ingredient => ingredient.isItemEqual(stack))) {
      return Array.empty
    }
    // Merge equal items for size division by output size.
    val merged = mutable.ArrayBuffer.empty[ItemStack]
    for (ingredient <- ingredients) {
      merged.find(_.isItemEqual(ingredient)) match {
        case Some(entry) => entry.stackSize += ingredient.stackSize
        case _ => merged += ingredient.copy()
      }
    }
    merged.foreach(_.stackSize /= count)
    // Split items up again to 'disassemble them individually'.
    val distinct = mutable.ArrayBuffer.empty[ItemStack]
    for (ingredient <- merged) {
      val size = ingredient.stackSize max 1
      ingredient.stackSize = 1
      for (i <- 0 until size) {
        distinct += ingredient.copy()
      }
    }
    distinct.toArray
  }
  catch {
    case t: Throwable =>
      OpenComputers.log.warn("Whoops, something went wrong when trying to figure out an item's parts.", t)
      Array.empty
  }

  private lazy val rng = new Random()

  private def resolveOreDictEntries[T](entries: Iterable[T]) = entries.collect {
    case stack: ItemStack => stack
    case list: java.util.ArrayList[ItemStack]@unchecked if !list.isEmpty => list.get(rng.nextInt(list.size))
  }

}

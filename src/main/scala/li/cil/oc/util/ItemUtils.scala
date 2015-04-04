package li.cil.oc.util

import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.util.Random

import li.cil.oc.Constants
import li.cil.oc.OpenComputers
import li.cil.oc.api
import li.cil.oc.common.Tier
import net.minecraft.item.ItemBucket
import net.minecraft.item.ItemStack
import net.minecraft.item.crafting.CraftingManager
import net.minecraft.item.crafting.IRecipe
import net.minecraft.item.crafting.ShapedRecipes
import net.minecraft.item.crafting.ShapelessRecipes
import net.minecraft.nbt.CompressedStreamTools
import net.minecraft.nbt.NBTTagCompound
import net.minecraftforge.oredict.ShapedOreRecipe
import net.minecraftforge.oredict.ShapelessOreRecipe

import scala.collection.convert.WrapAsScala._
import scala.collection.mutable

object ItemUtils {
  def caseTier(stack: ItemStack) = {
    val descriptor = api.Items.get(stack)
    if (descriptor == api.Items.get(Constants.BlockName.CaseTier1)) Tier.One
    else if (descriptor == api.Items.get(Constants.BlockName.CaseTier2)) Tier.Two
    else if (descriptor == api.Items.get(Constants.BlockName.CaseTier3)) Tier.Three
    else if (descriptor == api.Items.get(Constants.BlockName.CaseCreative)) Tier.Four
    else if (descriptor == api.Items.get(Constants.ItemName.MicrocontrollerCaseTier1)) Tier.One
    else if (descriptor == api.Items.get(Constants.ItemName.MicrocontrollerCaseTier2)) Tier.Two
    else if (descriptor == api.Items.get(Constants.ItemName.MicrocontrollerCaseCreative)) Tier.Four
    else if (descriptor == api.Items.get(Constants.ItemName.DroneCaseTier1)) Tier.One
    else if (descriptor == api.Items.get(Constants.ItemName.DroneCaseTier2)) Tier.Two
    else if (descriptor == api.Items.get(Constants.ItemName.DroneCaseCreative)) Tier.Four
    else if (descriptor == api.Items.get(Constants.ItemName.ServerTier1)) Tier.One
    else if (descriptor == api.Items.get(Constants.ItemName.ServerTier2)) Tier.Two
    else if (descriptor == api.Items.get(Constants.ItemName.ServerTier3)) Tier.Three
    else if (descriptor == api.Items.get(Constants.ItemName.ServerCreative)) Tier.Four
    else if (descriptor == api.Items.get(Constants.ItemName.TabletCaseTier1)) Tier.One
    else if (descriptor == api.Items.get(Constants.ItemName.TabletCaseTier2)) Tier.Two
    else if (descriptor == api.Items.get(Constants.ItemName.TabletCaseCreative)) Tier.Four
    else Tier.None
  }

  def caseNameWithTierSuffix(name: String, tier: Int) = name + (if (tier == Tier.Four) "Creative" else (tier + 1).toString)

  def loadStack(nbt: NBTTagCompound) = ItemStack.loadItemStackFromNBT(nbt)

  def loadStack(data: Array[Byte]) = {
    ItemStack.loadItemStackFromNBT(loadTag(data))
  }

  def loadTag(data: Array[Byte]) = {
    val bais = new ByteArrayInputStream(data)
    CompressedStreamTools.readCompressed(bais)
  }

  def saveStack(stack: ItemStack) = {
    val tag = new NBTTagCompound()
    stack.writeToNBT(tag)
    saveTag(tag)
  }

  def saveTag(tag: NBTTagCompound) = {
    val baos = new ByteArrayOutputStream()
    CompressedStreamTools.writeCompressed(tag, baos)
    baos.toByteArray
  }

  def getIngredients(stack: ItemStack): Array[ItemStack] = try {
    def getFilteredInputs(inputs: Iterable[ItemStack], outputSize: Double) = inputs.filter(input =>
      input != null &&
      input.getItem != null &&
      math.floor(input.stackSize / outputSize) > 0 &&
      // Strip out buckets, because those are returned when crafting, and
      // we have no way of returning the fluid only (and I can't be arsed
      // to make it output fluids into fluiducts or such, sorry).
      !input.getItem.isInstanceOf[ItemBucket]).toArray
    def getOutputSize(recipe: IRecipe) =
      if (recipe != null && recipe.getRecipeOutput != null)
        recipe.getRecipeOutput.stackSize
      else
        Double.PositiveInfinity

    val recipes = CraftingManager.getInstance.getRecipeList.map(_.asInstanceOf[IRecipe])
    val recipe = recipes.find(recipe => recipe.getRecipeOutput != null && recipe.getRecipeOutput.isItemEqual(stack))
    val count = recipe.fold(0)(_.getRecipeOutput.stackSize)
    val ingredients = recipe match {
      case Some(recipe: ShapedRecipes) => getFilteredInputs(recipe.recipeItems.toIterable, getOutputSize(recipe))
      case Some(recipe: ShapelessRecipes) => getFilteredInputs(recipe.recipeItems.map(_.asInstanceOf[ItemStack]), getOutputSize(recipe))
      case Some(recipe: ShapedOreRecipe) => getFilteredInputs(resolveOreDictEntries(recipe.getInput), getOutputSize(recipe))
      case Some(recipe: ShapelessOreRecipe) => getFilteredInputs(resolveOreDictEntries(recipe.getInput), getOutputSize(recipe))
      case _ => Array.empty
    }
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
    case list: java.util.List[ItemStack]@unchecked if !list.isEmpty => list.get(rng.nextInt(list.size))
  }

}

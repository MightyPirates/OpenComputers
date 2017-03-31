package li.cil.oc.util

import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.util.Random

import li.cil.oc.Constants
import li.cil.oc.OpenComputers
import li.cil.oc.Settings
import li.cil.oc.api
import li.cil.oc.common.Tier
import net.minecraft.block.Block
import net.minecraft.item.Item
import net.minecraft.item.ItemBlock
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
  def getDisplayName(nbt: NBTTagCompound): Option[String] = {
    if (nbt.hasKey("display")) {
      val displayNbt = nbt.getCompoundTag("display")
      if (displayNbt.hasKey("Name"))
        return Option(displayNbt.getString("Name"))
    }
    None
  }

  def setDisplayName(nbt: NBTTagCompound, name: String): Unit = {
    if (!nbt.hasKey("display")) {
      nbt.setTag("display", new NBTTagCompound())
    }
    nbt.getCompoundTag("display").setString("Name", name)
  }

  def caseTier(stack: ItemStack): Int = {
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

  def caseNameWithTierSuffix(name: String, tier: Int): String = name + (if (tier == Tier.Four) "Creative" else (tier + 1).toString)

  def loadTag(data: Array[Byte]): NBTTagCompound = {
    val bais = new ByteArrayInputStream(data)
    CompressedStreamTools.readCompressed(bais)
  }

  def saveStack(stack: ItemStack): Array[Byte] = {
    val tag = new NBTTagCompound()
    stack.writeToNBT(tag)
    saveTag(tag)
  }

  def saveTag(tag: NBTTagCompound): Array[Byte] = {
    val baos = new ByteArrayOutputStream()
    CompressedStreamTools.writeCompressed(tag, baos)
    baos.toByteArray
  }

  def getIngredients(stack: ItemStack): Array[ItemStack] = try {
    def getFilteredInputs(inputs: Iterable[ItemStack], outputSize: Int) = (inputs.filter(input =>
      !input.isEmpty &&
        input.getCount / outputSize > 0 &&
        // Strip out buckets, because those are returned when crafting, and
        // we have no way of returning the fluid only (and I can't be arsed
        // to make it output fluids into fluiducts or such, sorry).
        !input.getItem.isInstanceOf[ItemBucket]).toArray, outputSize)

    def getOutputSize(recipe: IRecipe) = recipe.getRecipeOutput.getCount

    def isInputBlacklisted(stack: ItemStack) = stack.getItem match {
      case item: ItemBlock => Settings.get.disassemblerInputBlacklist.contains(Block.REGISTRY.getNameForObject(item.getBlock))
      case item: Item => Settings.get.disassemblerInputBlacklist.contains(Item.REGISTRY.getNameForObject(item))
      case _ => false
    }

    val (ingredients, count) = CraftingManager.getInstance.getRecipeList.
      filter(recipe => !recipe.getRecipeOutput.isEmpty && recipe.getRecipeOutput.isItemEqual(stack)).collect {
      case recipe: ShapedRecipes => getFilteredInputs(recipe.recipeItems.toIterable, getOutputSize(recipe))
      case recipe: ShapelessRecipes => getFilteredInputs(recipe.recipeItems, getOutputSize(recipe))
      case recipe: ShapedOreRecipe => getFilteredInputs(resolveOreDictEntries(recipe.getInput), getOutputSize(recipe))
      case recipe: ShapelessOreRecipe => getFilteredInputs(resolveOreDictEntries(recipe.getInput), getOutputSize(recipe))
    }.collectFirst {
      case (inputs, outputSize) if !inputs.exists(isInputBlacklisted) => (inputs, outputSize)
    } match {
      case Some((inputs, outputSize)) => (inputs, outputSize)
      case _ => return Array.empty
    }

    // Avoid positive feedback loops.
    if (ingredients.exists(ingredient => ingredient.isItemEqual(stack))) {
      return Array.empty[ItemStack]
    }
    // Merge equal items for size division by output size.
    val merged = mutable.ArrayBuffer.empty[ItemStack]
    for (ingredient <- ingredients) {
      merged.find(_.isItemEqual(ingredient)) match {
        case Some(entry) => entry.grow(ingredient.getCount)
        case _ => merged += ingredient.copy()
      }
    }
    merged.foreach(s => s.setCount(s.getCount / count))
    // Split items up again to 'disassemble them individually'.
    val distinct = mutable.ArrayBuffer.empty[ItemStack]
    for (ingredient <- merged) {
      val size = ingredient.getCount max 1
      ingredient.setCount(1)
      for (i <- 0 until size) {
        distinct += ingredient.copy()
      }
    }
    distinct.toArray
  }
  catch {
    case t: Throwable =>
      OpenComputers.log.warn("Whoops, something went wrong when trying to figure out an item's parts.", t)
      Array.empty[ItemStack]
  }

  private lazy val rng = new Random()

  private def resolveOreDictEntries[T](entries: Iterable[T]) = entries.collect {
    case stack: ItemStack => stack
    case list: java.util.List[ItemStack]@unchecked if !list.isEmpty => list.get(rng.nextInt(list.size))
  }

}

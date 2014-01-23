package li.cil.oc.util

import cpw.mods.fml.common.registry.{ItemData, GameData}
import java.util
import li.cil.oc.util.ExtendedNBT._
import net.minecraft.item.{Item, ItemStack}
import net.minecraft.item.crafting._
import net.minecraft.nbt.{NBTTagList, NBTTagCompound}
import net.minecraftforge.oredict.{ShapelessOreRecipe, ShapedOreRecipe}
import scala.Some
import scala.collection.convert.WrapAsScala._
import scala.collection.mutable
import li.cil.oc.Items

object ItemCosts {
  protected val cache = mutable.Map.empty[ItemStack, Iterable[(ItemStack, Double)]]

  cache += Items.ironNugget.createItemStack() -> Iterable((new ItemStack(Item.ingotIron), 1.0 / 9.0))

  protected val modsByItemId = {
    val nbt = new NBTTagList()
    GameData.writeItemData(nbt)
    nbt.iterator[NBTTagCompound].map(new ItemData(_)).map(itemData => itemData.getItemId -> (itemData.getModId == "Minecraft")).toMap
  }

  protected def isVanilla(stack: ItemStack) = modsByItemId.getOrElse(stack.itemID, true)

  def addTooltip(stack: ItemStack, tooltip: util.List[String]) {
    tooltip.add("Materials:")
    for ((ingredient, count) <- computeIngredients(stack)) {
      val line = math.ceil(count).toInt + "x " + ingredient.getDisplayName
      tooltip.add(line)
    }
  }

  protected def computeIngredients(what: ItemStack): Iterable[(ItemStack, Double)] = {
    def deflate(list: Iterable[(ItemStack, Double)]): Iterable[(ItemStack, Double)] = {
      val counts = mutable.Map.empty[ItemStack, Double]
      for ((stack, count) <- list) {
        counts.find(_._1.isItemEqual(stack)) match {
          case Some((key, value)) => counts.update(key, value + count)
          case _ => counts += stack -> count
        }
      }
      counts
    }
    def accumulate(input: Any, path: Seq[ItemStack] = Seq.empty): Iterable[(ItemStack, Double)] = input match {
      case stack: ItemStack =>
        cache.find(_._1.isItemEqual(stack)) match {
          case Some((_, value)) => value
          case _ =>
            if (isVanilla(stack) || path.exists(_.isItemEqual(stack))) {
              Iterable((stack, 1.0))
            }
            else {
              val recipes = CraftingManager.getInstance.getRecipeList.map(_.asInstanceOf[IRecipe])
              val recipe = recipes.find(recipe => recipe.getRecipeOutput != null && stack.isItemEqual(recipe.getRecipeOutput))
              val (ingredients, output) = recipe match {
                case Some(recipe: ShapedRecipes) => (recipe.recipeItems.flatMap(accumulate(_, path :+ stack)).toIterable, recipe.getRecipeOutput.stackSize)
                case Some(recipe: ShapelessRecipes) => (recipe.recipeItems.flatMap(accumulate(_, path :+ stack)).toIterable, recipe.getRecipeOutput.stackSize)
                case Some(recipe: ShapedOreRecipe) => (recipe.getInput.flatMap(accumulate(_, path :+ stack)).toIterable, recipe.getRecipeOutput.stackSize)
                case Some(recipe: ShapelessOreRecipe) => (recipe.getInput.flatMap(accumulate(_, path :+ stack)).toIterable, recipe.getRecipeOutput.stackSize)
                case _ => FurnaceRecipes.smelting.getSmeltingList.asInstanceOf[util.Map[Integer, ItemStack]].find {
                  case (_, value) => stack.isItemEqual(value)
                } match {
                  case Some((blockId, result)) => (accumulate(new ItemStack(blockId, 1, 0), path :+ stack), result.stackSize)
                  case _ => FurnaceRecipes.smelting.getMetaSmeltingList.find {
                    case (_, value) => stack.isItemEqual(value)
                  } match {
                    case Some((data, result)) =>
                      val (itemId, metadata) = (data.get(0), data.get(1))
                      (accumulate(new ItemStack(itemId, 1, metadata), path :+ stack), result.stackSize)
                    case _ => (Iterable((stack, 1.0)), 1)
                  }
                }
              }
              val scaled = deflate(ingredients.map {
                case (ingredient, count) => (ingredient.copy(), count / output)
              })
              cache += stack.copy() -> scaled
              scaled
            }
        }
      case list: util.ArrayList[ItemStack]@unchecked if !list.isEmpty =>
        var result = Iterable.empty[(ItemStack, Double)]
        for (stack <- list if result.isEmpty) {
          result = accumulate(list.get(0), path)
        }
        result
      case _ => Iterable.empty
    }
    accumulate(what)
  }
}

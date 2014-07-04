package li.cil.oc.util

import java.util

import li.cil.oc.Localization
import net.minecraft.block.Block
import net.minecraft.init.{Blocks, Items}
import net.minecraft.item.crafting._
import net.minecraft.item.{Item, ItemStack}
import net.minecraftforge.oredict.{OreDictionary, ShapedOreRecipe, ShapelessOreRecipe}

import scala.collection.convert.WrapAsScala._
import scala.collection.mutable

object ItemCosts {
  protected val cache = mutable.Map.empty[ItemStack, Iterable[(ItemStack, Double)]]

  cache += li.cil.oc.Items.ironNugget.createItemStack() -> Iterable((new ItemStack(Items.iron_ingot), 1.0 / 9.0))

  def terminate(item: Item, meta: Int = 0) = cache += new ItemStack(item, 1, meta) -> mutable.Iterable((new ItemStack(item, 1, meta), 1))

  def terminate(block: Block) = cache += new ItemStack(block) -> mutable.Iterable((new ItemStack(block), 1))

  terminate(Blocks.clay)
  terminate(Blocks.cobblestone)
  terminate(Blocks.glass)
  terminate(Blocks.planks)
  terminate(Blocks.sand)
  terminate(Blocks.stone)
  terminate(Items.blaze_rod)
  terminate(Items.bucket)
  terminate(Items.clay_ball)
  terminate(Items.coal)
  terminate(Items.diamond)
  for (i <- 0 to 15) terminate(Items.dye, i)
  terminate(Items.emerald)
  terminate(Items.ender_pearl)
  terminate(Items.ender_eye)
  terminate(Items.ghast_tear)
  terminate(Items.glowstone_dust)
  terminate(Items.gold_ingot)
  terminate(Items.iron_ingot)
  terminate(Items.quartz)
  terminate(Items.nether_star)
  terminate(Items.paper)
  terminate(Items.redstone)
  terminate(Items.string)
  terminate(Items.slime_ball)
  terminate(Items.stick)

  def hasCosts(stack: ItemStack) = {
    val ingredients = computeIngredients(stack)
    ingredients.size > 0 && (ingredients.size > 1 || !ingredients.head._1.isItemEqual(stack))
  }

  def addTooltip(stack: ItemStack, tooltip: util.List[String]) {
    tooltip.add(Localization.Tooltip.Materials)
    for ((ingredient, count) <- computeIngredients(stack)) {
      val line = math.ceil(count).toInt + "x " + ingredient.getDisplayName
      tooltip.add(line)
    }
  }

  protected def computeIngredients(what: ItemStack): Iterable[(ItemStack, Double)] = {
    def deflate(list: Iterable[(ItemStack, Double)]): Iterable[(ItemStack, Double)] = {
      val counts = mutable.Map.empty[ItemStack, Double]
      for ((stack, count) <- list) {
        counts.find {
          case (key, value) => fuzzyEquals(key, stack)
        } match {
          case Some((key, value)) => counts.update(key, value + count)
          case _ => counts += stack -> count
        }
      }
      counts
    }
    def accumulate(input: Any, path: Seq[ItemStack] = Seq.empty): Iterable[(ItemStack, Double)] = input match {
      case stack: ItemStack =>
        cache.find {
          case (key, value) => fuzzyEquals(key, stack)
        } match {
          case Some((_, value)) => value
          case _ =>
            if (path.exists(value => fuzzyEquals(value, stack))) {
              Iterable((stack, 1.0))
            }
            else {
              val recipes = CraftingManager.getInstance.getRecipeList.map(_.asInstanceOf[IRecipe])
              val recipe = recipes.find(recipe => recipe.getRecipeOutput != null && fuzzyEquals(stack, recipe.getRecipeOutput))
              val (ingredients, output) = recipe match {
                case Some(recipe: ShapedRecipes) => (recipe.recipeItems.flatMap(accumulate(_, path :+ stack)).toIterable, recipe.getRecipeOutput.stackSize)
                case Some(recipe: ShapelessRecipes) => (recipe.recipeItems.flatMap(accumulate(_, path :+ stack)).toIterable, recipe.getRecipeOutput.stackSize)
                case Some(recipe: ShapedOreRecipe) => (recipe.getInput.flatMap(accumulate(_, path :+ stack)).toIterable, recipe.getRecipeOutput.stackSize)
                case Some(recipe: ShapelessOreRecipe) => (recipe.getInput.flatMap(accumulate(_, path :+ stack)).toIterable, recipe.getRecipeOutput.stackSize)
                case _ => FurnaceRecipes.smelting.getSmeltingList.asInstanceOf[util.Map[ItemStack, ItemStack]].find {
                  case (_, value) => fuzzyEquals(stack, value)
                } match {
                  case Some((rein, raus)) => (accumulate(rein, path :+ stack), raus.stackSize)
                  case _ => (Iterable((stack, 1.0)), 1)
                }
              }
              val scaled = deflate(ingredients.map {
                case (ingredient, count) => (ingredient.copy(), count / output)
              }).toArray.sortBy(_._1.getUnlocalizedName)
              cache += stack.copy() -> scaled
              scaled
            }
        }
      case list: util.ArrayList[ItemStack]@unchecked if !list.isEmpty =>
        var result = Iterable.empty[(ItemStack, Double)]
        for (stack <- list if result.isEmpty) {
          cache.find {
            case (key, value) => fuzzyEquals(key, stack)
          } match {
            case Some((_, value)) => result = value
            case _ =>
          }
        }
        if (result.isEmpty) {
          result = accumulate(list.get(0), path)
        }
        result
      case _ => Iterable.empty
    }
    accumulate(what)
  }

  // In case you'd like to use this class for your items and your items use
  // NBT data in the item stack to differentiate them uncomment the last part.
  // We don't use this in OC because the NBT of items can change dynamically,
  // for example by components being assigned an address, which will break the
  // equals check.
  private def fuzzyEquals(stack1: ItemStack, stack2: ItemStack) =
    stack1 == stack2 || (stack1 != null && stack2 != null &&
      stack1.getItem == stack2.getItem &&
      (stack1.getItemDamage == stack2.getItemDamage ||
        stack1.getItemDamage == OreDictionary.WILDCARD_VALUE ||
        stack2.getItemDamage == OreDictionary.WILDCARD_VALUE ||
        stack1.getItem.isDamageable) // && ItemStack.areItemStackTagsEqual(stack1, stack2)
      )
}

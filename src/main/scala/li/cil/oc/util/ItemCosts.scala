package li.cil.oc.util

import java.util

import li.cil.oc.Constants
import li.cil.oc.Localization
import li.cil.oc.api
import li.cil.oc.integration.Mods
import net.minecraft.block.Block
import net.minecraft.init.Blocks
import net.minecraft.init.Items
import net.minecraft.item.Item
import net.minecraft.item.ItemStack
import net.minecraft.item.crafting._
import net.minecraftforge.oredict.OreDictionary
import net.minecraftforge.oredict.ShapedOreRecipe
import net.minecraftforge.oredict.ShapelessOreRecipe

import scala.collection.convert.WrapAsScala._
import scala.collection.mutable

object ItemCosts {
  private final val Timeout = 500

  private val cache = mutable.Map.empty[ItemStackWrapper, Iterable[(ItemStack, Double)]]

  private var started = 0L

  cache += new ItemStackWrapper(api.Items.get(Constants.ItemName.IronNugget).createItemStack(1)) -> Iterable((new ItemStack(Items.IRON_INGOT), 1.0 / 9.0))

  def terminate(item: Item, meta: Int = 0) = cache += new ItemStackWrapper(new ItemStack(item, 1, meta)) -> mutable.Iterable((new ItemStack(item, 1, meta), 1))

  def terminate(block: Block) = cache += new ItemStackWrapper(new ItemStack(block)) -> mutable.Iterable((new ItemStack(block), 1))

  terminate(Blocks.CLAY)
  terminate(Blocks.COBBLESTONE)
  terminate(Blocks.GLASS)
  terminate(Blocks.PLANKS)
  terminate(Blocks.SAND)
  terminate(Blocks.STONE)
  terminate(Items.BLAZE_ROD)
  terminate(Items.BUCKET)
  terminate(Items.CLAY_BALL)
  terminate(Items.COAL)
  terminate(Items.DIAMOND)
  for (i <- 0 to 15) terminate(Items.DYE, i)
  terminate(Items.EMERALD)
  terminate(Items.ENDER_PEARL)
  terminate(Items.ENDER_EYE)
  terminate(Items.GHAST_TEAR)
  terminate(Items.GLOWSTONE_DUST)
  terminate(Items.GOLD_INGOT)
  terminate(Items.IRON_INGOT)
  terminate(Items.QUARTZ)
  terminate(Items.NETHER_STAR)
  terminate(Items.PAPER)
  terminate(Items.REDSTONE)
  terminate(Items.STRING)
  terminate(Items.SLIME_BALL)
  terminate(Items.STICK)

  def hasCosts(stack: ItemStack) = !Mods.CraftingCosts.isAvailable && {
    val ingredients = computeIngredients(stack)
    ingredients.nonEmpty && (ingredients.size > 1 || !ingredients.head._1.isItemEqual(stack))
  }

  def addTooltip(stack: ItemStack, tooltip: util.List[String]) {
    tooltip.add(Localization.Tooltip.Materials)
    for ((ingredient, count) <- computeIngredients(stack)) {
      val line = math.ceil(count).toInt + "x " + ingredient.getDisplayName
      tooltip.add(line)
    }
  }

  protected def computeIngredients(what: ItemStack): Iterable[(ItemStack, Double)] = cache.synchronized {
    started = System.currentTimeMillis()
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
    def accumulate(input: Any, path: Seq[ItemStack] = Seq.empty): Iterable[(ItemStack, Double)] = {
      val passed = System.currentTimeMillis() - started
      if (passed > Timeout) Iterable.empty
      else input match {
        case stack: ItemStack =>
          cache.find {
            case (key, value) => fuzzyEquals(key.inner, stack)
          } match {
            case Some((_, value)) => value
            case _ =>
              if (path.exists(value => fuzzyEquals(value, stack))) {
                Iterable((stack, 1.0))
              }
              else {
                val recipes = CraftingManager.getInstance.getRecipeList
                if (recipes == null) Iterable((stack, 1.0))
                else {
                  val recipe = recipes.filter(_ != null).find(recipe => recipe.getRecipeOutput != null && fuzzyEquals(stack, recipe.getRecipeOutput))
                  val (ingredients, output) = recipe match {
                    case Some(recipe: ShapedRecipes) => (recipe.recipeItems.flatMap(accumulate(_, path :+ stack)).toIterable, recipe.getRecipeOutput.getCount)
                    case Some(recipe: ShapelessRecipes) => (recipe.recipeItems.flatMap(accumulate(_, path :+ stack)), recipe.getRecipeOutput.getCount)
                    case Some(recipe: ShapedOreRecipe) => (recipe.getInput.flatMap(accumulate(_, path :+ stack)).toIterable, recipe.getRecipeOutput.getCount)
                    case Some(recipe: ShapelessOreRecipe) => (recipe.getInput.flatMap(accumulate(_, path :+ stack)), recipe.getRecipeOutput.getCount)
                    case _ => FurnaceRecipes.instance.getSmeltingList.find {
                      case (_, value) => fuzzyEquals(stack, value)
                    } match {
                      case Some((rein, raus)) => (accumulate(rein, path :+ stack), raus.getCount)
                      case _ => (Iterable((stack, 1.0)), 1)
                    }
                  }
                  val scaled = deflate(ingredients.map {
                    case (ingredient, count) => (ingredient.copy(), count / output)
                  }).toArray.sortBy(_._1.getUnlocalizedName)
                  cache += new ItemStackWrapper(stack.copy()) -> scaled
                  scaled
                }
              }
          }
        case list: java.util.List[ItemStack]@unchecked if !list.isEmpty =>
          var result = Iterable.empty[(ItemStack, Double)]
          for (stack <- list if result.isEmpty) {
            cache.find {
              case (key, value) => fuzzyEquals(key.inner, stack)
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

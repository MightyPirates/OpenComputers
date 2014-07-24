package li.cil.oc.common.recipe

import java.io.{File, FileReader}

import com.typesafe.config._
import cpw.mods.fml.common.Loader
import cpw.mods.fml.common.registry.GameRegistry
import li.cil.oc._
import li.cil.oc.util.Color
import li.cil.oc.util.mods.GregTech
import net.minecraft.block.Block
import net.minecraft.item.crafting.FurnaceRecipes
import net.minecraft.item.{Item, ItemStack}
import net.minecraftforge.oredict.RecipeSorter.Category
import net.minecraftforge.oredict.{OreDictionary, RecipeSorter}
import org.apache.commons.io.FileUtils

import scala.collection.convert.WrapAsScala._
import scala.collection.mutable

object Recipes {
  val list = mutable.LinkedHashMap.empty[ItemStack, String]
  val oreDictEntries = mutable.LinkedHashMap.empty[String, ItemStack]

  def addBlock[T <: common.block.Delegate](delegate: T, name: String, oreDict: String = null) = {
    Items.registerBlock(delegate, name)
    list += delegate.createItemStack() -> name
    register(oreDict, delegate.createItemStack())
    delegate
  }

  def addNewBlock(instance: Block, name: String, oreDict: String = null) = {
    GameRegistry.registerBlock(instance, classOf[common.block.Item], name)
    Items.registerBlock(instance, name)
    list += new ItemStack(instance) -> name
    register(oreDict, new ItemStack(instance))
    instance
  }

  def addItem[T <: common.item.Delegate](delegate: T, name: String, oreDict: String = null) = {
    Items.registerItem(delegate, name)
    list += delegate.createItemStack() -> name
    register(oreDict, delegate.createItemStack())
    delegate
  }

  def addItem(instance: Item, name: String) = {
    Items.registerItem(instance, name)
    list += new ItemStack(instance) -> name
    instance
  }

  private def register(name: String, item: ItemStack) {
    if (name != null) {
      oreDictEntries += name -> item
    }
  }

  def init() {
    RecipeSorter.register(Settings.namespace + "extshaped", classOf[ExtendedShapedOreRecipe], Category.SHAPED, "after:forge:shapedore")
    RecipeSorter.register(Settings.namespace + "extshapeless", classOf[ExtendedShapelessOreRecipe], Category.SHAPELESS, "after:forge:shapelessore")

    for ((name, stack) <- oreDictEntries) {
      if (!OreDictionary.getOres(name).contains(stack)) {
        OreDictionary.registerOre(name, stack)
      }
    }
    oreDictEntries.clear()

    try {
      val defaultRecipes = new File(Loader.instance.getConfigDir + File.separator + "opencomputers" + File.separator + "default.recipes")
      val hardmodeRecipes = new File(Loader.instance.getConfigDir + File.separator + "opencomputers" + File.separator + "hardmode.recipes")
      val gregTechRecipes = new File(Loader.instance.getConfigDir + File.separator + "opencomputers" + File.separator + "gregtech.recipes")
      val userRecipes = new File(Loader.instance.getConfigDir + File.separator + "opencomputers" + File.separator + "user.recipes")

      defaultRecipes.getParentFile.mkdirs()
      FileUtils.copyURLToFile(getClass.getResource("/assets/opencomputers/recipes/default.recipes"), defaultRecipes)
      FileUtils.copyURLToFile(getClass.getResource("/assets/opencomputers/recipes/hardmode.recipes"), hardmodeRecipes)
      FileUtils.copyURLToFile(getClass.getResource("/assets/opencomputers/recipes/gregtech.recipes"), gregTechRecipes)
      if (!userRecipes.exists()) {
        FileUtils.copyURLToFile(getClass.getResource("/assets/opencomputers/recipes/user.recipes"), userRecipes)
      }
      lazy val config: ConfigParseOptions = ConfigParseOptions.defaults.
        setSyntax(ConfigSyntax.CONF).
        setIncluder(new ConfigIncluder with ConfigIncluderFile {
        var fallback: ConfigIncluder = _

        override def withFallback(fallback: ConfigIncluder) = {
          this.fallback = fallback
          this
        }

        override def include(context: ConfigIncludeContext, what: String) = fallback.include(context, what)

        override def includeFile(context: ConfigIncludeContext, what: File) = {
          val in = if (what.isAbsolute) new FileReader(what) else new FileReader(new File(userRecipes.getParentFile, what.getPath))
          val result = ConfigFactory.parseReader(in, config)
          in.close()
          result.root()
        }
      })
      val recipes = ConfigFactory.parseFile(userRecipes, config)

      // Register all known recipes.
      for ((stack, name) <- list) {
        addRecipe(stack, recipes, name)
      }

      // Navigation upgrade recrafting.
      val navigationUpgrade = api.Items.get("navigationUpgrade").createItemStack(1)
      GameRegistry.addRecipe(new ExtendedShapelessOreRecipe(navigationUpgrade, navigationUpgrade, new ItemStack(net.minecraft.init.Items.filled_map, 1, OreDictionary.WILDCARD_VALUE)))

      // Floppy disk coloring.
      val floppy = api.Items.get("floppy").createItemStack(1)
      for (dye <- Color.dyes) {
        GameRegistry.addRecipe(new ExtendedShapelessOreRecipe(floppy, floppy, dye))
      }
    }
    catch {
      case e: Throwable => OpenComputers.log.error("Error parsing recipes, you may not be able to craft any items from this mod!", e)
    }
    list.clear()
  }

  private def addRecipe(output: ItemStack, list: Config, name: String) = try {
    if (list.hasPath(name)) {
      val recipe = list.getConfig(name)
      val recipeType = tryGetType(recipe)
      try {
        recipeType match {
          case "shaped" => addShapedRecipe(output, recipe)
          case "shapeless" => addShapelessRecipe(output, recipe)
          case "furnace" => addFurnaceRecipe(output, recipe)
          case "assembly" => addAssemblyRecipe(output, recipe)
          case other =>
            OpenComputers.log.warn("Failed adding recipe for '" + name + "', you will not be able to craft this item! The error was: Invalid recipe type '" + other + "'.")
            hide(output)
        }
      }
      catch {
        case e: RecipeException =>
          OpenComputers.log.warn("Failed adding " + recipeType + " recipe for '" + name + "', you will not be able to craft this item! The error was: " + e.getMessage)
          hide(output)
      }
    }
    else {
      OpenComputers.log.info("No recipe for '" + name + "', you will not be able to craft this item.")
      hide(output)
    }
  }
  catch {
    case e: Throwable =>
      OpenComputers.log.error("Failed adding recipe for '" + name + "', you will not be able to craft this item!", e)
      hide(output)
  }

  private def addShapedRecipe(output: ItemStack, recipe: Config) {
    val rows = recipe.getList("input").unwrapped().map {
      case row: java.util.List[AnyRef]@unchecked => row.map(parseIngredient)
      case other => throw new RecipeException("Invalid row entry for shaped recipe (not a list: " + other + ").")
    }
    output.stackSize = tryGetCount(recipe)

    var number = -1
    var shape = mutable.ArrayBuffer.empty[String]
    val input = mutable.ArrayBuffer.empty[AnyRef]
    for (row <- rows) {
      val (pattern, ingredients) = row.foldLeft((new StringBuilder, Seq.empty[AnyRef]))((acc, ingredient) => {
        val (pattern, ingredients) = acc
        ingredient match {
          case _@(_: ItemStack | _: String) =>
            number += 1
            (pattern.append(('a' + number).toChar), ingredients ++ Seq(Char.box(('a' + number).toChar), ingredient))
          case _ => (pattern.append(' '), ingredients)
        }
      })
      shape += pattern.toString
      input ++= ingredients
    }
    if (input.size > 0 && output.stackSize > 0) {
      GameRegistry.addRecipe(new ExtendedShapedOreRecipe(output, shape ++ input: _*))
    }
    else hide(output)
  }

  private def addShapelessRecipe(output: ItemStack, recipe: Config) {
    val input = recipe.getValue("input").unwrapped() match {
      case list: java.util.List[AnyRef]@unchecked => list.map(parseIngredient)
      case other => Seq(parseIngredient(other))
    }
    output.stackSize = tryGetCount(recipe)

    if (input.size > 0 && output.stackSize > 0) {
      GameRegistry.addRecipe(new ExtendedShapelessOreRecipe(output, input: _*))
    }
    else hide(output)
  }

  private def addAssemblyRecipe(output: ItemStack, recipe: Config) {
    val input = (recipe.getValue("input").unwrapped() match {
      case list: java.util.List[AnyRef]@unchecked => list.map(parseIngredient)
      case other => Seq(parseIngredient(other))
    }) map {
      case stack: ItemStack => stack
      case null => null
      case name: String => throw new RecipeException("Invalid ingredient '" + name + "', OreDictionary not supported for assembly recipes.")
      case other => throw new RecipeException("Invalid ingredient type: " + other + ".")
    }
    output.stackSize = tryGetCount(recipe)

    if (input.size < 1 || input.size > 2) {
      throw new RecipeException("Invalid recipe length: " + input.size + ", should be 1 or 2.")
    }

    val inputCount = recipe.getIntList("count")
    if (inputCount.size() != input.size) {
      throw new RecipeException("Ingredient and input count mismatch: " + input.size + " != " + inputCount.size + ".")
    }

    val eu = recipe.getInt("eu")
    val duration = recipe.getInt("time")

    (input, inputCount).zipped.foreach((stack, count) => if (stack != null && count > 0) stack.stackSize = stack.getMaxStackSize min count)
    input.padTo(2, null)

    if (input(0) != null) {
      GregTech.addAssemblerRecipe(input(0), input(1), output, duration, eu)
    }
  }

  private def addFurnaceRecipe(output: ItemStack, recipe: Config) {
    val input = parseIngredient(recipe.getValue("input").unwrapped())
    output.stackSize = tryGetCount(recipe)

    input match {
      case stack: ItemStack =>
        FurnaceRecipes.smelting.func_151394_a(stack, output, 0)
      case name: String =>
        for (stack <- OreDictionary.getOres(name)) {
          FurnaceRecipes.smelting.func_151394_a(stack, output, 0)
        }
      case _ =>
    }
  }

  private def parseIngredient(entry: AnyRef) = entry match {
    case map: java.util.Map[AnyRef, AnyRef]@unchecked =>
      if (map.contains("oreDict")) {
        map.get("oreDict") match {
          case value: String => value
          case other => throw new RecipeException("Invalid name in recipe (not a string: " + other + ").")
        }
      }
      else if (map.contains("item")) {
        map.get("item") match {
          case name: String =>
            // TODO Item.itemRegistry.getObject?
            Item.itemRegistry.find {
              case item: Item => itemNameEquals(item, name)
            } match {
              case Some(item: Item) => new ItemStack(item, 1, tryGetId(map))
              case _ => throw new RecipeException("No item found with name '" + name + "'.")
            }
          case id: Number => new ItemStack(validateItemId(id), 1, tryGetId(map))
          case other => throw new RecipeException("Invalid item name in recipe (not a string: " + other + ").")
        }
      }
      else if (map.contains("block")) {
        map.get("block") match {
          case name: String =>
            // TODO Block.blockRegistry.getObject?
            Block.blockRegistry.find {
              case block: Block => blockNameEquals(block, name)
            } match {
              case Some(block: Block) => new ItemStack(block, 1, tryGetId(map))
              case _ => throw new RecipeException("No block found with name '" + name + "'.")
            }
          case id: Number => new ItemStack(validateBlockId(id), 1, tryGetId(map))
          case other => throw new RecipeException("Invalid block name (not a string: " + other + ").")
        }
      }
      else throw new RecipeException("Invalid ingredient type (no oreDict, item or block entry).")
    case name: String =>
      if (name == null || name.trim.isEmpty) null
      else if (OreDictionary.getOres(name) != null && !OreDictionary.getOres(name).isEmpty) name
      else {
        // TODO Item.itemRegistry.getObject?
        Item.itemRegistry.find {
          case item: Item => itemNameEquals(item, name)
        } match {
          case Some(item: Item) => new ItemStack(item, 1, 0)
          case _ =>
            // TODO Block.blockRegistry.getObject?
            Block.blockRegistry.find {
              case block: Block => blockNameEquals(block, name)
            } match {
              case Some(block: Block) => new ItemStack(block, 1, 0)
              case _ => throw new RecipeException("No ore dictionary entry, item or block found for ingredient with name '" + name + "'.")
            }
        }
      }
    case other => throw new RecipeException("Invalid ingredient type (not a map or string): " + other)
  }

  private def itemNameEquals(item: Item, name: String) =
    item != null && (item.getUnlocalizedName == name || item.getUnlocalizedName == "item." + name)

  private def blockNameEquals(block: Block, name: String) =
    block != null && (block.getUnlocalizedName == name || block.getUnlocalizedName == "tile." + name)

  private def tryGetType(recipe: Config) = if (recipe.hasPath("type")) recipe.getString("type") else "shaped"

  private def tryGetCount(recipe: Config) = if (recipe.hasPath("output")) recipe.getInt("output") else 1

  private def tryGetId(ingredient: java.util.Map[AnyRef, AnyRef]): Int =
    if (ingredient.contains("subID")) ingredient.get("subID") match {
      case id: Number => id.intValue
      case "any" => OreDictionary.WILDCARD_VALUE
      case id: String => Integer.valueOf(id)
      case _ => 0
    } else 0

  private def validateBlockId(id: Number) = {
    val index = id.intValue
    val block = Block.getBlockById(index)
    if (block == null) throw new RecipeException("Invalid block ID: " + index)
    block
  }

  private def validateItemId(id: Number) = {
    val index = id.intValue
    val item = Item.getItemById(index)
    if (item == null) throw new RecipeException("Invalid item ID: " + index)
    item
  }

  private def hide(value: ItemStack) {
    Items.multi.subItem(value) match {
      case Some(stack) => stack.showInItemList = false
      case _ => common.block.Delegator.subBlock(value) match {
        case Some(block) => block.showInItemList = false
        case _ =>
      }
    }
  }

  private class RecipeException(message: String) extends RuntimeException(message)

}

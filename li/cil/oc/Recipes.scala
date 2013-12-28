package li.cil.oc

import com.typesafe.config._
import cpw.mods.fml.common.Loader
import cpw.mods.fml.common.registry.GameRegistry
import java.io.{FileReader, File}
import java.util.logging.Level
import li.cil.oc.util.mods.GregTech
import net.minecraft.block.Block
import net.minecraft.item.crafting.FurnaceRecipes
import net.minecraft.item.{ItemStack, Item}
import net.minecraftforge.oredict.{OreDictionary, ShapelessOreRecipe, ShapedOreRecipe}
import org.apache.commons.io.FileUtils
import scala.Some
import scala.collection.convert.wrapAsScala._
import scala.collection.mutable.ArrayBuffer

object Recipes {
  def init() {
    try {
      val defaultRecipes = new File(Loader.instance.getConfigDir + File.separator + "opencomputers" + File.separator + "default.recipes")
      val gregTechRecipes = new File(Loader.instance.getConfigDir + File.separator + "opencomputers" + File.separator + "gregtech.recipes")
      val userRecipes = new File(Loader.instance.getConfigDir + File.separator + "opencomputers" + File.separator + "user.recipes")

      defaultRecipes.getParentFile.mkdirs()
      FileUtils.copyURLToFile(getClass.getResource("/assets/opencomputers/recipes/default.recipes"), defaultRecipes)
      FileUtils.copyURLToFile(getClass.getResource("/assets/opencomputers/recipes/gregtech.recipes"), gregTechRecipes)
      if (!userRecipes.exists()) {
        FileUtils.copyURLToFile(getClass.getResource("/assets/opencomputers/recipes/user.recipes"), userRecipes)
      }
      val config = ConfigParseOptions.defaults.
        setSyntax(ConfigSyntax.CONF).
        setIncluder(new ConfigIncluder with ConfigIncluderFile {
        var fallback: ConfigIncluder = _

        def withFallback(fallback: ConfigIncluder) = {
          this.fallback = fallback
          this
        }

        def include(context: ConfigIncludeContext, what: String) = fallback.include(context, what)

        def includeFile(context: ConfigIncludeContext, what: File) = {
          val in = if (what.isAbsolute) new FileReader(what) else new FileReader(new File(userRecipes.getParentFile, what.getPath))
          val result = ConfigFactory.parseReader(in)
          in.close()
          result.root()
        }
      })
      val recipes = ConfigFactory.parseFile(userRecipes, config)

      // Try to keep this in the same order as the fields in the Items class
      // to make it easier to match them and check if anything is missing.
      addRecipe(Items.analyzer.createItemStack(), recipes, "analyzer")

      addRecipe(Items.ram1.createItemStack(), recipes, "ram1")
      addRecipe(Items.ram2.createItemStack(), recipes, "ram2")
      addRecipe(Items.ram3.createItemStack(), recipes, "ram3")

      addRecipe(Items.floppyDisk.createItemStack(), recipes, "floppy")
      addRecipe(Items.hdd1.createItemStack(), recipes, "hdd1")
      addRecipe(Items.hdd2.createItemStack(), recipes, "hdd2")
      addRecipe(Items.hdd3.createItemStack(), recipes, "hdd3")

      addRecipe(Items.gpu1.createItemStack(), recipes, "graphicsCard1")
      addRecipe(Items.gpu2.createItemStack(), recipes, "graphicsCard2")
      addRecipe(Items.gpu3.createItemStack(), recipes, "graphicsCard3")
      addRecipe(Items.lan.createItemStack(), recipes, "lanCard")
      addRecipe(Items.rs.createItemStack(), recipes, "redstoneCard")
      addRecipe(Items.wlan.createItemStack(), recipes, "wlanCard")

      addRecipe(Items.upgradeCrafting.createItemStack(), recipes, "craftingUpgrade")
      addRecipe(Items.upgradeGenerator.createItemStack(), recipes, "generatorUpgrade")
      addRecipe(Items.upgradeNavigation.createItemStack(), recipes, "navigationUpgrade")
      addRecipe(Items.upgradeSign.createItemStack(), recipes, "signUpgrade")
      addRecipe(Items.upgradeSolarGenerator.createItemStack(), recipes, "solarGeneratorUpgrade")

      if (OreDictionary.getOres("nuggetIron").contains(Items.ironNugget.createItemStack())) {
        addRecipe(Items.ironNugget.createItemStack(), recipes, "nuggetIron")
      }
      addRecipe(Items.cuttingWire.createItemStack(), recipes, "cuttingWire")
      addRecipe(Items.acid.createItemStack(), recipes, "acid")
      addRecipe(Items.disk.createItemStack(), recipes, "disk")

      addRecipe(Items.buttonGroup.createItemStack(), recipes, "buttonGroup")
      addRecipe(Items.arrowKeys.createItemStack(), recipes, "arrowKeys")
      addRecipe(Items.numPad.createItemStack(), recipes, "numPad")

      addRecipe(Items.transistor.createItemStack(), recipes, "transistor")
      addRecipe(Items.chip1.createItemStack(), recipes, "chip1")
      addRecipe(Items.chip2.createItemStack(), recipes, "chip2")
      addRecipe(Items.chip3.createItemStack(), recipes, "chip3")
      addRecipe(Items.alu.createItemStack(), recipes, "alu")
      addRecipe(Items.cpu.createItemStack(), recipes, "cpu")
      addRecipe(Items.cu.createItemStack(), recipes, "cu")

      addRecipe(Items.rawCircuitBoard.createItemStack(), recipes, "rawCircuitBoard")
      addRecipe(Items.circuitBoard.createItemStack(), recipes, "circuitBoard")
      addRecipe(Items.pcb.createItemStack(), recipes, "printedCircuitBoard")
      addRecipe(Items.card.createItemStack(), recipes, "card")

      // Try to keep this in the same order as the fields in the Blocks class
      // to make it easier to match them and check if anything is missing.
      addRecipe(Blocks.adapter.createItemStack(), recipes, "adapter")
      addRecipe(Blocks.cable.createItemStack(), recipes, "cable")
      addRecipe(Blocks.capacitor.createItemStack(), recipes, "capacitor")
      addRecipe(Blocks.charger.createItemStack(), recipes, "charger")
      addRecipe(Blocks.case1.createItemStack(), recipes, "case1")
      addRecipe(Blocks.case2.createItemStack(), recipes, "case2")
      addRecipe(Blocks.case3.createItemStack(), recipes, "case3")
      addRecipe(Blocks.diskDrive.createItemStack(), recipes, "diskDrive")
      addRecipe(Blocks.keyboard.createItemStack(), recipes, "keyboard")
      addRecipe(Blocks.powerConverter.createItemStack(), recipes, "powerConverter")
      addRecipe(Blocks.powerDistributor.createItemStack(), recipes, "powerDistributor")
      addRecipe(Blocks.redstone.createItemStack(), recipes, "redstone")
      addRecipe(Blocks.robotProxy.createItemStack(), recipes, "robot")
      addRecipe(Blocks.router.createItemStack(), recipes, "router")
      addRecipe(Blocks.screen1.createItemStack(), recipes, "screen1")
      addRecipe(Blocks.screen2.createItemStack(), recipes, "screen2")
      addRecipe(Blocks.screen3.createItemStack(), recipes, "screen3")

      // Navigation upgrade recrafting.
      GameRegistry.addRecipe(new ShapelessOreRecipe(Items.upgradeNavigation.createItemStack(), Items.upgradeNavigation.createItemStack(), new ItemStack(Item.map, 1, OreDictionary.WILDCARD_VALUE)))
    }
    catch {
      case e: Throwable => OpenComputers.log.log(Level.SEVERE, "Error parsing recipes, you may not be able to craft any items from this mod!", e)
    }
  }

  def addRecipe(output: ItemStack, list: Config, name: String) = try {
    if (list.hasPath(name)) {
      val recipe = list.getConfig(name)
      val recipeType = tryGetType(recipe)
      try {
        recipeType match {
          case "shaped" => addShapedRecipe(output, recipe)
          case "shapeless" => addShapelessRecipe(output, recipe)
          case "furnace" => addFurnaceRecipe(output, recipe)
          case "assembly" => addAssemblyRecipe(output, recipe)
          case other => OpenComputers.log.warning("Failed adding recipe for '" + name + "', you will not be able to craft this item! The error was: Invalid recipe type '" + other + "'.")
        }
      }
      catch {
        case e: RecipeException => OpenComputers.log.warning("Failed adding " + recipeType + " recipe for '" + name + "', you will not be able to craft this item! The error was: " + e.getMessage)
      }
    }
    else {
      OpenComputers.log.info("No recipe for '" + name + "', you will not be able to craft this item.")
    }
  }
  catch {
    case e: Throwable => OpenComputers.log.log(Level.SEVERE, "Failed adding recipe for '" + name + "', you will not be able to craft this item!", e)
  }

  private def addShapedRecipe(output: ItemStack, recipe: Config) {
    val rows = recipe.getList("input").unwrapped().map {
      case row: java.util.List[Object] => row.map(parseIngredient)
      case other => throw new RecipeException("Invalid row entry for shaped recipe (not a list: " + other + ").")
    }
    output.stackSize = tryGetCount(recipe)

    var number = -1
    var shape = ArrayBuffer.empty[String]
    val input = ArrayBuffer.empty[AnyRef]
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

    GameRegistry.addRecipe(new ShapedOreRecipe(output, shape ++ input: _*))
  }

  private def addShapelessRecipe(output: ItemStack, recipe: Config) {
    val input = recipe.getValue("input").unwrapped() match {
      case list: java.util.List[Object] => list.map(parseIngredient)
      case other => Seq(parseIngredient(other))
    }
    output.stackSize = tryGetCount(recipe)

    if (input.size > 0 && output.stackSize > 0) {
      GameRegistry.addRecipe(new ShapelessOreRecipe(output, input: _*))
    }
  }

  private def addAssemblyRecipe(output: ItemStack, recipe: Config) {
    val input = (recipe.getValue("input").unwrapped() match {
      case list: java.util.List[Object] => list.map(parseIngredient)
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
        FurnaceRecipes.smelting().addSmelting(stack.itemID, stack.getItemDamage, output, 0)
      case name: String =>
        for (stack <- OreDictionary.getOres(name)) {
          FurnaceRecipes.smelting().addSmelting(stack.itemID, stack.getItemDamage, output, 0)
        }
      case _ =>
    }
  }

  private def parseIngredient(entry: AnyRef) = entry match {
    case map: java.util.HashMap[String, _] =>
      if (map.contains("oreDict")) {
        map.get("oreDict") match {
          case value: String => value
          case other => throw new RecipeException("Invalid name in recipe (not a string: " + other + ").")
        }
      }
      else if (map.contains("item")) {
        map.get("item") match {
          case name: String =>
            Item.itemsList.find(itemNameEquals(_, name)) match {
              case Some(item) => new ItemStack(item, 1, tryGetId(map))
              case _ => throw new RecipeException("No item found with name '" + name + "'.")
            }
          case id: Number => new ItemStack(validateItemId(id), 1, tryGetId(map))
          case other => throw new RecipeException("Invalid item name in recipe (not a string: " + other + ").")
        }
      }
      else if (map.contains("block")) {
        map.get("block") match {
          case name: String =>
            Block.blocksList.find(blockNameEquals(_, name)) match {
              case Some(block) => new ItemStack(block, 1, tryGetId(map))
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
        Item.itemsList.find(itemNameEquals(_, name)) match {
          case Some(item) => new ItemStack(item, 1, 0)
          case _ => Block.blocksList.find(blockNameEquals(_, name)) match {
            case Some(block) => new ItemStack(block, 1, 0)
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

  private def tryGetId(ingredient: java.util.HashMap[String, _]): Int =
    if (ingredient.contains("subID")) ingredient.get("subID") match {
      case id: Number => id.intValue
      case "any" => 32767
      case id: String => Integer.valueOf(id)
      case _ => 0
    } else 0

  private def validateBlockId(id: Number) = {
    val index = id.intValue
    if (index < 1 || index >= Block.blocksList.length || Block.blocksList(index) == null) throw new RecipeException("Invalid block ID: " + index)
    Block.blocksList(index)
  }

  private def validateItemId(id: Number) = {
    val index = id.intValue
    if (index < 0 || index >= Item.itemsList.length || Item.itemsList(index) == null) throw new RecipeException("Invalid item ID: " + index)
    Item.itemsList(index)
  }

  private def cartesianProduct[T](xss: List[List[T]]): List[List[T]] = xss match {
    case Nil => List(Nil)
    case h :: t => for (xh <- h;
                        xt <- cartesianProduct(t)) yield xh :: xt
  }

  private class RecipeException(message: String) extends RuntimeException(message)

}

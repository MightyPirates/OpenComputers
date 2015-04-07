package li.cil.oc.common.recipe

import java.io.File
import java.io.FileReader

import com.typesafe.config._
import cpw.mods.fml.common.Loader
import cpw.mods.fml.common.registry.GameRegistry
import li.cil.oc._
import li.cil.oc.common.block.SimpleBlock
import li.cil.oc.common.init.Items
import li.cil.oc.common.item.Delegator
import li.cil.oc.common.item.SimpleItem
import li.cil.oc.common.item.data.PrintData
import li.cil.oc.integration.Mods
import li.cil.oc.integration.util.NEI
import li.cil.oc.util.Color
import net.minecraft.block.Block
import net.minecraft.item.Item
import net.minecraft.item.ItemBlock
import net.minecraft.item.ItemStack
import net.minecraft.item.crafting.FurnaceRecipes
import net.minecraft.nbt.NBTTagCompound
import net.minecraft.util.RegistryNamespaced
import net.minecraftforge.oredict.OreDictionary
import net.minecraftforge.oredict.RecipeSorter
import net.minecraftforge.oredict.RecipeSorter.Category
import org.apache.commons.io.FileUtils

import scala.collection.convert.WrapAsScala._
import scala.collection.mutable

object Recipes {
  val list = mutable.LinkedHashMap.empty[ItemStack, String]
  val oreDictEntries = mutable.LinkedHashMap.empty[String, ItemStack]
  var hadErrors = false

  def addBlock(instance: Block, name: String, oreDict: String = null) = {
    Items.registerBlock(instance, name)
    addRecipe(new ItemStack(instance), name)
    register(oreDict, instance match {
      case simple: SimpleBlock => simple.createItemStack()
      case _ => new ItemStack(instance)
    })
    instance
  }

  def addSubItem[T <: common.item.Delegate](delegate: T, name: String, oreDict: String = null) = {
    Items.registerItem(delegate, name)
    addRecipe(delegate.createItemStack(), name)
    register(oreDict, delegate.createItemStack())
    delegate
  }

  def addItem(instance: Item, name: String, oreDict: String = null) = {
    Items.registerItem(instance, name)
    addRecipe(new ItemStack(instance), name)
    register(oreDict, instance match {
      case simple: SimpleItem => simple.createItemStack()
      case _ => new ItemStack(instance)
    })
    instance
  }

  def addRecipe(stack: ItemStack, name: String) {
    list += stack -> name
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

      // Recrafting operations.
      val navigationUpgrade = api.Items.get(Constants.ItemName.NavigationUpgrade)
      val mcu = api.Items.get(Constants.BlockName.Microcontroller)
      val floppy = api.Items.get(Constants.ItemName.Floppy)
      val lootDisk = api.Items.get(Constants.ItemName.LootDisk)
      val drone = api.Items.get(Constants.ItemName.Drone)
      val eeprom = api.Items.get(Constants.ItemName.EEPROM)
      val robot = api.Items.get(Constants.BlockName.Robot)
      val tablet = api.Items.get(Constants.ItemName.Tablet)
      val chamelium = api.Items.get(Constants.ItemName.Chamelium)
      val chameliumBlock = api.Items.get(Constants.BlockName.ChameliumBlock)
      val print = api.Items.get(Constants.BlockName.Print)

      // Navigation upgrade recrafting.
      GameRegistry.addRecipe(new ExtendedShapelessOreRecipe(
        navigationUpgrade.createItemStack(1),
        navigationUpgrade.createItemStack(1), new ItemStack(net.minecraft.init.Items.filled_map, 1, OreDictionary.WILDCARD_VALUE)))

      // Floppy disk coloring.
      for (dye <- Color.dyes) {
        val result = floppy.createItemStack(1)
        val tag = new NBTTagCompound()
        tag.setInteger(Settings.namespace + "color", Color.dyes.indexOf(dye))
        result.setTagCompound(tag)
        GameRegistry.addRecipe(new ExtendedShapelessOreRecipe(result, floppy.createItemStack(1), dye))
      }

      // Microcontroller recrafting.
      GameRegistry.addRecipe(new ExtendedShapelessOreRecipe(
        mcu.createItemStack(1),
        mcu.createItemStack(1), eeprom.createItemStack(1)))

      // Drone recrafting.
      GameRegistry.addRecipe(new ExtendedShapelessOreRecipe(
        drone.createItemStack(1),
        drone.createItemStack(1), eeprom.createItemStack(1)))

      // EEPROM copying via crafting.
      GameRegistry.addRecipe(new ExtendedShapelessOreRecipe(
        eeprom.createItemStack(2),
        eeprom.createItemStack(1), eeprom.createItemStack(1)))

      // Robot recrafting.
      GameRegistry.addRecipe(new ExtendedShapelessOreRecipe(
        robot.createItemStack(1),
        robot.createItemStack(1), eeprom.createItemStack(1)))

      // Tablet recrafting.
      GameRegistry.addRecipe(new ExtendedShapelessOreRecipe(
        tablet.createItemStack(1),
        tablet.createItemStack(1), eeprom.createItemStack(1)))

      // Chamelium block splitting.
      GameRegistry.addRecipe(new ExtendedShapelessOreRecipe(
        chamelium.createItemStack(9),
        chameliumBlock.createItemStack(1)))

      // Chamelium dying.
      for ((dye, meta) <- Color.dyes.zipWithIndex) {
        val result = chameliumBlock.createItemStack(1)
        result.setItemDamage(meta)
        val input = chameliumBlock.createItemStack(1)
        input.setItemDamage(OreDictionary.WILDCARD_VALUE)
        GameRegistry.addRecipe(new ExtendedShapelessOreRecipe(
          result,
          input, dye))
      }

      // Print beaconification.
      val beaconPrint = print.createItemStack(1)

      {
        val printData = new PrintData(beaconPrint)
        printData.isBeaconBase = true
        printData.save(beaconPrint)
      }

      for (block <- Array(
        net.minecraft.init.Blocks.iron_block,
        net.minecraft.init.Blocks.gold_block,
        net.minecraft.init.Blocks.emerald_block,
        net.minecraft.init.Blocks.diamond_block
      )) {
        GameRegistry.addRecipe(new ExtendedShapelessOreRecipe(
          beaconPrint,
          print.createItemStack(1), new ItemStack(block)))
      }

      // Floppy disk formatting.
      GameRegistry.addRecipe(new ExtendedShapelessOreRecipe(floppy.createItemStack(1), floppy.createItemStack(1)))
      GameRegistry.addRecipe(new ExtendedShapelessOreRecipe(floppy.createItemStack(1), lootDisk.createItemStack(1)))

      // Hard disk formatting.
      val hdds = Array(
        api.Items.get(Constants.ItemName.HDDTier1),
        api.Items.get(Constants.ItemName.HDDTier2),
        api.Items.get(Constants.ItemName.HDDTier3)
      )
      for (hdd <- hdds) {
        GameRegistry.addRecipe(new ExtendedShapelessOreRecipe(hdd.createItemStack(1), hdd.createItemStack(1)))
      }

      // Print light value increments.
      val lightPrint = print.createItemStack(1)

      {
        val printData = new PrintData(lightPrint)
        printData.lightLevel = 1
        printData.save(lightPrint)
      }

      GameRegistry.addRecipe(new ExtendedShapelessOreRecipe(
        lightPrint,
        print.createItemStack(1), new ItemStack(net.minecraft.init.Items.glowstone_dust)))
    }
    catch {
      case e: Throwable => OpenComputers.log.error("Error parsing recipes, you may not be able to craft any items from this mod!", e)
    }
    list.clear()
  }

  private def addRecipe(output: ItemStack, list: Config, name: String) = try {
    if (list.hasPath(name)) {
      val value = list.getValue(name)
      value.valueType match {
        case ConfigValueType.OBJECT =>
          val recipe = list.getConfig(name)
          val recipeType = tryGetType(recipe)
          try {
            recipeType match {
              case "shaped" => addShapedRecipe(output, recipe)
              case "shapeless" => addShapelessRecipe(output, recipe)
              case "furnace" => addFurnaceRecipe(output, recipe)
              case "gt_assembler" =>
                if (Mods.GregTech.isAvailable) {
                  addGTAssemblingMachineRecipe(output, recipe)
                }
                else {
                  OpenComputers.log.error(s"Skipping GregTech assembler recipe for '$name' because GregTech is not present, you will not be able to craft this item.")
                  hadErrors = true
                }
              case other =>
                OpenComputers.log.error(s"Failed adding recipe for '$name', you will not be able to craft this item. The error was: Invalid recipe type '$other'.")
                hadErrors = true
            }
          }
          catch {
            case e: RecipeException =>
              OpenComputers.log.error(s"Failed adding $recipeType recipe for '$name', you will not be able to craft this item! The error was: ${e.getMessage}")
              hadErrors = true
          }
        case ConfigValueType.BOOLEAN =>
          // Explicitly disabled, keep in NEI if true.
          if (!value.unwrapped.asInstanceOf[Boolean]) {
            hide(output)
          }
        case _ =>
          OpenComputers.log.error(s"Failed adding recipe for '$name', you will not be able to craft this item. The error was: Invalid value for recipe.")
          hadErrors = true
      }
    }
    else {
      OpenComputers.log.warn(s"No recipe for '$name', you will not be able to craft this item. To suppress this warning, disable the recipe (assign `false` to it).")
      hadErrors = true
    }
  }
  catch {
    case e: Throwable =>
      OpenComputers.log.error(s"Failed adding recipe for '$name', you will not be able to craft this item.", e)
      hadErrors = true
  }

  private def addShapedRecipe(output: ItemStack, recipe: Config) {
    val rows = recipe.getList("input").unwrapped().map {
      case row: java.util.List[AnyRef]@unchecked => row.map(parseIngredient)
      case other => throw new RecipeException(s"Invalid row entry for shaped recipe (not a list: $other).")
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
  }

  private def addGTAssemblingMachineRecipe(output: ItemStack, recipe: Config) {
    val inputs = (recipe.getValue("input").unwrapped() match {
      case list: java.util.List[AnyRef]@unchecked => list.map(parseIngredient)
      case other => Seq(parseIngredient(other))
    }) map {
      case null => Array.empty[ItemStack]
      case stack: ItemStack => Array(stack)
      case name: String => Array(OreDictionary.getOres(name): _*)
      case other => throw new RecipeException(s"Invalid ingredient type: $other.")
    }
    output.stackSize = tryGetCount(recipe)

    if (inputs.size < 1 || inputs.size > 2) {
      throw new RecipeException(s"Invalid recipe length: ${inputs.size}, should be 1 or 2.")
    }

    val inputCount = recipe.getIntList("count")
    if (inputCount.size() != inputs.size) {
      throw new RecipeException(s"Ingredient and input count mismatch: ${inputs.size} != ${inputCount.size}.")
    }

    val eu = recipe.getInt("eu")
    val duration = recipe.getInt("time")

    (inputs, inputCount).zipped.foreach((stacks, count) => stacks.foreach(stack => if (stack != null && count > 0) stack.stackSize = stack.getMaxStackSize min count))
    inputs.padTo(2, null)

    if (inputs.head != null) {
      for (input1 <- inputs.head) {
        if (inputs.last != null) {
          for (input2 <- inputs.last)
            gregtech.api.GregTech_API.sRecipeAdder.addAssemblerRecipe(input1, input2, output, duration, eu)
        }
        else gregtech.api.GregTech_API.sRecipeAdder.addAssemblerRecipe(input1, null, output, duration, eu)
      }
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
          case other => throw new RecipeException(s"Invalid name in recipe (not a string: $other).")
        }
      }
      else if (map.contains("item")) {
        map.get("item") match {
          case name: String =>
            findItem(name) match {
              case Some(item: Item) => new ItemStack(item, 1, tryGetId(map))
              case _ => throw new RecipeException(s"No item found with name '$name'.")
            }
          case id: Number => new ItemStack(validateItemId(id), 1, tryGetId(map))
          case other => throw new RecipeException(s"Invalid item name in recipe (not a string: $other).")
        }
      }
      else if (map.contains("block")) {
        map.get("block") match {
          case name: String =>
            findBlock(name) match {
              case Some(block: Block) => new ItemStack(block, 1, tryGetId(map))
              case _ => throw new RecipeException(s"No block found with name '$name'.")
            }
          case id: Number => new ItemStack(validateBlockId(id), 1, tryGetId(map))
          case other => throw new RecipeException(s"Invalid block name (not a string: $other).")
        }
      }
      else throw new RecipeException("Invalid ingredient type (no oreDict, item or block entry).")
    case name: String =>
      if (name == null || name.trim.isEmpty) null
      else if (OreDictionary.getOres(name) != null && !OreDictionary.getOres(name).isEmpty) name
      else {
        findItem(name) match {
          case Some(item: Item) => new ItemStack(item, 1, 0)
          case _ =>
            findBlock(name) match {
              case Some(block: Block) => new ItemStack(block, 1, 0)
              case _ => throw new RecipeException(s"No ore dictionary entry, item or block found for ingredient with name '$name'.")
            }
        }
      }
    case other => throw new RecipeException(s"Invalid ingredient type (not a map or string): $other")
  }

  private def findItem(name: String) = getObjectWithoutFallback(Item.itemRegistry, name).orElse(Item.itemRegistry.find {
    case item: Item => item.getUnlocalizedName == name || item.getUnlocalizedName == "item." + name
    case _ => false
  })

  private def findBlock(name: String) = getObjectWithoutFallback(Block.blockRegistry, name).orElse(Block.blockRegistry.find {
    case block: Block => block.getUnlocalizedName == name || block.getUnlocalizedName == "tile." + name
    case _ => false
  })

  private def getObjectWithoutFallback(registry: RegistryNamespaced, key: String) =
    if (registry.containsKey(key)) Option(registry.getObject(key))
    else None

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
    if (block == null) throw new RecipeException(s"Invalid block ID: $index")
    block
  }

  private def validateItemId(id: Number) = {
    val index = id.intValue
    val item = Item.getItemById(index)
    if (item == null) throw new RecipeException(s"Invalid item ID: $index")
    item
  }

  private def hide(value: ItemStack) {
    Delegator.subItem(value) match {
      case Some(stack) => stack.showInItemList = false
      case _ => value.getItem match {
        case itemBlock: ItemBlock => itemBlock.field_150939_a match {
          case simple: SimpleBlock =>
            simple.setCreativeTab(null)
            NEI.hide(simple)
          case _ =>
        }
        case _ =>
      }
    }
  }

  private class RecipeException(message: String) extends RuntimeException(message)

}

package li.cil.oc.common.recipe

import java.io.File
import java.io.FileReader

import com.typesafe.config._
import cpw.mods.fml.common.Loader
import cpw.mods.fml.common.registry.GameRegistry
import li.cil.oc._
import li.cil.oc.common.Loot
import li.cil.oc.common.block.SimpleBlock
import li.cil.oc.common.init.Items
import li.cil.oc.common.item.Delegator
import li.cil.oc.common.item.data.PrintData
import li.cil.oc.common.item.traits.Delegate
import li.cil.oc.common.item.traits.SimpleItem
import li.cil.oc.integration.util.NEI
import li.cil.oc.util.Color
import net.minecraft.block.Block
import net.minecraft.item.Item
import net.minecraft.item.ItemBlock
import net.minecraft.item.ItemStack
import net.minecraft.nbt.NBTTagCompound
import net.minecraft.util.RegistryNamespaced
import net.minecraftforge.fluids.FluidRegistry
import net.minecraftforge.fluids.FluidStack
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
  val recipeHandlers = mutable.LinkedHashMap.empty[String, (ItemStack, Config) => Unit]

  def registerRecipeHandler(name: String, recipe: (ItemStack, Config) => Unit): Unit = {
    recipeHandlers += name -> recipe
  }

  def addBlock(instance: Block, name: String, oreDict: String*) = {
    Items.registerBlock(instance, name)
    addRecipe(new ItemStack(instance), name)
    register(instance match {
      case simple: SimpleBlock => simple.createItemStack()
      case _ => new ItemStack(instance)
    }, oreDict: _*)
    instance
  }

  def addSubItem[T <: Delegate](delegate: T, name: String, oreDict: String*) = {
    Items.registerItem(delegate, name)
    addRecipe(delegate.createItemStack(), name)
    register(delegate.createItemStack(), oreDict: _*)
    delegate
  }

  def addItem(instance: Item, name: String, oreDict: String*) = {
    Items.registerItem(instance, name)
    addRecipe(new ItemStack(instance), name)
    register(instance match {
      case simple: SimpleItem => simple.createItemStack()
      case _ => new ItemStack(instance)
    }, oreDict: _*)
    instance
  }

  def addStack(stack: ItemStack, name: String, oreDict: String*) = {
    Items.registerStack(stack, name)
    addRecipe(stack, name)
    register(stack, oreDict: _*)
    stack
  }

  def addRecipe(stack: ItemStack, name: String) {
    list += stack -> name
  }

  private def register(item: ItemStack, names: String*) {
    for (name <- names if name != null) {
      oreDictEntries += name -> item
    }
  }

  def init() {
    RecipeSorter.register(Settings.namespace + "extshaped", classOf[ExtendedShapedOreRecipe], Category.SHAPED, "after:forge:shapedore")
    RecipeSorter.register(Settings.namespace + "extshapeless", classOf[ExtendedShapelessOreRecipe], Category.SHAPELESS, "after:forge:shapelessore")
    RecipeSorter.register(Settings.namespace + "colorizer", classOf[ColorizeRecipe], Category.SHAPELESS, "after:forge:shapelessore")
    RecipeSorter.register(Settings.namespace + "decolorizer", classOf[DecolorizeRecipe], Category.SHAPELESS, "after:oc:colorizer")

    for ((name, stack) <- oreDictEntries) {
      if (!OreDictionary.getOres(name).contains(stack)) {
        OreDictionary.registerOre(name, stack)
      }
    }
    oreDictEntries.clear()

    try {
      val recipeSets = Array("default", "hardmode", "gregtech", "peaceful")
      val recipeDirectory = new File(Loader.instance.getConfigDir + File.separator + "opencomputers")
      val userRecipes = new File(recipeDirectory, "user.recipes")
      userRecipes.getParentFile.mkdirs()
      if (!userRecipes.exists()) {
        FileUtils.copyURLToFile(getClass.getResource("/assets/opencomputers/recipes/user.recipes"), userRecipes)
      }
      for (recipeSet <- recipeSets) {
        FileUtils.copyURLToFile(getClass.getResource(s"/assets/opencomputers/recipes/$recipeSet.recipes"), new File(recipeDirectory, s"$recipeSet.recipes"))
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
        if (recipes.hasPath(name)) {
          val value = recipes.getValue(name)
          value.valueType match {
            case ConfigValueType.OBJECT =>
              addRecipe(stack, recipes.getConfig(name), s"'$name'")
            case ConfigValueType.BOOLEAN =>
              // Explicitly disabled, keep in NEI if true.
              if (!value.unwrapped.asInstanceOf[Boolean]) {
                hide(stack)
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

      // Register all unknown recipes. Well. Loot disk recipes.
      if (recipes.hasPath("lootDisks")) try {
        val lootRecipes = recipes.getConfigList("lootDisks")
        for (recipe <- lootRecipes) {
          val name = recipe.getString("name")
          Loot.globalDisks.get(name) match {
            case Some((stack, _)) => addRecipe(stack, recipe, s"loot disk '$name'")
            case _ =>
              OpenComputers.log.warn(s"Failed adding recipe for loot disk '$name': No such global loot disk.")
              hadErrors = true
          }
        }
      }
      catch {
        case t: Throwable =>
          OpenComputers.log.warn("Failed parsing loot disk recipes.", t)
          hadErrors = true
      }

      if (recipes.hasPath("generic")) try {
        val genericRecipes = recipes.getConfigList("generic")
        for (recipe <- genericRecipes) {
          val result = recipe.getValue("result").unwrapped()
          parseIngredient(result) match {
            case stack: ItemStack => addRecipe(stack, recipe, s"'$result'")
            case _ =>
              OpenComputers.log.warn(s"Failed adding generic recipe for '$result': Invalid output (make sure it's not an OreDictionary name).")
              hadErrors = true
          }
        }
      }
      catch {
        case t: Throwable =>
          OpenComputers.log.warn("Failed parsing generic recipes.", t)
          hadErrors = true
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

      // EEPROM formatting.
      GameRegistry.addRecipe(new ExtendedShapelessOreRecipe(eeprom.createItemStack(1), eeprom.createItemStack(1)))

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

      {
        val printData = new PrintData(lightPrint)
        printData.lightLevel = 4
        printData.save(lightPrint)
      }

      GameRegistry.addRecipe(new ExtendedShapelessOreRecipe(
        lightPrint,
        print.createItemStack(1), new ItemStack(net.minecraft.init.Blocks.glowstone)))

      // Switch/AccessPoint -> Relay conversion
      GameRegistry.addShapelessRecipe(api.Items.get(Constants.BlockName.Relay).createItemStack(1),
        api.Items.get(Constants.BlockName.AccessPoint).createItemStack(1))
      GameRegistry.addShapelessRecipe(api.Items.get(Constants.BlockName.Relay).createItemStack(1),
        api.Items.get(Constants.BlockName.Switch).createItemStack(1))

      // Hover Boot dyeing
      GameRegistry.addRecipe(new ColorizeRecipe(api.Items.get(Constants.ItemName.HoverBoots).item()))
      GameRegistry.addRecipe(new DecolorizeRecipe(api.Items.get(Constants.ItemName.HoverBoots).item()))
    }
    catch {
      case e: Throwable => OpenComputers.log.error("Error parsing recipes, you may not be able to craft any items from this mod!", e)
    }
    list.clear()
  }

  private def addRecipe(output: ItemStack, recipe: Config, name: String) = try {
    val recipeType = tryGetType(recipe)
    recipeHandlers.get(recipeType) match {
      case Some(recipeHandler) => recipeHandler(output, recipe)
      case _ =>
        OpenComputers.log.error(s"Failed adding recipe for $name, you will not be able to craft this item. The error was: Invalid recipe type '$recipeType'.")
        hadErrors = true
    }
  }
  catch {
    case e: RecipeException =>
      OpenComputers.log.error(s"Failed adding recipe for $name, you will not be able to craft this item.", e)
      hadErrors = true
  }

  def tryGetCount(recipe: Config) = if (recipe.hasPath("output")) recipe.getInt("output") else 1

  def parseIngredient(entry: AnyRef): AnyRef = entry match {
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

  def parseFluidIngredient(entry: Config): Option[FluidStack] = {
    val fluid = FluidRegistry.getFluid(entry.getString("name"))
    val amount =
      if (entry.hasPath("amount")) entry.getInt("amount")
      else 1000
    Option(new FluidStack(fluid, amount))
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

  class RecipeException(message: String) extends RuntimeException(message)

}

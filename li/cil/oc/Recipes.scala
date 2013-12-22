package li.cil.oc

import cpw.mods.fml.common.Loader
import cpw.mods.fml.common.registry.GameRegistry
import net.minecraft.block.Block
import net.minecraft.item.crafting.FurnaceRecipes
import net.minecraft.item.{ItemStack, Item}
import net.minecraftforge.oredict.{OreDictionary, ShapelessOreRecipe, ShapedOreRecipe}
import gregtechmod.api.GregTech_API
import scala.collection.convert.wrapAsScala._
import com.typesafe.config.{ConfigValueType, Config, ConfigFactory}
import java.io.{IOException, FileOutputStream, File}
import scala.collection.mutable.ListBuffer
import scala.collection.mutable
import java.util
import org.apache.commons.io.FileUtils
import java.net.URL

object Recipes {
  def init() {

    val custom = ConfigFactory.parseResources("/assets/opencomputers/recipes/custom.conf")
    try {
      val defaultFile = new File(Loader.instance.getConfigDir + File.separator + "opencomputers" + File.separator + "default.conf")
      val customFile = new File(Loader.instance.getConfigDir + File.separator + "opencomputers" + File.separator + "custom.conf")
      defaultFile.getParentFile.mkdirs()
      var in = Recipes.getClass.getResource("/assets/opencomputers/recipes/default.conf")
      inputToFile(in, defaultFile)
      if (!customFile.exists()) {
        //debug
        customFile.getParentFile.mkdirs()
        in = Recipes.getClass.getResource("/assets/opencomputers/recipes/custom.conf")
        inputToFile(in, customFile)
      }
      val config = ConfigFactory.parseFile(customFile).withFallback(custom)


      val diskList = config.getConfig("disk")


      loadRecipe(config.getConfig("locator"), Items.locator.createItemStack())
      loadRecipe(config.getConfig("test"), Items.locator.createItemStack())
    }
  }

  def loadRecipe(conf: Config, result: ItemStack) {
    conf.getString("type") match {
      case "shaped" => addShapedRecipe(result, conf)
      case "shapeless" => addShapelessRecipe(result, conf)
      case "assembler" =>
      case _ => throw new Exception("Don't know what to do with " + conf.getString("type"))

    }

  }

  private def addShapedRecipe(output: ItemStack, conf: Config) {
    val out = output.copy()
    var input = new ListBuffer[Object]()
    var recipe = new ListBuffer[String]
    var ch = 65
    //loop through recipe and get type of entry
    for (item <- conf.getList("recipe")) {
      {
        var line = ""
        item.unwrapped() match {
          //got list so we expect a entry with subid
          case list: java.util.List[Object] => {
            var obj: Object = null
            var itemSubID: Int = 0
            for (entry <- list) {
              entry match {
                //found hash value check for type of value
                case map: java.util.HashMap[String, Object] => {
                  for (entrySet <- map.entrySet()) {
                    entrySet.getKey match {
                      case "oreDict" => {
                        entrySet.getValue match {
                          case value: String => obj = value
                          case _ => throw new Exception("This case is not implemented please try adding a different way ore report the recipe you tried to add. " + entrySet)
                        }
                      }
                      case "item" => {
                        entrySet.getValue match {
                          case value: String => obj = getItemFromName(value)
                          case _ => throw new Exception("This case is not implemented please try adding a different way ore report the recipe you tried to add. " + entrySet)
                        }
                      }
                      case "block" =>
                        entrySet.getValue match {
                          case value: String => obj = getBlockFromName(value)
                          case _ => throw new Exception("This case is not implemented please try adding a different way ore report the recipe you tried to add. " + entrySet)
                        }

                      case "subID" => {
                        entrySet.getValue match {
                          case value: Integer => itemSubID = value
                          case value: String => itemSubID = Integer.valueOf(value)
                          case _ => throw new Exception("This case is not implemented please try adding a different way ore report the recipe you tried to add. " + entrySet)
                        }
                      }
                      case _ => throw new Exception("This case is not implemented please try adding a different way ore report the recipe you tried to add. " + entrySet)
                    }
                  }
                }
                //only found a string so just add this
                case value: String => obj = getNameOrStackFromName(value)
              }
              line += ch.toChar

              //alright now add recipe to list, with subid if found
              obj match {
                case item: Item => {

                  input += (ch.toChar: Character)
                  input += new ItemStack(item, 1, itemSubID)

                }
                case block: Block => {
                  input += (ch.toChar: Character)
                  input += new ItemStack(block, 1, itemSubID)
                }
                case value: String => {
                  input += (ch.toChar: Character)
                  input += value
                }
                case null=>
              }

              ch += 1
            }

          }

          //          case list: java.util.HashMap[String, Object] => {
          //            for (entrySet <- list.entrySet()) {
          //              entrySet.getKey match {
          //                case "oreDict" => {
          //                  entrySet.getValue match {
          //                    case value: String => input += value
          //                    case _ => throw new Exception("This case is not implemented please try adding a different way ore report the recipe you tried to add. " + entrySet)
          //                  }
          //                }
          //                case "item" => {
          //                  entrySet.getValue match {
          //                    case value: String => input += getItemFromName(value)
          //                    case _ => throw new Exception("This case is not implemented please try adding a different way ore report the recipe you tried to add. " + entrySet)
          //                  }
          //                }
          //                case "block" =>
          //                  entrySet.getValue match {
          //                    case value: String => input += getBlockFromName(value)
          //                    case _ => throw new Exception("This case is not implemented please try adding a different way ore report the recipe you tried to add. " + entrySet)
          //                  }
          //              }
          //            }
          //          }
          //          case value: String => input += getNameOrStackFromName(value)
          //          case _ => println(item.unwrapped())
        }
        recipe += line

      }
    }
    //try to get number of outputted stacks, ignore if not present
    try {
      out.stackSize = conf.getInt("result")
    } catch {
      case _: Throwable => // ignore
    }
    GameRegistry.addRecipe(new ShapedOreRecipe(out, (recipe ++ input): _*))
  }

  private def addShapelessRecipe(output: ItemStack, conf: Config) {
    var out = output.copy()
    var input = new ListBuffer[Object]()

    //loop through recipe and get type of entry
    for (item <- conf.getList("recipe")) {
      {
        item.unwrapped() match {
          //got list so we expect a entry with subid
          case list: java.util.List[Object] => {
            var obj: Object = null
            var itemSubID: Int = 0
            for (entry <- list) {
              entry match {
                //found hash value check for type of value
                case map: java.util.HashMap[String, Object] => {
                  for (entrySet <- map.entrySet()) {
                    entrySet.getKey match {
                      case "oreDict" => {
                        entrySet.getValue match {
                          case value: String => obj = value
                          case _ => throw new Exception("This case is not implemented please try adding a different way ore report the recipe you tried to add. " + entrySet)
                        }
                      }
                      case "item" => {
                        entrySet.getValue match {
                          case value: String => obj = getItemFromName(value)
                          case _ => throw new Exception("This case is not implemented please try adding a different way ore report the recipe you tried to add. " + entrySet)
                        }
                      }
                      case "block" =>
                        entrySet.getValue match {
                          case value: String => obj = getBlockFromName(value)
                          case _ => throw new Exception("This case is not implemented please try adding a different way ore report the recipe you tried to add. " + entrySet)
                        }

                      case "subID" => {
                        entrySet.getValue match {
                          case value: Integer => itemSubID = value
                          case value: String => itemSubID = Integer.valueOf(value)
                          case _ => throw new Exception("This case is not implemented please try adding a different way ore report the recipe you tried to add. " + entrySet)
                        }
                      }
                    }
                  }
                }
                //only found a string so just add this
                case value: String => obj = getNameOrStackFromName(value)
              }
            }
            //alright now add recipe to list, with subid if found
            obj match {
              case item: Item => input += new ItemStack(item, 1, itemSubID)
              case block: Block => input += new ItemStack(block, 1, itemSubID)
              case value: String => input += value
            }

          }

          case list: java.util.HashMap[String, Object] => {
            for (entrySet <- list.entrySet()) {
              entrySet.getKey match {
                case "oreDict" => {
                  entrySet.getValue match {
                    case value: String => input += value
                    case _ => throw new Exception("This case is not implemented please try adding a different way ore report the recipe you tried to add. " + entrySet)
                  }
                }
                case "item" => {
                  entrySet.getValue match {
                    case value: String => input += getItemFromName(value)
                    case _ => throw new Exception("This case is not implemented please try adding a different way ore report the recipe you tried to add. " + entrySet)
                  }
                }
                case "block" =>
                  entrySet.getValue match {
                    case value: String => input += getBlockFromName(value)
                    case _ => throw new Exception("This case is not implemented please try adding a different way ore report the recipe you tried to add. " + entrySet)
                  }
              }
            }
          }
          case value: String => input += getNameOrStackFromName(value)
          case _ => println(item.unwrapped())
        }

      }
    }
    //try to get number of outputted stacks, ignore if not present
    try {
      out.stackSize = conf.getInt("result")
    } catch {
      case _: Throwable => // ignore
    }


    GameRegistry.addRecipe(new ShapelessOreRecipe(out, input: _*))
  }

  private def addShapelessRecipe(output: ItemStack, params: List[String]) {
    GameRegistry.addRecipe(new ShapelessOreRecipe(output, params.map(getNameOrStackFromName): _*))
  }

  private def addShapedRecipe(output: ItemStack, params: List[String]) = {

    params match {
      case List(a, b, c) => {

        GameRegistry.addRecipe(new ShapedOreRecipe(output,
          Seq("abc") ++ List(
            ('a': Character, getNameOrStackFromName(a)),
            ('b': Character, getNameOrStackFromName(b)),
            ('c': Character, getNameOrStackFromName(c))
          ).collect {
            case (a, b: String) if !b.isEmpty => Seq(a, b)
            case (a, b@(_: Item | _: Block | _: ItemStack)) => Seq(a, b)
          }.flatten: _*))

      }
      case List(a, b, c, d, e, f) => GameRegistry.addRecipe(new ShapedOreRecipe(output,
        Seq("abc",
          "def") ++ List(
          ('a': Character, getNameOrStackFromName(a)),
          ('b': Character, getNameOrStackFromName(b)),
          ('c': Character, getNameOrStackFromName(c)),
          ('d': Character, getNameOrStackFromName(d)),
          ('e': Character, getNameOrStackFromName(e)),
          ('f': Character, getNameOrStackFromName(f))
        ).collect {
          case (a, b: String) if !b.isEmpty => Seq(a, b)
          case (a, b@(_: Item | _: Block | _: ItemStack)) => Seq(a, b)
        }.flatten: _*))
      case List(a, b, c, d, e, f, g, h, i) => {

        GameRegistry.addRecipe(new ShapedOreRecipe(output,
          Seq("abc",
            "def",
            "ghi") ++ List(
            ('a': Character, getNameOrStackFromName(a)),
            ('b': Character, getNameOrStackFromName(b)),
            ('c': Character, getNameOrStackFromName(c)),
            ('d': Character, getNameOrStackFromName(d)),
            ('e': Character, getNameOrStackFromName(e)),
            ('f': Character, getNameOrStackFromName(f)),
            ('g': Character, getNameOrStackFromName(g)),
            ('h': Character, getNameOrStackFromName(h)),
            ('i': Character, getNameOrStackFromName(i))
          ).collect {
            case (a, b: String) if !b.isEmpty => Seq(a, b)
            case (a, b@(_: Item | _: Block | _: ItemStack)) => Seq(a, b)
          }.flatten: _*
        )
        )
      }
      case _ => OpenComputers.log.warning("invalid recipe for item " + output)
    }


  }

  private def addAssemblingMachineRecipe(output: ItemStack, input1: String, input2: String, count: List[Int], duration: Int, eu: Int) {


    for (items <- cartesianProduct(List(OreDictionary.getOres(input1).toList, OreDictionary.getOres(input2).toList))) {
      val stacks = (items, count).zipped.toList

      var (itemCopy, amount) = stacks(0)
      itemCopy = itemCopy.copy()
      itemCopy.stackSize = amount

      var (itemCopy2, amount2) = stacks(1)
      itemCopy2 = itemCopy2.copy()
      itemCopy2.stackSize = amount2
      GregTech_API.sRecipeAdder.addAssemblerRecipe(output, itemCopy, itemCopy2, duration, eu)


    }

  }

  val blazeRod = new ItemStack(Item.blazeRod)
  val boneMeal = new ItemStack(Item.dyePowder, 1, 15)
  val cactusGreen = new ItemStack(Item.dyePowder, 1, 2)
  val clock = new ItemStack(Item.pocketSundial)
  val comparator = new ItemStack(Item.comparator)
  val craftingTable = new ItemStack(Block.workbench)
  val diamond = new ItemStack(Item.diamond)
  val dispenser = new ItemStack(Block.dispenser)
  val emerald = new ItemStack(Item.emerald)
  val enderPearl = new ItemStack(Item.enderPearl)
  val glass = new ItemStack(Block.glass)
  val glowstoneDust = new ItemStack(Item.glowstone)
  val goldIngot = new ItemStack(Item.ingotGold)
  val goldNugget = new ItemStack(Item.goldNugget)
  val ironBars = new ItemStack(Block.fenceIron)
  val ironIngot = new ItemStack(Item.ingotIron)
  val lapis = new ItemStack(Item.dyePowder, 1, 4)
  val lever = new ItemStack(Block.lever)
  val minecartHopper = new ItemStack(Item.minecartHopper)
  val netherQuartz = new ItemStack(Item.netherQuartz)
  val obsidian = new ItemStack(Block.obsidian)
  val paper = new ItemStack(Item.paper)
  val piston = new ItemStack(Block.pistonBase)
  val redstoneBlock = new ItemStack(Block.blockRedstone)
  val redstoneDust = new ItemStack(Item.redstone)
  val redstoneTorch = new ItemStack(Block.torchRedstoneActive)
  val repeater = new ItemStack(Item.redstoneRepeater)
  val roseRed = new ItemStack(Item.dyePowder, 1, 1)
  val slimeBall = new ItemStack(Item.slimeBall)
  val spiderEye = new ItemStack(Item.spiderEye)
  val stick = new ItemStack(Item.stick)
  val sugar = new ItemStack(Item.sugar)

  val acid = Items.acid.createItemStack()
  val alu = Items.alu.createItemStack()
  val cable = Blocks.cable.createItemStack()
  val card = Items.card.createItemStack()
  val chip1 = Items.chip1.createItemStack()
  val chip2 = Items.chip2.createItemStack()
  val chip3 = Items.chip3.createItemStack()
  val board = Items.circuitBoard.createItemStack()
  val cpu = Items.cpu.createItemStack()
  val cu = Items.cu.createItemStack()
  val disk = Items.disk.createItemStack()
  val floppy = Items.floppyDisk.createItemStack()
  val gpu1 = Items.gpu1.createItemStack()
  val gpu2 = Items.gpu2.createItemStack()
  val gpu3 = Items.gpu3.createItemStack()
  val hdd1 = Items.hdd1.createItemStack()
  val hdd2 = Items.hdd2.createItemStack()
  val hdd3 = Items.hdd3.createItemStack()
  val lanCard = Items.lan.createItemStack()
  val pcb = Items.pcb.createItemStack()
  val ram1 = Items.ram1.createItemStack()
  val ram2 = Items.ram2.createItemStack()
  val ram3 = Items.ram3.createItemStack()
  val rawBoard = Items.rawCircuitBoard.createItemStack()
  val redstoneCard = Items.rs.createItemStack()
  val transistor = Items.transistor.createItemStack()
  val wlanCard = Items.wlan.createItemStack()

  // ----------------------------------------------------------------------- //

  if (Settings.get.dynamicRecipes) {
    val existsAluminum = !OreDictionary.getOres("plateAluminium").isEmpty
    var m: Map[String, Array[Any]] = Map()
    if (existsAluminum) {
      //        m += ("disk" ->(disk,
      //          " A ",
      //          "A A",
      //          " A ",
      //          'A', "plateAluminium"))
    } else {
      //standard
    }
    if (Loader.isModLoaded("gregtech_addon")) {
      for (plate <- OreDictionary.getOres("plateIron")) {
        val plateCopy = plate.copy()
        plateCopy.stackSize = 2
        GregTech_API.sRecipeAdder.addAssemblerRecipe(new ItemStack(Item.redstone, 1), plateCopy, Items.transistor.createItemStack(4), 500, 16)
      }

      GregTech_API.sRecipeAdder.addAssemblerRecipe(new ItemStack(Item.comparator, 3), chip1, alu, 500, 24)

      GregTech_API.sRecipeAdder.addAssemblerRecipe(Items.transistor.createItemStack(6), chip1, Items.cu.createItemStack(3), 750, 32)
      //
    }



    //
    //      "cu" ->(cu,
    //        "gtg",
    //        "scs",
    //        "gdg",
    //        'g', goldNugget,
    //        't', redstoneTorch,
    //        's', transistor,
    //        'c', clock,
    //        'd', redstoneDust),
    //
    //      "cpu" ->(cpu,
    //        "cdc",
    //        "bub",
    //        "cac",
    //        'c', chip1,
    //        'd', redstoneDust,
    //        'b', ironBars,
    //        'u', cu,
    //        'a', alu),
    //
    //      "card" ->(card,
    //        "ict",
    //        "ibb",
    //        "igg",
    //        'i', "nuggetIron",
    //        'c', chip1,
    //        't', transistor,
    //        'b', pcb,
    //        'g', goldNugget),
    //
    //      "buttonGroup" ->(Items.buttonGroup.createItemStack(),
    //        "bbb",
    //        "bbb",
    //        'b', new ItemStack(Block.stoneButton)),
    //
    //      "arrowKeys" ->(Items.arrowKeys.createItemStack(),
    //        " b ",
    //        "bbb",
    //        'b', new ItemStack(Block.stoneButton)),
    //
    //      "numPad" ->(Items.numPad.createItemStack(),
    //        "bbb",
    //        "bbb",
    //        "bbb",
    //        'b', new ItemStack(Block.stoneButton)),
    //
    //      // ----------------------------------------------------------------------- //
    //
    //      "case1" ->(Blocks.case1.createItemStack(),
    //        "ipi",
    //        "bcb",
    //        "imi",
    //        'i', ironIngot,
    //        'p', pcb,
    //        'b', ironBars,
    //        'c', cpu,
    //        'm', chip1),
    //
    //      "case2" ->(Blocks.case2.createItemStack(),
    //        "gpg",
    //        "mcm",
    //        "gpg",
    //        'g', goldIngot,
    //        'p', pcb,
    //        'm', chip2,
    //        'c', Blocks.case1.createItemStack()),
    //
    //      "case3" ->(Blocks.case3.createItemStack(),
    //        "mpm",
    //        "dcd",
    //        "mpm",
    //        'm', chip3,
    //        'p', pcb,
    //        'd', diamond,
    //        'c', Blocks.case2.createItemStack()),
    //
    //      "screen1" ->(Blocks.screen1.createItemStack(),
    //        "iig",
    //        "rtg",
    //        "iig",
    //        'i', ironIngot,
    //        'g', glass,
    //        'r', redstoneDust,
    //        't', transistor),
    //
    //      "screen2" ->(Blocks.screen2.createItemStack(),
    //        "iri",
    //        "cgs",
    //        "ibi",
    //        'i', goldIngot,
    //        'r', roseRed,
    //        'c', chip2,
    //        'g', cactusGreen,
    //        's', Blocks.screen1.createItemStack(),
    //        'b', lapis),
    //
    //      "screen3" ->(Blocks.screen3.createItemStack(),
    //        "opc",
    //        "bqs",
    //        "opc",
    //        'o', obsidian,
    //        'p', pcb,
    //        'c', chip3,
    //        'b', blazeRod,
    //        'q', netherQuartz,
    //        's', Blocks.screen2.createItemStack()),
    //
    //      "capacitor" ->(Blocks.capacitor.createItemStack(),
    //        "iti",
    //        "gpg",
    //        "ibi",
    //        'i', ironIngot,
    //        't', transistor,
    //        'g', goldNugget,
    //        'p', paper,
    //        'b', pcb),
    //
    //      "powerConverter" ->(Blocks.powerConverter.createItemStack(),
    //        "iwi",
    //        "gcg",
    //        "ibi",
    //        'i', ironIngot,
    //        'c', chip1,
    //        'w', cable,
    //        'g', goldIngot,
    //        'b', pcb),
    //
    //      "diskDrive" ->(Blocks.diskDrive.createItemStack(),
    //        "ici",
    //        "ps ",
    //        "ici",
    //        'i', ironIngot,
    //        'c', chip1,
    //        'p', piston,
    //        's', stick),
    //
    //      "adapter" ->(Blocks.adapter.createItemStack(),
    //        "iwi",
    //        "wcw",
    //        "ibi",
    //        'i', ironIngot,
    //        'w', cable,
    //        'c', chip1,
    //        'b', pcb),
    //
    //      "redstone" ->(Blocks.redstone.createItemStack(),
    //        "iri",
    //        "rcr",
    //        "ibi",
    //        'i', ironIngot,
    //        'r', redstoneBlock,
    //        'c', redstoneCard,
    //        'b', pcb),
    //
    //      "powerDistributor" ->(Blocks.powerDistributor.createItemStack(),
    //        "igi",
    //        "wcw",
    //        "ibi",
    //        'i', ironIngot,
    //        'g', goldIngot,
    //        'w', cable,
    //        'c', chip1,
    //        'b', pcb),
    //
    //      "router" ->(Blocks.router.createItemStack(),
    //        "iwi",
    //        "wnw",
    //        "ibi",
    //        'i', ironIngot,
    //        'w', cable,
    //        'n', lanCard,
    //        'b', pcb),
    //
    //      "charger" ->(Blocks.charger.createItemStack(),
    //        "igi",
    //        "pcp",
    //        "ibi",
    //        'i', ironIngot,
    //        'g', goldIngot,
    //        'p', Blocks.capacitor.createItemStack(),
    //        'c', chip2,
    //        'b', pcb),
    //
    //      "robotProxy" ->(Blocks.robotProxy.createItemStack(),
    //        "sgf",
    //        "dcr",
    //        "bmb",
    //        's', Blocks.screen1.createItemStack(),
    //        'g', gpu1,
    //        'f', Blocks.diskDrive.createItemStack(),
    //        'd', dispenser,
    //        'c', Blocks.case1.createItemStack(),
    //        'r', ram1,
    //        'b', Blocks.capacitor.createItemStack(),
    //        'm', minecartHopper),
    //
    //      "keyboard" ->(Blocks.keyboard.createItemStack(),
    //        "ggg",
    //        "gan",
    //        'g', Items.buttonGroup.createItemStack(),
    //        'a', Items.arrowKeys.createItemStack(),
    //        'n', Items.numPad.createItemStack()),
    //
    //      "cable" ->(Blocks.cable.createItemStack(4),
    //        " i ",
    //        "iri",
    //        " i ",
    //        'i', "nuggetIron",
    //        'r', redstoneDust),
    //
    //      // ----------------------------------------------------------------------- //
    //
    //      "cuttingWire" ->(Items.cuttingWire.createItemStack(),
    //        "sis",
    //        's', stick,
    //        'i', "nuggetIron"),
    //
    //      "analyzer" ->(Items.analyzer.createItemStack(),
    //        " r ",
    //        "tcg",
    //        "tpg",
    //        'r', redstoneTorch,
    //        't', transistor,
    //        'c', chip1,
    //        'g', goldNugget,
    //        'p', pcb),
    //
    //      "ram1" ->(ram1,
    //        "ccc",
    //        "bbb",
    //        'c', chip1,
    //        'b', pcb),
    //
    //      "ram2" ->(ram2,
    //        "ccc",
    //        "rbr",
    //        'c', chip2,
    //        'r', ram1,
    //        'b', pcb),
    //
    //      "ram3" ->(ram3,
    //        "ccc",
    //        "rbr",
    //        'c', chip3,
    //        'r', ram2,
    //        'b', pcb),
    //
    //      "floppy" ->(floppy,
    //        "ili",
    //        "bdb",
    //        "ipi",
    //        'i', "nuggetIron",
    //        'l', lever,
    //        'b', board,
    //        'd', disk,
    //        'p', paper),
    //
    //      "hdd1" ->(hdd1,
    //        "cdi",
    //        "bdp",
    //        "cdi",
    //        'c', chip1,
    //        'd', disk,
    //        'i', ironIngot,
    //        'b', pcb,
    //        'p', piston),
    //
    //      "hdd2" ->(hdd2,
    //        "gdg",
    //        "cbc",
    //        "gdg",
    //        'g', goldIngot,
    //        'd', hdd1,
    //        'c', chip2,
    //        'b', pcb),
    //
    //      "hdd3" ->(hdd3,
    //        "cdc",
    //        "rbr",
    //        "cdc",
    //        'c', chip3,
    //        'd', hdd2,
    //        'r', ram1,
    //        'b', pcb),
    //
    //      "gpu1" ->(gpu1,
    //        "car",
    //        " b ",
    //        'c', chip1,
    //        'a', alu,
    //        'r', ram1,
    //        'b', card),
    //
    //      "gpu2" ->(gpu2,
    //        "ccr",
    //        " g ",
    //        'c', chip2,
    //        'r', ram2,
    //        'g', gpu1),
    //
    //      "gpu3" ->(gpu3,
    //        "ccr",
    //        " g ",
    //        'c', chip3,
    //        'r', ram3,
    //        'g', gpu2),
    //
    //      "redstoneCard" ->(redstoneCard,
    //        "tc ",
    //        " b ",
    //        't', redstoneTorch,
    //        'c', chip1,
    //        'b', card),
    //
    //      "lanCard" ->(lanCard,
    //        "wc ",
    //        " b ",
    //        'w', cable,
    //        'c', chip1,
    //        'b', card),
    //
    //      "wlanCard" ->(wlanCard,
    //        "pc ",
    //        " b ",
    //        'p', enderPearl,
    //        'c', chip2,
    //        'b', lanCard),
    //
    //      "generator" ->(Items.generator.createItemStack(),
    //        "i i",
    //        "cpc",
    //        "bib",
    //        'i', ironIngot,
    //        'c', chip1,
    //        'p', piston,
    //        'b', pcb),
    //
    //      "crafting" ->(Items.crafting.createItemStack(),
    //        "ipi",
    //        "cwc",
    //        "ibi",
    //        'i', ironIngot,
    //        'p', piston,
    //        'c', chip1,
    //        'w', craftingTable,
    //        'b', pcb)
    //
    //      )


  }
  else {
    //use vanilla
  }


  //vanilla
  val standardRecipes = Map(
    "ironIngot" ->(ironIngot,
      "xxx",
      "xxx",
      "xxx",
      'x', "nuggetIron"),
    "disk" ->(disk,
      " i ",
      "i i",
      " i ",
      'i', "nuggetIron"),

    "transistor" ->(transistor,
      "iii",
      "grg",
      " t ",
      'i', "nuggetIron",
      'g', goldNugget,
      'r', redstoneDust,
      't', redstoneTorch),

    "chip1" ->(chip1,
      "ibi",
      "rtr",
      "ibi",
      'i', "nuggetIron",
      'b', ironBars,
      'r', redstoneDust,
      't', transistor),

    "chip2" ->(chip2,
      "glg",
      "cdc",
      "glg",
      'g', goldNugget,
      'l', lapis,
      'c', chip1,
      'd', diamond),

    "chip3" ->(chip3,
      "dmd",
      "cec",
      "dmd",
      'd', glowstoneDust,
      'm', comparator,
      'c', chip2,
      'e', emerald),

    "alu" ->(alu,
      "rtr",
      "sss",
      "idi",
      'r', repeater,
      's', transistor,
      't', redstoneTorch,
      'i', "nuggetIron",
      'd', redstoneDust),

    "cu" ->(cu,
      "gtg",
      "scs",
      "gdg",
      'g', goldNugget,
      't', redstoneTorch,
      's', transistor,
      'c', clock,
      'd', redstoneDust),

    "cpu" ->(cpu,
      "cdc",
      "bub",
      "cac",
      'c', chip1,
      'd', redstoneDust,
      'b', ironBars,
      'u', cu,
      'a', alu),

    "card" ->(card,
      "ict",
      "ibb",
      "igg",
      'i', "nuggetIron",
      'c', chip1,
      't', transistor,
      'b', pcb,
      'g', goldNugget),

    "buttonGroup" ->(Items.buttonGroup.createItemStack(),
      "bbb",
      "bbb",
      'b', new ItemStack(Block.stoneButton)),

    "arrowKeys" ->(Items.arrowKeys.createItemStack(),
      " b ",
      "bbb",
      'b', new ItemStack(Block.stoneButton)),

    "numPad" ->(Items.numPad.createItemStack(),
      "bbb",
      "bbb",
      "bbb",
      'b', new ItemStack(Block.stoneButton)),

    // ----------------------------------------------------------------------- //

    "case1" ->(Blocks.case1.createItemStack(),
      "ipi",
      "bcb",
      "imi",
      'i', ironIngot,
      'p', pcb,
      'b', ironBars,
      'c', cpu,
      'm', chip1),

    "case2" ->(Blocks.case2.createItemStack(),
      "gpg",
      "mcm",
      "gpg",
      'g', goldIngot,
      'p', pcb,
      'm', chip2,
      'c', Blocks.case1.createItemStack()),

    "case3" ->(Blocks.case3.createItemStack(),
      "mpm",
      "dcd",
      "mpm",
      'm', chip3,
      'p', pcb,
      'd', diamond,
      'c', Blocks.case2.createItemStack()),

    "screen1" ->(Blocks.screen1.createItemStack(),
      "iig",
      "rtg",
      "iig",
      'i', ironIngot,
      'g', glass,
      'r', redstoneDust,
      't', transistor),

    "screen2" ->(Blocks.screen2.createItemStack(),
      "iri",
      "cgs",
      "ibi",
      'i', goldIngot,
      'r', roseRed,
      'c', chip2,
      'g', cactusGreen,
      's', Blocks.screen1.createItemStack(),
      'b', lapis),

    "screen3" ->(Blocks.screen3.createItemStack(),
      "opc",
      "bqs",
      "opc",
      'o', obsidian,
      'p', pcb,
      'c', chip3,
      'b', blazeRod,
      'q', netherQuartz,
      's', Blocks.screen2.createItemStack()),

    "capacitor" ->(Blocks.capacitor.createItemStack(),
      "iti",
      "gpg",
      "ibi",
      'i', ironIngot,
      't', transistor,
      'g', goldNugget,
      'p', paper,
      'b', pcb),

    "powerConverter" ->(Blocks.powerConverter.createItemStack(),
      "iwi",
      "gcg",
      "ibi",
      'i', ironIngot,
      'c', chip1,
      'w', cable,
      'g', goldIngot,
      'b', pcb),

    "diskDrive" ->(Blocks.diskDrive.createItemStack(),
      "ici",
      "ps ",
      "ici",
      'i', ironIngot,
      'c', chip1,
      'p', piston,
      's', stick),

    "adapter" ->(Blocks.adapter.createItemStack(),
      "iwi",
      "wcw",
      "ibi",
      'i', ironIngot,
      'w', cable,
      'c', chip1,
      'b', pcb),

    "redstone" ->(Blocks.redstone.createItemStack(),
      "iri",
      "rcr",
      "ibi",
      'i', ironIngot,
      'r', redstoneBlock,
      'c', redstoneCard,
      'b', pcb),

    "powerDistributor" ->(Blocks.powerDistributor.createItemStack(),
      "igi",
      "wcw",
      "ibi",
      'i', ironIngot,
      'g', goldIngot,
      'w', cable,
      'c', chip1,
      'b', pcb),

    "router" ->(Blocks.router.createItemStack(),
      "iwi",
      "wnw",
      "ibi",
      'i', ironIngot,
      'w', cable,
      'n', lanCard,
      'b', pcb),

    "charger" ->(Blocks.charger.createItemStack(),
      "igi",
      "pcp",
      "ibi",
      'i', ironIngot,
      'g', goldIngot,
      'p', Blocks.capacitor.createItemStack(),
      'c', chip2,
      'b', pcb),

    "robotProxy" ->(Blocks.robotProxy.createItemStack(),
      "sgf",
      "dcr",
      "bmb",
      's', Blocks.screen1.createItemStack(),
      'g', gpu1,
      'f', Blocks.diskDrive.createItemStack(),
      'd', dispenser,
      'c', Blocks.case1.createItemStack(),
      'r', ram1,
      'b', Blocks.capacitor.createItemStack(),
      'm', minecartHopper),

    "keyboard" ->(Blocks.keyboard.createItemStack(),
      "ggg",
      "gan",
      'g', Items.buttonGroup.createItemStack(),
      'a', Items.arrowKeys.createItemStack(),
      'n', Items.numPad.createItemStack()),

    "cable" ->(Blocks.cable.createItemStack(4),
      " i ",
      "iri",
      " i ",
      'i', "nuggetIron",
      'r', redstoneDust),

    // ----------------------------------------------------------------------- //

    "cuttingWire" ->(Items.cuttingWire.createItemStack(),
      "sis",
      's', stick,
      'i', "nuggetIron"),

    "analyzer" ->(Items.analyzer.createItemStack(),
      " r ",
      "tcg",
      "tpg",
      'r', redstoneTorch,
      't', transistor,
      'c', chip1,
      'g', goldNugget,
      'p', pcb),

    "ram1" ->(ram1,
      "ccc",
      "bbb",
      'c', chip1,
      'b', pcb),

    "ram2" ->(ram2,
      "ccc",
      "rbr",
      'c', chip2,
      'r', ram1,
      'b', pcb),

    "ram3" ->(ram3,
      "ccc",
      "rbr",
      'c', chip3,
      'r', ram2,
      'b', pcb),

    "floppy" ->(floppy,
      "ili",
      "bdb",
      "ipi",
      'i', "nuggetIron",
      'l', lever,
      'b', board,
      'd', disk,
      'p', paper),

    "hdd1" ->(hdd1,
      "cdi",
      "bdp",
      "cdi",
      'c', chip1,
      'd', disk,
      'i', ironIngot,
      'b', pcb,
      'p', piston),

    "hdd2" ->(hdd2,
      "gdg",
      "cbc",
      "gdg",
      'g', goldIngot,
      'd', hdd1,
      'c', chip2,
      'b', pcb),

    "hdd3" ->(hdd3,
      "cdc",
      "rbr",
      "cdc",
      'c', chip3,
      'd', hdd2,
      'r', ram1,
      'b', pcb),

    "gpu1" ->(gpu1,
      "car",
      " b ",
      'c', chip1,
      'a', alu,
      'r', ram1,
      'b', card),

    "gpu2" ->(gpu2,
      "ccr",
      " g ",
      'c', chip2,
      'r', ram2,
      'g', gpu1),

    "gpu3" ->(gpu3,
      "ccr",
      " g ",
      'c', chip3,
      'r', ram3,
      'g', gpu2),

    "redstoneCard" ->(redstoneCard,
      "tc ",
      " b ",
      't', redstoneTorch,
      'c', chip1,
      'b', card),

    "lanCard" ->(lanCard,
      "wc ",
      " b ",
      'w', cable,
      'c', chip1,
      'b', card),

    "wlanCard" ->(wlanCard,
      "pc ",
      " b ",
      'p', enderPearl,
      'c', chip2,
      'b', lanCard),

    "generator" ->(Items.generator.createItemStack(),
      "i i",
      "cpc",
      "bib",
      'i', ironIngot,
      'c', chip1,
      'p', piston,
      'b', pcb),

    "crafting" ->(Items.crafting.createItemStack(),
      "ipi",
      "cwc",
      "ibi",
      'i', ironIngot,
      'p', piston,
      'c', chip1,
      'w', craftingTable,
      'b', pcb)


  )
  if (!Loader.isModLoaded("gregtech_addon")) {
    GameRegistry.addRecipe(new ShapelessOreRecipe(Items.ironNugget.createItemStack(9), ironIngot))
  }

  //  GameRegistry.addShapelessRecipe(rawBoard, Items.cuttingWire.createItemStack(), new ItemStack(Block.blockClay), cactusGreen)
  //  FurnaceRecipes.smelting().addSmelting(rawBoard.itemID, rawBoard.getItemDamage, board, 0)
  //  GameRegistry.addRecipe(new ShapelessOreRecipe(acid, Item.bucketWater, sugar, roseRed, slimeBall, spiderEye, boneMeal))
  //  GameRegistry.addRecipe(new ShapelessOreRecipe(pcb, acid, Item.goldNugget, board))
  //GameRegistry.addRecipe(new ShapelessOreRecipe(Items.locator.createItemStack(), new ItemStack(Item.map, 1, OreDictionary.WILDCARD_VALUE), pcb))


  private def addRecipe(output: ItemStack, args: Any*) = {
    GameRegistry.addRecipe(new ShapedOreRecipe(output, args.map(_.asInstanceOf[AnyRef]): _*))
  }

  def inputToFile(is: URL, f: java.io.File) {

    FileUtils.copyURLToFile(is, f)


  }

  def getItemFromName(name: String) = {
    Item.itemsList.find(item => item != null && (item.getUnlocalizedName == name || item.getUnlocalizedName == "item." + name)) match {
      case Some(item) => {
        item
      }
      case _ => throw new Exception("No item found for name: " + name)
    }
  }

  def getBlockFromName(name: String) = {
    Block.blocksList.find(block => block != null && (block.getUnlocalizedName == name || block.getUnlocalizedName == "tile." + name)) match {
      case Some(block) => {
        block
      }
      case _ => throw new Exception("No block found for name: " + name)
    }
  }


  def getNameOrStackFromName(name: String) = {
    if (name.isEmpty)
      null
    else if (!OreDictionary.getOres(name).isEmpty) name
    else {
      var list = Item.itemsList

      list.find(item => item != null && (item.getUnlocalizedName == name || item.getUnlocalizedName == "item." + name)) match {
        case Some(item) => {
          item
        }
        case _ => {
          Block.blocksList.find(block => block != null && (block.getUnlocalizedName == name || block.getUnlocalizedName == "tile." + name)) match {
            case Some(block) => block
            case _ => {
              throw new Exception("No item / block found for name: " + name)

            }
          }
        }
      }
    }
  }

  def cartesianProduct[T](xss: List[List[T]]): List[List[T]] = xss match {
    case Nil => List(Nil)
    case h :: t => for (xh <- h;
                        xt <- cartesianProduct(t)) yield xh :: xt
  }
}

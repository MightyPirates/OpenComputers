package li.cil.oc

import com.typesafe.config.{Config, ConfigFactory}
import cpw.mods.fml.common.Loader
import cpw.mods.fml.common.registry.GameRegistry
import java.io.File
import java.net.URL
import net.minecraft.block.Block
import net.minecraft.item.crafting.FurnaceRecipes
import net.minecraft.item.{ItemStack, Item}
import net.minecraftforge.oredict.{OreDictionary, ShapelessOreRecipe, ShapedOreRecipe}
import org.apache.commons.io.FileUtils
import scala.collection.convert.wrapAsScala._
import scala.collection.mutable.ListBuffer

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
      loadRecipe(config.getConfig("cu"), Items.cu.createItemStack())
      loadRecipe(config.getConfig("cpu"), Items.cpu.createItemStack())
      loadRecipe(config.getConfig("card"), Items.card.createItemStack())
      loadRecipe(config.getConfig("buttonGroup"), Items.buttonGroup.createItemStack())
      loadRecipe(config.getConfig("arrowKeys"), Items.arrowKeys.createItemStack())
      loadRecipe(config.getConfig("numPad"), Items.numPad.createItemStack())
      loadRecipe(config.getConfig("case1"), Blocks.case1.createItemStack())
      loadRecipe(config.getConfig("case2"), Blocks.case2.createItemStack())
      loadRecipe(config.getConfig("case3"), Blocks.case3.createItemStack())
      loadRecipe(config.getConfig("screen1"), Blocks.screen1.createItemStack())
      loadRecipe(config.getConfig("screen2"), Blocks.screen2.createItemStack())
      loadRecipe(config.getConfig("screen3"), Blocks.screen3.createItemStack())
      loadRecipe(config.getConfig("capacitor"), Blocks.capacitor.createItemStack())
      loadRecipe(config.getConfig("powerConverter"), Blocks.powerConverter.createItemStack())
      loadRecipe(config.getConfig("diskDrive"), Blocks.diskDrive.createItemStack())
      loadRecipe(config.getConfig("adapter"), Blocks.adapter.createItemStack())
      loadRecipe(config.getConfig("redstoneCrafting"), Blocks.redstone.createItemStack())
      loadRecipe(config.getConfig("powerDistributor"), Blocks.powerDistributor.createItemStack())
      loadRecipe(config.getConfig("router"), Blocks.router.createItemStack())
      loadRecipe(config.getConfig("charger"), Blocks.charger.createItemStack())
      loadRecipe(config.getConfig("robot"), Blocks.robotProxy.createItemStack())
      loadRecipe(config.getConfig("keyboard"), Blocks.keyboard.createItemStack())
      loadRecipe(config.getConfig("cable"), Blocks.cable.createItemStack())
      loadRecipe(config.getConfig("cuttingWire"), Items.cuttingWire.createItemStack())
      loadRecipe(config.getConfig("analyzer"), Items.analyzer.createItemStack())
      loadRecipe(config.getConfig("craftingRAMBasic"), Items.ram1.createItemStack())
      loadRecipe(config.getConfig("craftingRAMAdvanced"), Items.ram2.createItemStack())
      loadRecipe(config.getConfig("craftingRAMElite"), Items.ram3.createItemStack())
      loadRecipe(config.getConfig("floppy"), Items.floppyDisk.createItemStack())
      loadRecipe(config.getConfig("hdd1"), Items.hdd1.createItemStack())
      loadRecipe(config.getConfig("hdd2"), Items.hdd2.createItemStack())
      loadRecipe(config.getConfig("hdd3"), Items.hdd3.createItemStack())
      loadRecipe(config.getConfig("gpu1"), Items.gpu1.createItemStack())
      loadRecipe(config.getConfig("gpu2"), Items.gpu2.createItemStack())
      loadRecipe(config.getConfig("gpu3"), Items.gpu3.createItemStack())
      loadRecipe(config.getConfig("redstoneCard"), Items.rs.createItemStack())
      loadRecipe(config.getConfig("lanCard"), Items.lan.createItemStack())
      loadRecipe(config.getConfig("wlanCard"), Items.wlan.createItemStack())
      loadRecipe(config.getConfig("generator"), Items.generator.createItemStack())
      loadRecipe(config.getConfig("crafting"), Items.crafting.createItemStack())
      loadRecipe(config.getConfig("locator"), Items.locator.createItemStack())
      loadRecipe(config.getConfig("solarGenerator"), Items.solarGenerator.createItemStack())
      loadRecipe(config.getConfig("reader"), Items.signUpgrade.createItemStack())
      loadRecipe(config.getConfig("chip1"), Items.chip1.createItemStack())
      loadRecipe(config.getConfig("chip2"), Items.chip2.createItemStack())
      loadRecipe(config.getConfig("chip3"), Items.chip3.createItemStack())
      loadRecipe(config.getConfig("alu"), Items.alu.createItemStack())
      loadRecipe(config.getConfig("pcb"), Items.pcb.createItemStack())
      loadRecipe(config.getConfig("disk"), Items.disk.createItemStack())
      loadRecipe(config.getConfig("pcb"), Items.pcb.createItemStack())
      loadRecipe(config.getConfig("acid"), Items.acid.createItemStack())
      loadRecipe(config.getConfig("rawBoard"), Items.rawCircuitBoard.createItemStack())
      loadRecipe(config.getConfig("circuitBoard"), Items.circuitBoard.createItemStack())
      if (OreDictionary.getOres("nuggetIron").contains(Items.ironNugget.createItemStack())) {
        GameRegistry.addRecipe(new ShapelessOreRecipe(Items.ironNugget.createItemStack(9), "nuggetIron"))
      }
      GameRegistry.addRecipe(new ShapelessOreRecipe(Items.locator.createItemStack(), Items.locator.createItemStack(), new ItemStack(Item.map, 1, OreDictionary.WILDCARD_VALUE)))
    }
  }

  def loadRecipe(conf: Config, result: ItemStack) {
    conf.getString("type") match {
      case "shaped" => addShapedRecipe(result, conf)
      case "shapeless" => addShapelessRecipe(result, conf)
      case "furnace" => addSmeltingRecipe(result, conf)
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

                //again a list
                case list2: java.util.List[Object] => {
                  for (entry2 <- list2) {
                    entry2 match {
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
                case null =>
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

  private def addSmeltingRecipe(output: ItemStack, conf: Config) {
    var out = output.copy()
    try {
      out.stackSize = conf.getInt("result")
    } catch {
      case _: Throwable => // ignore
    }
    //loop through recipe and get type of entry
    for (item <- conf.getList("recipe")) {
      {
        var obj: Object = null
        var itemSubID: Int = 0
        item.unwrapped() match {

          case list: java.util.HashMap[String, Object] => {
            for (entrySet <- list.entrySet()) {
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
          case value: String => {
            obj = getNameOrStackFromName(value)
          }
          case _ => println(item.unwrapped())
        }
        obj match {
          case item: Item => {

            val newItem = new ItemStack(item, 1, itemSubID)
            FurnaceRecipes.smelting().addSmelting(newItem.itemID, newItem.getItemDamage, out, 0)
          }
          case block: Block => {
            val newItem = new ItemStack(block, 1, itemSubID)
            FurnaceRecipes.smelting().addSmelting(newItem.itemID, newItem.getItemDamage, out, 0)
          }
          case value: String => {
            for (stack <- OreDictionary.getOres(value)) {
              FurnaceRecipes.smelting().addSmelting(stack.itemID, stack.getItemDamage, out, 0)
            }
          }
          case null =>
        }
      }
    }
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

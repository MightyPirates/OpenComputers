package li.cil.oc.common

import java.io
import java.util.Random

import cpw.mods.fml.common.eventhandler.SubscribeEvent
import li.cil.oc.common.recipe.Recipes
import li.cil.oc.util.Color
import li.cil.oc.{OpenComputers, Settings, api}
import net.minecraft.inventory.IInventory
import net.minecraft.item.ItemStack
import net.minecraft.nbt.NBTTagCompound
import net.minecraft.util.WeightedRandomChestContent
import net.minecraftforge.common.{ChestGenHooks, DimensionManager}
import net.minecraftforge.event.world.WorldEvent

import scala.collection.convert.WrapAsScala._
import scala.collection.mutable

object Loot extends WeightedRandomChestContent(api.Items.get("openOS").createItemStack(1), 1, 1, Settings.get.lootProbability) {
  val containers = Array(
    ChestGenHooks.DUNGEON_CHEST,
    ChestGenHooks.PYRAMID_DESERT_CHEST,
    ChestGenHooks.PYRAMID_JUNGLE_CHEST,
    ChestGenHooks.STRONGHOLD_LIBRARY)

  val builtInDisks = mutable.Map.empty[String, (ItemStack, Int)]

  val worldDisks = mutable.Map.empty[String, (ItemStack, Int)]

  val disks = mutable.ArrayBuffer.empty[ItemStack]

  def init() {
    for (container <- containers) {
      ChestGenHooks.addItem(container, Loot)
    }

    val list = new java.util.Properties()
    val listStream = getClass.getResourceAsStream("/assets/" + Settings.resourceDomain + "/loot/loot.properties")
    list.load(listStream)
    listStream.close()
    parseLootDisks(list, builtInDisks)

    for ((name, (stack, _)) <- builtInDisks if name == "OpenOS") {
      Recipes.list += stack -> "openOS"
    }
  }

  @SubscribeEvent
  def initForWorld(e: WorldEvent.Load) {
    worldDisks.clear()
    disks.clear()
    val path = new io.File(DimensionManager.getCurrentSaveRootDirectory, Settings.savePath + "loot/")
    if (path.exists && path.isDirectory) {
      val listFile = new io.File(path, "loot.properties")
      if (listFile.exists && listFile.isFile) {
        try {
          val listStream = new io.FileInputStream(listFile)
          val list = new java.util.Properties()
          list.load(listStream)
          listStream.close()
          parseLootDisks(list, worldDisks)
        }
        catch {
          case t: Throwable => OpenComputers.log.warn("Failed opening loot descriptor file in saves folder.")
        }
      }
    }
    for ((name, entry) <- builtInDisks if !worldDisks.contains(name)) {
      worldDisks += name -> entry
    }
    for ((_, (stack, count)) <- worldDisks) {
      for (i <- 0 until count) {
        disks += stack
      }
    }
  }

  private def parseLootDisks(list: java.util.Properties, acc: mutable.Map[String, (ItemStack, Int)]) {
    for (key <- list.stringPropertyNames if key != "OpenOS") {
      val value = list.getProperty(key)
      try value.split(":") match {
        case Array(name, count, color) =>
          acc += key -> ((createLootDisk(name, key, Some(color)), count.toInt))
        case Array(name, count) =>
          acc += key -> ((createLootDisk(name, key), count.toInt))
        case _ =>
          acc += key -> ((createLootDisk(value, key), 1))
      }
      catch {
        case t: Throwable => OpenComputers.log.warn("Bad loot descriptor: " + value, t)
      }
    }
  }

  private def createLootDisk(name: String, path: String, color: Option[String] = None) = {
    val data = new NBTTagCompound()
    data.setString(Settings.namespace + "fs.label", name)

    val tag = new NBTTagCompound()
    tag.setTag(Settings.namespace + "data", data)
    // Store this top level, so it won't get wiped on save.
    tag.setString(Settings.namespace + "lootPath", path)
    color match {
      case Some(oreDictName) =>
        tag.setInteger(Settings.namespace + "color", Color.dyes.indexOf(oreDictName))
      case _ =>
    }

    val disk = api.Items.get("lootDisk").createItemStack(1)
    disk.setTagCompound(tag)

    disk
  }

  override def generateChestContent(random: Random, newInventory: IInventory) =
    if (disks.length > 0)
      ChestGenHooks.generateStacks(random, disks(random.nextInt(disks.length)),
        theMinimumChanceToGenerateItem, theMaximumChanceToGenerateItem)
    else Array.empty
}

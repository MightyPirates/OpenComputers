package li.cil.oc.common

import java.io
import java.util.Random
import java.util.concurrent.Callable

import cpw.mods.fml.common.Loader
import cpw.mods.fml.common.eventhandler.SubscribeEvent
import li.cil.oc.Constants
import li.cil.oc.OpenComputers
import li.cil.oc.Settings
import li.cil.oc.api
import li.cil.oc.api.fs.FileSystem
import li.cil.oc.common.init.Items
import li.cil.oc.util.Color
import net.minecraft.inventory.IInventory
import net.minecraft.item.Item
import net.minecraft.item.ItemStack
import net.minecraft.nbt.NBTTagCompound
import net.minecraft.util.WeightedRandomChestContent
import net.minecraftforge.common.ChestGenHooks
import net.minecraftforge.common.DimensionManager
import net.minecraftforge.event.world.WorldEvent

import scala.collection.convert.WrapAsScala._
import scala.collection.mutable

object Loot extends WeightedRandomChestContent(new ItemStack(null: Item), 1, 1, Settings.get.lootProbability) {
  val containers = Array(
    ChestGenHooks.DUNGEON_CHEST,
    ChestGenHooks.PYRAMID_DESERT_CHEST,
    ChestGenHooks.PYRAMID_JUNGLE_CHEST,
    ChestGenHooks.STRONGHOLD_LIBRARY)

  val factories = mutable.Map.empty[String, Callable[FileSystem]]

  val globalDisks = mutable.Map.empty[String, (ItemStack, Int)]

  val worldDisks = mutable.Map.empty[String, (ItemStack, Int)]

  val disksForSampling = mutable.ArrayBuffer.empty[ItemStack]

  val disksForClient = mutable.ArrayBuffer.empty[ItemStack]

  def registerLootDisk(name: String, color: Int, factory: Callable[FileSystem]): ItemStack = {
    val mod = Loader.instance.activeModContainer.getModId

    OpenComputers.log.info(s"Registering loot disk '$name' from mod $mod.")

    val modSpecificName = mod + ":" + name

    val data = new NBTTagCompound()
    data.setString(Settings.namespace + "fs.label", name)

    val nbt = new NBTTagCompound()
    nbt.setTag(Settings.namespace + "data", data)

    // Store this top level, so it won't get wiped on save.
    nbt.setString(Settings.namespace + "lootFactory", modSpecificName)
    nbt.setInteger(Settings.namespace + "color", color max 0 min 15)

    val stack = Items.get(Constants.ItemName.Floppy).createItemStack(1)
    stack.setTagCompound(nbt)

    Loot.factories += modSpecificName -> factory

    stack.copy()
  }

  def init() {
    for (container <- containers) {
      ChestGenHooks.addItem(container, Loot)
    }

    val list = new java.util.Properties()
    val listStream = getClass.getResourceAsStream("/assets/" + Settings.resourceDomain + "/loot/loot.properties")
    list.load(listStream)
    listStream.close()
    parseLootDisks(list, globalDisks, external = false)
  }

  @SubscribeEvent
  def initForWorld(e: WorldEvent.Load): Unit = if (!e.world.isRemote && e.world.provider.dimensionId == 0) {
    worldDisks.clear()
    disksForSampling.clear()
    val path = new io.File(DimensionManager.getCurrentSaveRootDirectory, Settings.savePath + "loot/")
    if (path.exists && path.isDirectory) {
      val listFile = new io.File(path, "loot.properties")
      if (listFile.exists && listFile.isFile) {
        try {
          val listStream = new io.FileInputStream(listFile)
          val list = new java.util.Properties()
          list.load(listStream)
          listStream.close()
          parseLootDisks(list, worldDisks, external = true)
        }
        catch {
          case t: Throwable => OpenComputers.log.warn("Failed opening loot descriptor file in saves folder.")
        }
      }
    }
    for ((name, entry) <- globalDisks if !worldDisks.contains(name)) {
      worldDisks += name -> entry
    }
    for ((_, (stack, count)) <- worldDisks) {
      for (i <- 0 until count) {
        disksForSampling += stack
      }
    }
  }

  private def parseLootDisks(list: java.util.Properties, acc: mutable.Map[String, (ItemStack, Int)], external: Boolean) {
    for (key <- list.stringPropertyNames) {
      val value = list.getProperty(key)
      try value.split(":") match {
        case Array(name, count, color) =>
          acc += key -> ((createLootDisk(name, key, external, Some(Color.dyes.indexOf(color))), count.toInt))
        case Array(name, count) =>
          acc += key -> ((createLootDisk(name, key, external), count.toInt))
        case _ =>
          acc += key -> ((createLootDisk(value, key, external), 1))
      }
      catch {
        case t: Throwable => OpenComputers.log.warn("Bad loot descriptor: " + value, t)
      }
    }
  }

  def createLootDisk(name: String, path: String, external: Boolean, color: Option[Int] = None) = {
    val callable = if (external) new Callable[FileSystem] {
      override def call(): FileSystem = api.FileSystem.fromSaveDirectory("loot/" + path, 0, false)
    } else new Callable[FileSystem] {
      override def call(): FileSystem = api.FileSystem.fromClass(OpenComputers.getClass, Settings.resourceDomain, "loot/" + path)
    }
    val stack = registerLootDisk(name, color.getOrElse(8), callable)
    if (!external) {
      Items.registerStack(stack, name)
    }
    stack
  }

  override def generateChestContent(random: Random, newInventory: IInventory) =
    if (disksForSampling.length > 0)
      ChestGenHooks.generateStacks(random, disksForSampling(random.nextInt(disksForSampling.length)),
        theMinimumChanceToGenerateItem, theMaximumChanceToGenerateItem)
    else Array.empty[ItemStack]
}

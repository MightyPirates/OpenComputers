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
import net.minecraft.item.ItemStack
import net.minecraft.nbt.NBTTagCompound
import net.minecraft.util.WeightedRandomChestContent
import net.minecraftforge.common.ChestGenHooks
import net.minecraftforge.common.DimensionManager
import net.minecraftforge.common.util.Constants.NBT
import net.minecraftforge.event.world.WorldEvent

import scala.collection.convert.WrapAsScala._
import scala.collection.mutable

class Loot extends WeightedRandomChestContent(api.Items.get(Constants.ItemName.Floppy).createItemStack(1), 1, 1, Settings.get.lootProbability) {
  override def generateChestContent(random: Random, newInventory: IInventory) =
    if (Loot.disksForSampling.nonEmpty)
      ChestGenHooks.generateStacks(random, Loot.disksForSampling(random.nextInt(Loot.disksForSampling.length)),
        theMinimumChanceToGenerateItem, theMaximumChanceToGenerateItem)
    else Array.empty[ItemStack]
}

object Loot {
  val containers = Array(
    ChestGenHooks.DUNGEON_CHEST,
    ChestGenHooks.PYRAMID_DESERT_CHEST,
    ChestGenHooks.PYRAMID_JUNGLE_CHEST,
    ChestGenHooks.STRONGHOLD_LIBRARY)

  val factories = mutable.Map.empty[String, Callable[FileSystem]]

  val globalDisks = mutable.ArrayBuffer.empty[(ItemStack, Int)]

  val worldDisks = mutable.ArrayBuffer.empty[(ItemStack, Int)]

  val disksForSampling = mutable.ArrayBuffer.empty[ItemStack]

  val disksForClient = mutable.ArrayBuffer.empty[ItemStack]

  def isLootDisk(stack: ItemStack): Boolean = api.Items.get(stack) == api.Items.get(Constants.ItemName.Floppy) && stack.hasTagCompound && stack.getTagCompound.hasKey(Settings.namespace + "lootFactory", NBT.TAG_STRING)

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
      ChestGenHooks.addItem(container, new Loot())
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
    for (entry <- globalDisks if !worldDisks.contains(entry)) {
      worldDisks += entry
    }
    for ((stack, count) <- worldDisks) {
      for (i <- 0 until count) {
        disksForSampling += stack
      }
    }
  }

  private def parseLootDisks(list: java.util.Properties, acc: mutable.ArrayBuffer[(ItemStack, Int)], external: Boolean) {
    for (key <- list.stringPropertyNames) {
      val value = list.getProperty(key)
      try value.split(":") match {
        case Array(name, count, color) =>
          acc += ((createLootDisk(name, key, external, Some(Color.dyes.indexOf(color))), count.toInt))
        case Array(name, count) =>
          acc += ((createLootDisk(name, key, external), count.toInt))
        case _ =>
          acc += ((createLootDisk(value, key, external), 1))
      }
      catch {
        case t: Throwable => OpenComputers.log.warn("Bad loot descriptor: " + value, t)
      }
    }
  }

  def createLootDisk(name: String, path: String, external: Boolean, color: Option[Int] = None) = {
    val callable = if (external) new Callable[FileSystem] {
      override def call(): FileSystem = api.FileSystem.asReadOnly(api.FileSystem.fromSaveDirectory("loot/" + path, 0, false))
    } else new Callable[FileSystem] {
      override def call(): FileSystem = api.FileSystem.fromClass(OpenComputers.getClass, Settings.resourceDomain, "loot/" + path)
    }
    val stack = registerLootDisk(path, color.getOrElse(8), callable)
    stack.setStackDisplayName(name)
    if (!external) {
      Items.registerStack(stack, path)
    }
    stack
  }
}

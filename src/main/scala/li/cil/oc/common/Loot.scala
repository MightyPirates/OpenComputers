package li.cil.oc.common

import java.io
import java.util.Random
import java.util.concurrent.Callable

import li.cil.oc.Constants
import li.cil.oc.OpenComputers
import li.cil.oc.Settings
import li.cil.oc.api
import li.cil.oc.api.fs.FileSystem
import li.cil.oc.common.init.Items
import li.cil.oc.util.Color
import net.minecraft.item.EnumDyeColor
import net.minecraft.item.ItemStack
import net.minecraft.nbt.NBTTagCompound
import net.minecraftforge.common.DimensionManager
import net.minecraftforge.common.util.Constants.NBT
import net.minecraftforge.event.world.WorldEvent
import net.minecraftforge.fml.common.Loader
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent

import scala.collection.convert.WrapAsScala._
import scala.collection.mutable

//class Loot extends WeightedRandomChestContent(api.Items.get(Constants.ItemName.Floppy).item(), api.Items.get(Constants.ItemName.Floppy).createItemStack(1).getItemDamage, 1, 1, Settings.get.lootProbability) {
//  override def generateChestContent(random: Random, newInventory: IInventory) =
//    Loot.randomDisk(random) match {
//      case Some(disk) =>
//        ChestGenHooks.generateStacks(random, disk, minStackSize, maxStackSize)
//      case _ => Array.empty[ItemStack]
//    }
//}

object Loot {
//  val containers = Array(
//    ChestGenHooks.DUNGEON_CHEST,
//    ChestGenHooks.PYRAMID_DESERT_CHEST,
//    ChestGenHooks.PYRAMID_JUNGLE_CHEST,
//    ChestGenHooks.STRONGHOLD_LIBRARY)

  val factories = mutable.Map.empty[String, Callable[FileSystem]]

  val globalDisks = mutable.ArrayBuffer.empty[(ItemStack, Int)]

  val worldDisks = mutable.ArrayBuffer.empty[(ItemStack, Int)]

  def disksForCycling = if(disksForCyclingClient.nonEmpty) disksForCyclingClient else disksForCyclingServer

  val disksForCyclingServer = mutable.ArrayBuffer.empty[ItemStack]

  val disksForCyclingClient = mutable.ArrayBuffer.empty[ItemStack]

  val disksForSampling = mutable.ArrayBuffer.empty[ItemStack]

  val disksForClient = mutable.ArrayBuffer.empty[ItemStack]

  def isLootDisk(stack: ItemStack): Boolean = api.Items.get(stack) == api.Items.get(Constants.ItemName.Floppy) && stack.hasTagCompound && stack.getTagCompound.hasKey(Settings.namespace + "lootFactory", NBT.TAG_STRING)

  def randomDisk(rng: Random) =
    if (disksForSampling.nonEmpty) Some(disksForSampling(rng.nextInt(disksForSampling.length)))
    else None

  def registerLootDisk(name: String, color: EnumDyeColor, factory: Callable[FileSystem], doRecipeCycling: Boolean): ItemStack = {
    val mod = Loader.instance.activeModContainer.getModId

    OpenComputers.log.debug(s"Registering loot disk '$name' from mod $mod.")

    val modSpecificName = mod + ":" + name

    val data = new NBTTagCompound()
    data.setString(Settings.namespace + "fs.label", name)

    val nbt = new NBTTagCompound()
    nbt.setTag(Settings.namespace + "data", data)

    // Store this top level, so it won't get wiped on save.
    nbt.setString(Settings.namespace + "lootFactory", modSpecificName)
    nbt.setInteger(Settings.namespace + "color", color.getDyeDamage)

    val stack = Items.get(Constants.ItemName.Floppy).createItemStack(1)
    stack.setTagCompound(nbt)

    Loot.factories += modSpecificName -> factory

    if(doRecipeCycling) {
      Loot.disksForCyclingServer += stack
    }

    stack.copy()
  }

  def init() {
//    for (container <- containers) {
//      ChestGenHooks.addItem(container, new Loot())
//    }

    val list = new java.util.Properties()
    val listStream = getClass.getResourceAsStream("/assets/" + Settings.resourceDomain + "/loot/loot.properties")
    list.load(listStream)
    listStream.close()
    parseLootDisks(list, globalDisks, external = false)
  }

  @SubscribeEvent
  def initForWorld(e: WorldEvent.Load): Unit = if (!e.getWorld.isRemote && e.getWorld.provider.getDimension == 0) {
    worldDisks.clear()
    disksForSampling.clear()
    if (!e.getWorld.isRemote) {
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
          acc += ((createLootDisk(name, key, external, Color.byOreName.get(color)), count.toInt))
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

  def createLootDisk(name: String, path: String, external: Boolean, color: Option[EnumDyeColor] = None) = {
    val callable = if (external) new Callable[FileSystem] {
      override def call(): FileSystem = api.FileSystem.asReadOnly(api.FileSystem.fromSaveDirectory("loot/" + path, 0, false))
    } else new Callable[FileSystem] {
      override def call(): FileSystem = api.FileSystem.fromClass(OpenComputers.getClass, Settings.resourceDomain, "loot/" + path)
    }
    val stack = registerLootDisk(path, color.getOrElse(EnumDyeColor.SILVER), callable, doRecipeCycling = true)
    stack.setStackDisplayName(name)
    if (!external) {
      Items.registerStack(stack, path)
    }
    stack
  }
}

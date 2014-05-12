package li.cil.oc.common

import net.minecraftforge.common.ChestGenHooks
import net.minecraft.util.WeightedRandomChestContent
import net.minecraft.nbt.NBTTagCompound
import scala.collection.convert.WrapAsScala._
import java.util.Random
import net.minecraft.inventory.IInventory
import net.minecraft.item.ItemStack
import scala.collection.mutable
import li.cil.oc.{Settings, api}
import li.cil.oc.common.recipe.Recipes

object Loot extends WeightedRandomChestContent(api.Items.get("lootDisk").createItemStack(1), 1, 1, Settings.get.lootProbability) {
  val containers = Array(
    ChestGenHooks.DUNGEON_CHEST,
    ChestGenHooks.PYRAMID_DESERT_CHEST,
    ChestGenHooks.PYRAMID_JUNGLE_CHEST,
    ChestGenHooks.STRONGHOLD_LIBRARY)

  val disks = mutable.ArrayBuffer.empty[ItemStack]

  def init() {
    for (container <- containers) {
      ChestGenHooks.addItem(container, Loot)
    }

    val list = new java.util.Properties()
    val listFile = getClass.getResourceAsStream("/assets/" + Settings.resourceDomain + "/loot/loot.properties")
    list.load(listFile)
    listFile.close()

    for (key <- list.stringPropertyNames) {
      disks += createLootDisk(key, list.getProperty(key))
    }
  }

  def createLootDisk(name: String, path: String) = {
    val data = new NBTTagCompound()
    data.setString(Settings.namespace + "fs.label", name)

    val tag = new NBTTagCompound("tag")
    tag.setTag(Settings.namespace + "data", data)
    // Store this top level, so it won't get wiped on save.
    tag.setString(Settings.namespace + "lootPath", path)

    val disk = api.Items.get("lootDisk").createItemStack(1)
    disk.setTagCompound(tag)

    if (name == "OpenOS") {
      Recipes.list += disk -> "openOS"
    }

    disk
  }

  override def generateChestContent(random: Random, newInventory: IInventory) =
    if (disks.length > 0)
      ChestGenHooks.generateStacks(random, disks(random.nextInt(disks.length)),
        theMinimumChanceToGenerateItem, theMaximumChanceToGenerateItem)
    else Array.empty
}

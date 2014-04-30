package li.cil.oc

import net.minecraftforge.common.ChestGenHooks
import net.minecraft.util.WeightedRandomChestContent
import net.minecraft.nbt.NBTTagCompound
import scala.collection.convert.WrapAsScala._
import java.util.Random
import net.minecraft.inventory.IInventory
import net.minecraft.item.ItemStack
import scala.collection.mutable

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
    val listFile = getClass.getResourceAsStream("/assets/" + Settings.resourceDomain + "/" + "loot/loot.properties")
    list.load(listFile)
    listFile.close()

    for (key <- list.stringPropertyNames) {
      val value = list.getProperty(key)

      val data = new NBTTagCompound()
      data.setString(Settings.namespace + "fs.label", value)

      val tag = new NBTTagCompound("tag")
      tag.setTag(Settings.namespace + "data", data)
      // Store this top level, so it won't get wiped on save.
      tag.setString(Settings.namespace + "lootPath", key)


      val disk = api.Items.get("lootDisk").createItemStack(1)
      disk.setTagCompound(tag)

      disks += disk
    }
  }

  override def generateChestContent(random: Random, newInventory: IInventory) =
    if (disks.length > 0)
      ChestGenHooks.generateStacks(random, disks(random.nextInt(disks.length)),
        theMinimumChanceToGenerateItem, theMaximumChanceToGenerateItem)
    else Array.empty
}

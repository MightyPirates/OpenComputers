package li.cil.oc

import net.minecraftforge.common.ChestGenHooks
import net.minecraft.util.WeightedRandomChestContent
import net.minecraft.nbt.NBTTagCompound
import scala.collection.convert.WrapAsScala._

object Loot {

  def init() {
    val prop = new java.util.Properties()
    prop.load(getClass.getResourceAsStream("/assets/" + Settings.resourceDomain + "/" + "loot/loot.properties"))

    for (key <- prop.stringPropertyNames()) {

      val value = prop.getProperty(key)
      val loot = Items.lootDisk.createItemStack()
      var tag = loot.getTagCompound

      if (tag == null) {
        tag = new NBTTagCompound()
        loot.setTagCompound(tag)
      }
      val data = new NBTTagCompound()
      tag.setTag(Settings.namespace + "data", data)
      data.setString(Settings.namespace + "lootpath", key)
      data.setString(Settings.namespace + "fs.label", value)

      addToChest(new WeightedRandomChestContent(loot, 1, 1, 10 / prop.propertyNames().length))
    }

  }

  def addToChest(item: WeightedRandomChestContent) {
    ChestGenHooks.addItem(ChestGenHooks.DUNGEON_CHEST, item)
    ChestGenHooks.addItem(ChestGenHooks.PYRAMID_DESERT_CHEST, item)
    ChestGenHooks.addItem(ChestGenHooks.PYRAMID_JUNGLE_CHEST, item)
    ChestGenHooks.addItem(ChestGenHooks.STRONGHOLD_LIBRARY, item)
  }
}

package li.cil.oc

import java.io.File

object Config {
  var blockId = 3650

  var itemHDDId = 4600
  var itemGPUId = 4601

  var threads = 4

  def load(file: File) = {
    val config = new net.minecraftforge.common.Configuration(file)

    Config.blockId = config.getBlock("block", Config.blockId,
      "The block ID used for any blocks.").getInt(Config.blockId)

    Config.itemGPUId = config.getItem("gpu", Config.itemGPUId,
      "The item ID used for graphics cards.").getInt(Config.itemGPUId)
    Config.itemHDDId = config.getItem("hdd", Config.itemHDDId,
      "The item ID used for hard disk drives.").getInt(Config.itemHDDId)

    if (config.hasChanged)
      config.save()
  }
}
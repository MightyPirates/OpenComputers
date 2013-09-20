package li.cil.oc

import java.io.File

object Config {
  var blockId = 3650
  var blockSpecialId = 3651

  var itemHDDId = 4600
  var itemGPUId = 4601

  var threads = 4

  var blockRenderId = 0

  def load(file: File) = {
    val config = new net.minecraftforge.common.Configuration(file)

    Config.blockId = config.getBlock("block", Config.blockId,
      "The block ID used for simple blocks.").getInt(Config.blockId)
    Config.blockSpecialId = config.getBlock("blockSpecial", Config.blockSpecialId,
      "The block ID used for special blocks.").getInt(Config.blockSpecialId)

    Config.itemGPUId = config.getItem("gpu", Config.itemGPUId,
      "The item ID used for graphics cards.").getInt(Config.itemGPUId)
    Config.itemHDDId = config.getItem("hdd", Config.itemHDDId,
      "The item ID used for hard disk drives.").getInt(Config.itemHDDId)

    Config.threads = config.get("config", "threads", Config.threads,
      "The overall number of threads to use to driver computers.").getInt(Config.threads)

    if (config.hasChanged)
      config.save()
  }
}
package li.cil.oc

import java.io.File

object Config {
  val scriptPath = "/assets/opencomputers/lua/"
  val driverPath = "/assets/opencomputers/lua/drivers/"

  var blockId = 3650
  var blockSpecialId = 3651

  var itemId = 4600

  var threads = 4

  var blockRenderId = 0

  def load(file: File) = {
    val config = new net.minecraftforge.common.Configuration(file)

    Config.blockId = config.getBlock("block", Config.blockId,
      "The block ID used for simple blocks.").
      getInt(Config.blockId)
    Config.blockSpecialId = config.getBlock("blockSpecial", Config.blockSpecialId,
      "The block ID used for special blocks.").
      getInt(Config.blockSpecialId)

    Config.itemId = config.getItem("item", Config.itemId,
      "The item ID used for all non-stackable items.").
      getInt(Config.itemId)

    Config.threads = config.get("config", "threads", Config.threads,
      "The overall number of threads to use to driver computers.").
      getInt(Config.threads)

    if (config.hasChanged)
      config.save()
  }
}
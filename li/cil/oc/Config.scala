package li.cil.oc

import java.io.File

object Config {
  val resourceDomain = "opencomputers"
  val scriptPath = "/assets/" + resourceDomain + "/lua/"
  val driverPath = "/assets/" + resourceDomain + "/lua/drivers/"

  var blockId = 3650
  var blockSpecialId = 3651

  var itemId = 4600

  var threads = 4
  var timeout = 3.0
  var baseMemory = 0

  var blockRenderId = 0

  def load(file: File) = {
    val config = new net.minecraftforge.common.Configuration(file)

    blockId = config.getBlock("block", blockId,
      "The block ID used for simple blocks.").
      getInt(blockId)
    blockSpecialId = config.getBlock("blockSpecial", blockSpecialId,
      "The block ID used for special blocks.").
      getInt(blockSpecialId)

    itemId = config.getItem("item", itemId,
      "The item ID used for all non-stackable items.").
      getInt(itemId)

    threads = config.get("config", "threads", threads,
      "The overall number of threads to use to driver computers.").
      getInt(threads)
    timeout = config.get("config", "timeout", timeout,
      "The time in seconds a program may run without yielding before it is forcibly aborted.").
      getDouble(timeout)
    baseMemory = config.get("config", "baseMemory", baseMemory,
      "The base amount of memory made available in computers even if they have no RAM installed.").
      getInt(baseMemory)

    if (config.hasChanged)
      config.save()
  }
}
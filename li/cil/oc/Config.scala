package li.cil.oc

import java.io.File

object Config {
  val resourceDomain = "opencomputers"
  val savePath = "opencomputers/"
  val scriptPath = "/assets/" + resourceDomain + "/lua/"
  val driverPath = "/assets/" + resourceDomain + "/lua/drivers/"

  // ----------------------------------------------------------------------- //

  var blockRenderId = 0

  // ----------------------------------------------------------------------- //

  var blockId = 3650
  var blockSpecialId = 3651

  // ----------------------------------------------------------------------- //

  var itemId = 4600

  // ----------------------------------------------------------------------- //

  var baseMemory = 0
  var fileCost = 512
  var filesBuffered = true
  var threads = 4
  var timeout = 3.0

  var maxScreenTextRenderDistance = 6
  var screenTextFadeStartDistance = 2
  var maxScreenWidth = 8
  var maxScreenHeight = 6

  // ----------------------------------------------------------------------- //

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

    baseMemory = config.get("config", "baseMemory", baseMemory, "" +
      "The base amount of memory made available in computers even if they have no\n" +
      "RAM installed. Use this if you feel you can't get enough RAM using the\n" +
      "given means, that being RAM components. Just keep in mind that this is\n" +
      "global and applies to all computers!").
      getInt(baseMemory)

    fileCost = config.get("config", "fileCost", fileCost, "" +
      "The base 'cost' of a single file or directory on a limited file system,\n" +
      "such as hard drivers. When computing the used space we add this cost to\n" +
      "the real size of each file (and folders, which are zero sized otherwise).\n" +
      "This is to ensure that users cannot spam the file system with an infinite\n" +
      "number of files and/or folders. Note that the size returned via fs.size\n" +
      "will always be the real file size, however.").
      getInt(fileCost)

    filesBuffered = config.get("config", "filesBuffered", filesBuffered, "" +
      "Whether persistent file systems such as disk drivers should be 'buffered',\n" +
      "and only written to disk when the world is saved. This applies to all hard\n" +
      "drives. The advantage of having this enabled is that data will never go\n" +
      "'out of sync' with the computer's state if the game crashes. The price is\n" +
      "slightly higher memory consumption, since all loaded files have to be kept\n" +
      "in memory (loaded as in when the hard drive is in a computer).").
      getBoolean(filesBuffered)

    threads = config.get("config", "threads", threads, "" +
      "The overall number of threads to use to drive computers. Whenever a\n" +
      "computer should run, for example because a signal should be processed or\n" +
      "some sleep timer expired it is queued for execution by a worker thread.\n" +
      "The higher the number of worker threads, the less likely it will be that\n" +
      "computers block each other from running (leading to 'lag'), but the higher\n" +
      "the system load may become.").
      getInt(threads)

    timeout = config.get("config", "timeout", timeout, "" +
      "The time in seconds a program may run without yielding before it is\n" +
      "forcibly aborted. This is used to avoid stupidly written or malicious\n" +
      "programs blocking other computers by locking down the executor threads.").
      getDouble(timeout)

    if (config.hasChanged)
      config.save()
  }
}
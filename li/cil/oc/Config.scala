package li.cil.oc

import java.io.File
import li.cil.oc.util.PackedColor

object Config {
  val resourceDomain = "opencomputers"
  val namespace = "oc:"
  val savePath = "opencomputers/"
  val scriptPath = "/assets/" + resourceDomain + "/lua/"

  // ----------------------------------------------------------------------- //

  val screenResolutionsByTier = Array((50, 16), (80, 25), (160, 50))
  val screenDepthsByTier = Array(PackedColor.Depth.OneBit, PackedColor.Depth.FourBit, PackedColor.Depth.EightBit)

  // ----------------------------------------------------------------------- //

  var ignorePower = false

  var bufferConverter = 100.0
  var bufferCapacitor = 50.0
  var bufferPowerSupply = 50.0

  var computerCost = 1.0
  var gpuFillCost = 1.0 / 100
  var gpuClearCost = 1.0 / 400
  var gpuCopyCost = 1.0 / 200
  var gpuSetCost = 1.0 / 80
  var hddReadCost = 1.0 / 1600.0
  var hddWriteCost = 1.0 / 800.0
  var powerSupplyCost = -1.25
  var screenCost = 0.2
  var wirelessCostPerRange = 1.0 / 20.0

  // ----------------------------------------------------------------------- //

  var blockRenderId = 0

  // ----------------------------------------------------------------------- //

  var blockId = 3650
  var blockSpecialId = 3651
  var itemId = 4600

  // ----------------------------------------------------------------------- //

  var maxScreenTextRenderDistance = 10.0
  var screenTextFadeStartDistance = 8.0
  var textLinearFiltering = false

  // ----------------------------------------------------------------------- //

  var baseMemory = 0
  var canComputersBeOwned = true
  var maxUsernameLength = 32
  var maxUsers = 16
  var startupDelay = 0.5
  var threads = 4
  var timeout = 3.0

  var fileCost = 512
  var filesBuffered = true
  var maxHandles = 16
  var maxReadBuffer = 8 * 1024

  var commandUser = "OpenComputers"
  var maxScreenHeight = 6
  var maxScreenWidth = 8
  var maxWirelessRange = 400.0

  // ----------------------------------------------------------------------- //

  def load(file: File) = {
    val config = new net.minecraftforge.common.Configuration(file)

    // --------------------------------------------------------------------- //

    blockId = config.getBlock("block", blockId,
      "The block ID used for simple blocks.").
      getInt(blockId)

    blockSpecialId = config.getBlock("blockSpecial", blockSpecialId,
      "The block ID used for special blocks.").
      getInt(blockSpecialId)

    itemId = config.getItem("item", itemId,
      "The item ID used for all non-stackable items.").
      getInt(itemId)

    // --------------------------------------------------------------------- //

    config.getCategory("client").
      setComment("Client side settings, presentation and performance related stuff.")

    maxScreenTextRenderDistance = config.get("client", "maxScreenTextRenderDistance", maxScreenTextRenderDistance, "" +
      "The maximum distance at which to render text on screens. Rendering text\n" +
      "can be pretty expensive, so if you have a lot of screens you'll want\n" +
      "to avoid huge numbers here. Note that this setting is client-sided, and\n" +
      "only has an impact on render performance on clients.").
      getDouble(maxScreenTextRenderDistance)

    screenTextFadeStartDistance = config.get("client", "screenTextFadeStartDistance", screenTextFadeStartDistance, "" +
      "The distance at which to start fading out the text on screens. This is\n" +
      "purely cosmetic, to avoid text disappearing instantly when moving too far\n" +
      "away from a screen. This should have no measurable impact on performance.\n" +
      "Note that this needs OpenGL 1.4 to work, otherwise text will always just\n" +
      "instantly disappear when moving away from the screen displaying it.").
      getDouble(screenTextFadeStartDistance)

    textLinearFiltering = config.get("client", "textLinearFiltering", textLinearFiltering, "" +
      "Whether to apply linear filtering for text displayed on screens when the\n" +
      "screen has to be scaled down - i.e. the text is rendered at a resolution\n" +
      "lower than their native one, e.g. when the GUI scale is less than one or\n" +
      "when looking at a far away screen. This leads to smoother text for scaled\n" +
      "down text but results in characters not perfectly connecting anymore (for\n" +
      "example for box drawing characters. Look it up on Wikipedia.)").
      getBoolean(textLinearFiltering)

    // --------------------------------------------------------------------- //

    config.getCategory("power").
      setComment("Power settings, buffer sizes and power consumption.")

    ignorePower = config.get("power", "ignorePower", ignorePower, "" +
      "Whether to ignore any power requirements. Whenever something requires\n" +
      "power to function, it will try to get the amount of energy it needs from\n" +
      "the buffer of its connector node, and in case it fails it won't perform\n" +
      "the action / trigger a shutdown / whatever. Setting this to `true` will\n" +
      "simply make the check 'is there enough energy' succeed unconditionally.\n" +
      "Note that buffers are still filled and emptied following the usual rules,\n" +
      "there just is no failure case anymore.").
      getBoolean(ignorePower)

    // --------------------------------------------------------------------- //

    bufferCapacitor = config.get("power.buffer", "bufferCapacitor", bufferCapacitor, "" +
      "The amount of energy a capacitor can store.").
      getDouble(bufferCapacitor) max 0

    bufferConverter = config.get("power.buffer", "bufferConverter", bufferConverter, "" +
      "The amount of energy a power converter can store.").
      getDouble(bufferConverter) max 0

    bufferPowerSupply = config.get("power.buffer", "bufferPowerSupply", bufferPowerSupply, "" +
      "The amount of energy a power supply can store.").
      getDouble(bufferPowerSupply) max 0

    // --------------------------------------------------------------------- //

    computerCost = config.get("power.cost", "computerCost", computerCost, "" +
      "The amount of energy a computer consumes per tick when running.").
      getDouble(computerCost) max 0

    gpuFillCost = config.get("power.cost", "gpuFillCost", gpuFillCost, "" +
      "Energy it takes to change a single 'pixel' via the fill command. This\n" +
      "means the total cost of the fill command will be its area times this.").
      getDouble(gpuFillCost) max 0

    gpuClearCost = config.get("power.cost", "gpuClearCost", gpuClearCost, "" +
      "Energy it takes to change a single 'pixel' to blank using the fill\n" +
      "command. This means the total cost of the fill command will be its\n" +
      "area times this.").
      getDouble(gpuClearCost) max 0

    gpuCopyCost = config.get("power.cost", "gpuCopyCost", gpuCopyCost, "" +
      "Energy it takes to move a single 'pixel' via the copy command. This\n" +
      "means the total cost of the copy command will be its area times this.").
      getDouble(gpuCopyCost) max 0

    gpuSetCost = config.get("power.cost", "gpuSetCost", gpuSetCost, "" +
      "Energy it takes to change a single 'pixel' via the set command. For\n" +
      "calls to set with a string, this means the total cost will be the\n" +
      "string length times this.").
      getDouble(gpuSetCost) max 0

    hddReadCost = config.get("power.cost", "hddReadCost", hddReadCost, "" +
      "Energy it takes read a single byte from a file system. Note that non\n" +
      "I/O operations on file systems such as `list` or `getFreeSpace` do\n" +
      "*not* consume power.").
      getDouble(hddReadCost) max 0

    hddWriteCost = config.get("power.cost", "hddWriteCost", hddWriteCost, "" +
      "Energy it takes to write a single byte to a file system.").
      getDouble(hddWriteCost) max 0

    powerSupplyCost = config.get("power.cost", "powerSupplyCost", powerSupplyCost, "" +
      "The amount of energy a power supply (item) produces per tick. This is\n" +
      "basically just a consumer, but instead of taking energy it puts it\n" +
      "back into the network. This is slightly more than what a computer\n" +
      "consumes per tick. It's meant as an easy way to powering a small\n" +
      "setup, mostly for testing. ").
      getDouble(powerSupplyCost)

    screenCost = config.get("power.cost", "screenCost", screenCost, "" +
      "The amount of energy a screen consumes per tick. If a screen cannot\n" +
      "consume the defined amount of energy it will stop rendering the text\n" +
      "that should be displayed on it. It will *not* forget that text,\n" +
      "however, so when enough power is available again it will restore the\n" +
      "previously displayed text (with any changes possibly made in the\n" +
      "meantime). Note that for multi-block screens *each* screen that is\n" +
      "part of it will consume this amount of energy per tick.").
      getDouble(screenCost) max 0

    wirelessCostPerRange = config.get("power.cost", "wirelessCostPerRange", wirelessCostPerRange, "" +
      "The amount of energy it costs to send a signal with strength one,\n" +
      "which means the signal reaches one block. This is scaled up linearly,\n" +
      "so for example to send a signal 400 blocks a signal strength of 400\n" +
      "is required, costing a total of 400 * `wirelessCostPerRange`. In\n" +
      "other words, the higher this value, the higher the cost of wireless\n" +
      "messages.\n" +
      "See also: `maxWirelessRange`.").
      getDouble(wirelessCostPerRange) max 0

    // --------------------------------------------------------------------- //

    config.getCategory("server").
      setComment("Server side settings, gameplay and security related stuff.")

    baseMemory = config.get("server.computer", "baseMemory", baseMemory, "" +
      "The base amount of memory made available in computers even if they\n" +
      "have no RAM installed. Use this if you feel you can't get enough RAM\n" +
      "using the given means, that being RAM components. Just keep in mind\n" +
      "that this is global and applies to all computers!").
      getInt(baseMemory)

    canComputersBeOwned = config.get("server.computer", "canComputersBeOwned", canComputersBeOwned, "" +
      "This determines whether computers can only be used by players that\n" +
      "are registered as users on them. Per default a newly placed computer\n" +
      "has no users. Whenever there are no users the computer is free for\n" +
      "all. Users can be managed via the Lua API (os.addUser, os.removeUser,\n" +
      "os.users). If this is true, the following interactions are only\n" +
      "possible for users:\n" +
      " - input via the keyboard.\n" +
      " - inventory management.\n" +
      " - breaking the computer block.\n" +
      "If this is set to false, all computers will always be usable by all\n" +
      "players, no matter the contents of the user list. Note that operators\n" +
      "are treated as if they were in the user list of every computer, i.e.\n" +
      "no restrictions apply to them.\n" +
      "See also: `maxUsers` and `maxUsernameLength`.").
      getBoolean(canComputersBeOwned)

    maxUsernameLength = config.get("server.computer", "maxUsernameLength", maxUsernameLength, "" +
      "Sanity check for username length for users registered with computers.\n" +
      "We store the actual user names instead of a hash to allow iterating\n" +
      "the list of registered users on the Lua side.\n" +
      "See also: `canComputersBeOwned`.").
      getInt(maxUsernameLength) max 0

    maxUsers = config.get("server.computer", "maxUsers", maxUsers, "" +
      "The maximum number of users that can be registered with a single\n" +
      "computer. This is used to avoid computers allocating unchecked\n" +
      "amounts of memory by registering an unlimited number of users.\n" +
      "See also: `canComputersBeOwned`.").
      getInt(maxUsers) max 0

    startupDelay = config.get("server.computer", "startupDelay", startupDelay, "" +
      "The time in seconds to wait after a computer has been restored before\n" +
      "it continues to run. This is meant to allow the world around the\n" +
      "computer to settle, avoiding issues such as components in neighboring\n" +
      "chunks being removed and then re-connected and other odd things that\n" +
      "might happen.").
      getDouble(startupDelay) max 0

    threads = config.get("server.computer", "threads", threads, "" +
      "The overall number of threads to use to drive computers. Whenever a\n" +
      "computer should run, for example because a signal should be processed\n" +
      "or some sleep timer expired it is queued for execution by a worker\n" +
      "thread. The higher the number of worker threads, the less likely it\n" +
      "will be that computers block each other from running, but the higher\n" +
      "the host system's load may become.").
      getInt(threads) max 1

    timeout = config.get("server.computer", "timeout", timeout, "" +
      "The time in seconds a program may run without yielding before it is\n" +
      "forcibly aborted. This is used to avoid stupidly written or malicious\n" +
      "programs blocking other computers by locking down the executor\n" +
      "threads. Note that changing this won't have any effect on computers\n" +
      "that are already running - they'll have to be rebooted for this to\n" +
      "take effect.").
      getDouble(timeout) max 0

    // --------------------------------------------------------------------- //

    fileCost = config.get("server.filesystem", "fileCost", fileCost, "" +
      "The base 'cost' of a single file or directory on a limited file\n" +
      "system, such as hard drives. When computing the used space we add\n" +
      "this cost to the real size of each file (and folders, which are zero\n" +
      "sized otherwise). This is to ensure that users cannot spam the file\n" +
      "system with an infinite number of files and/or folders. Note that the\n" +
      "size returned via the API will always be the real file size, however.").
      getInt(fileCost) max 0

    filesBuffered = config.get("server.filesystem", "filesBuffered", filesBuffered, "" +
      "Whether persistent file systems such as disk drivers should be\n" +
      "'buffered', and only written to disk when the world is saved. This\n" +
      "applies to all hard drives. The advantage of having this enabled is\n" +
      "that data will never go 'out of sync' with the computer's state if\n" +
      "the game crashes. The price is slightly higher memory consumption,\n" +
      "since all loaded files have to be kept in memory (loaded as in when\n" +
      "the hard drive is in a computer).").
      getBoolean(filesBuffered)

    maxHandles = config.get("server.filesystem", "maxHandles", maxHandles, "" +
      "The maximum number of file handles any single computer may have open\n" +
      "at a time. Note that this is *per filesystem*. Also note that this is\n" +
      "only enforced by the filesystem node - if an add-on decides to be\n" +
      "fancy it may well ignore this. Since file systems are usually\n" +
      "'virtual' this will usually not have any real impact on performance\n" +
      "and won't be noticeable on the host operating system.")
      .getInt(maxHandles) max 0

    maxReadBuffer = config.get("server.filesystem", "maxReadBuffer", maxReadBuffer, "" +
      "The maximum block size that can be read in one 'read' call on a file\n" +
      "system. This is used to limit the amount of memory a call from a user\n" +
      "program can cause to be allocated on the host side: when 'read' is,\n" +
      "called a byte array with the specified size has to be allocated. So\n" +
      "if this weren't limited, a Lua program could trigger massive memory\n" +
      "allocations regardless of the amount of RAM installed in the computer\n" +
      "it runs on. As a side effect this pretty much determines the read\n" +
      "performance of file systems.")
      .getInt(maxReadBuffer) max 0

    // --------------------------------------------------------------------- //

    commandUser = config.get("server.misc", "commandUser", commandUser, "" +
      "The user name to specify when executing a command via a command\n" +
      "block. If you leave this empty it will use the address of the network\n" +
      "node that sent the execution request - which will usually be a\n" +
      "computer.").
      getString.trim

    maxScreenHeight = config.get("server.misc", "maxScreenHeight", maxScreenHeight, "" +
      "The maximum height of multi-block screens, in blocks. This is limited\n" +
      "to avoid excessive computations for merging screens. If you really\n" +
      "need bigger screens it's probably safe to bump this quite a bit\n" +
      "before you notice anything, since at least incremental updates should\n" +
      "be very efficient (i.e. when adding/removing a single screen).")
      .getInt(maxScreenHeight) max 1

    maxScreenWidth = config.get("server.misc", "maxScreenWidth", maxScreenWidth, "" +
      "The maximum width of multi-block screens, in blocks.\n" +
      "See also: `maxScreenHeight`.")
      .getInt(maxScreenWidth) max 1

    maxWirelessRange = config.get("server.misc", "maxWirelessRange", maxWirelessRange, "" +
      "The maximum distance a wireless message can be sent. In other words,\n" +
      "this is the maximum signal strength a wireless network card supports.\n" +
      "This is used to limit the search range in which to check for modems,\n" +
      "which may or may not lead to performance issues for ridiculous\n" +
      "ranges - like, you know, more than the loaded area.\n" +
      "See also: `wirelessCostPerRange`.").
      getDouble(maxWirelessRange) max 0

    // --------------------------------------------------------------------- //

    if (config.hasChanged)
      config.save()
  }
}
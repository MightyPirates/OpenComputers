package li.cil.oc

import java.io.File
import li.cil.oc.util.ExtendedConfiguration._
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

  var blockId1 = 3650
  var blockId2 = 3651
  var blockId3 = 3652
  var blockId4 = 3653
  var itemId = 4600

  // ----------------------------------------------------------------------- //
  // client

  var maxScreenTextRenderDistance = 10.0
  var screenTextFadeStartDistance = 8.0
  var textLinearFiltering = false
  var rTreeDebugRenderer = false // *Not* to be configurable via config file.

  // ----------------------------------------------------------------------- //
  // power

  var ignorePower = false
  var ratioIndustrialCraft2 = 2.0f
  var ratioBuildCraft = 5.0f
  var ratioUniversalElectricity = 0.05f

  // power.buffer
  var bufferConverter = 100.0
  var bufferCapacitor = 50.0
  var bufferPowerSupply = 50.0

  // power.cost
  var computerCost = 1.0
  var gpuFillCost = 1.0 / 100
  var gpuClearCost = 1.0 / 400
  var gpuCopyCost = 1.0 / 200
  var gpuSetCost = 1.0 / 80
  var hddReadCost = 1.0 / 1600.0
  var hddWriteCost = 1.0 / 800.0
  var powerSupplyCost = -1.25
  var screenCost = 0.1
  var wirelessCostPerRange = 1.0 / 20.0

  // ----------------------------------------------------------------------- //
  // server

  // server.computer
  var baseMemory = 0
  var canComputersBeOwned = true
  var maxClipboard = 1024
  var maxUsernameLength = 32
  var maxUsers = 16
  var startupDelay = 0.25
  var threads = 4
  var timeout = 1.0

  // server.filesystem
  var fileCost = 512
  var bufferChanges = true
  var maxHandles = 16
  var maxReadBuffer = 8 * 1024

  // server.misc
  var commandUser = "OpenComputers"
  var maxScreenHeight = 6
  var maxScreenWidth = 8
  var maxWirelessRange = 400.0
  var rTreeMaxEntries = 10

  // ----------------------------------------------------------------------- //
  // robot

  var allowActivateBlocks = true
  var canAttackPlayers = false
  var canPlaceInAir = false
  var itemDamageRate = 0.05
  var swingRange = 0.49
  var useAndPlaceRange = 0.65

  // ----------------------------------------------------------------------- //
  // robot.delays

  var dropDelay = 0.1
  var moveDelay = 0.4
  var placeDelay = 0.3
  var suckDelay = 0.1
  var swingDelay = 0.4
  var turnDelay = 0.4
  var useDelay = 0.4

  // ----------------------------------------------------------------------- //

  def load(file: File) = {
    val config = new net.minecraftforge.common.Configuration(file)

    // --------------------------------------------------------------------- //

    val Array(a, b, c, d) = config.fetch("block.ids", Array(blockId1, blockId2, blockId3, blockId4),
      """List of block IDs the mod uses for different types of blocks.""")
    blockId1 = a
    blockId2 = b
    blockId3 = c
    blockId4 = d

    itemId = config.getItem("item", itemId,
      "The item ID used for all non-stackable items.").
      getInt(itemId)

    // --------------------------------------------------------------------- //

    config.getCategory("client").
      setComment("Client side settings, presentation and performance related stuff.")

    maxScreenTextRenderDistance = config.fetch("client.maxScreenTextRenderDistance", maxScreenTextRenderDistance,
      """|The maximum distance at which to render text on screens. Rendering text
        |can be pretty expensive, so if you have a lot of screens you'll want
        |to avoid huge numbers here. Note that this setting is client-sided, and
        |only has an impact on render performance on clients.""".stripMargin)

    screenTextFadeStartDistance = config.fetch("client.screenTextFadeStartDistance", screenTextFadeStartDistance,
      """|The distance at which to start fading out the text on screens. This is
        |purely cosmetic, to avoid text disappearing instantly when moving too far
        |away from a screen. This should have no measurable impact on performance.
        |Note that this needs OpenGL 1.4 to work, otherwise text will always just
        |instantly disappear when moving away from the screen displaying it.""".stripMargin)

    textLinearFiltering = config.fetch("client.textLinearFiltering", textLinearFiltering,
      """|Whether to apply linear filtering for text displayed on screens when the
        |screen has to be scaled down - i.e. the text is rendered at a resolution
        |lower than their native one, e.g. when the GUI scale is less than one or
        |when looking at a far away screen. This leads to smoother text for scaled
        |down text but results in characters not perfectly connecting anymore (for
        |example for box drawing characters. Look it up on Wikipedia.)""".stripMargin)

    // --------------------------------------------------------------------- //

    config.getCategory("power").
      setComment("Power settings, buffer sizes and power consumption.")

    ignorePower = config.fetch("power.ignorePower", ignorePower,
      """|Whether to ignore any power requirements. Whenever something requires
        |power to function, it will try to get the amount of energy it needs from
        |the buffer of its connector node, and in case it fails it won't perform
        |the action / trigger a shutdown / whatever. Setting this to `true` will
        |simply make the check 'is there enough energy' succeed unconditionally.
        |Note that buffers are still filled and emptied following the usual rules,
        |there just is no failure case anymore.""".stripMargin)

    ratioIndustrialCraft2 = config.fetch("power.ratioIndustrialCraft2", ratioIndustrialCraft2,
      """|Conversion ratio for IndustrialCraft2's EU. This is how many internal
        |energy units one EU generates.""".stripMargin)

    ratioBuildCraft = config.fetch("power.ratioBuildCraft", ratioBuildCraft,
      """|Conversion ratio for BuildCraft's MJ. This is how many internal
        |energy units one MJ generates.""".stripMargin)

    ratioUniversalElectricity = config.fetch("power.ratioUniversalElectricity", ratioUniversalElectricity,
      """|Conversion ratio for Universal Electricity's Joules. This is how many
        |internal energy units one Joule generates.""".stripMargin)

    // --------------------------------------------------------------------- //

    bufferCapacitor = config.fetch("power.buffer.capacitor", bufferCapacitor,
      """|The amount of energy a capacitor can store.""".stripMargin) max 0

    bufferConverter = config.fetch("power.buffer.converter", bufferConverter,
      """|The amount of energy a power converter can store.""".stripMargin) max 0

    bufferPowerSupply = config.fetch("power.buffer.powerSupply", bufferPowerSupply,
      """|The amount of energy a power supply can store.""".stripMargin) max 0

    // --------------------------------------------------------------------- //

    computerCost = config.fetch("power.cost.computer", computerCost,
      """|The amount of energy a computer consumes per tick when running.""".stripMargin) max 0

    gpuFillCost = config.fetch("power.cost.gpuFill", gpuFillCost,
      """|Energy it takes to change a single 'pixel' via the fill command. This
        |means the total cost of the fill command will be its area times this.""".stripMargin) max 0

    gpuClearCost = config.fetch("power.cost.gpuClear", gpuClearCost,
      """|Energy it takes to change a single 'pixel' to blank using the fill
        |command. This means the total cost of the fill command will be its
        |area times this.""".stripMargin) max 0

    gpuCopyCost = config.fetch("power.cost.gpuCopy", gpuCopyCost,
      """|Energy it takes to move a single 'pixel' via the copy command. This
        |means the total cost of the copy command will be its area times this.""".stripMargin) max 0

    gpuSetCost = config.fetch("power.cost.gpuSet", gpuSetCost,
      """|Energy it takes to change a single 'pixel' via the set command. For
        |calls to set with a string, this means the total cost will be the
        |string length times this.""".stripMargin) max 0

    hddReadCost = config.fetch("power.cost.hddRead", hddReadCost,
      """|Energy it takes read a single byte from a file system. Note that non
        |I/O operations on file systems such as `list` or `getFreeSpace` do
        |*not* consume power.""".stripMargin) max 0

    hddWriteCost = config.fetch("power.cost.hddWrite", hddWriteCost,
      """|Energy it takes to write a single byte to a file system.""".stripMargin) max 0

    powerSupplyCost = config.fetch("power.cost.powerSupply", powerSupplyCost,
      """|The amount of energy a power supply (item) produces per tick. This is
        |basically just a consumer, but instead of taking energy it puts it
        |back into the network. This is slightly more than what a computer
        |consumes per tick. It's meant as an easy way to powering a small
        |setup, mostly for testing.""".stripMargin)

    screenCost = config.fetch("power.cost.screen", screenCost,
      """|The amount of energy a screen consumes per displayed character per
        |tick. If a screen cannot consume the defined amount of energy it will
        |stop rendering the text that should be displayed on it. It will *not*
        |forget that text, however, so when enough power is available again it
        |will restore the previously displayed text (with any changes possibly
        |made in the meantime). Note that for multi-block screens *each*
        |screen that is part of it will consume this amount of energy per
        |tick.""".stripMargin) max 0

    wirelessCostPerRange = config.fetch("power.cost.wirelessStrength", wirelessCostPerRange,
      """|The amount of energy it costs to send a signal with strength one,
        |which means the signal reaches one block. This is scaled up linearly,
        |so for example to send a signal 400 blocks a signal strength of 400
        |is required, costing a total of 400 * `wirelessCostPerRange`. In
        |other words, the higher this value, the higher the cost of wireless
        |messages.[nl]
        |See also: `maxWirelessRange`.""".stripMargin) max 0

    // --------------------------------------------------------------------- //

    config.getCategory("server").
      setComment("Server side settings, gameplay and security related stuff.")

    baseMemory = config.fetch("server.computer.baseMemory", baseMemory,
      """|The base amount of memory made available in computers even if they
        |have no RAM installed. Use this if you feel you can't get enough RAM
        |using the given means, that being RAM components. Just keep in mind
        |that this is global and applies to all computers!""".stripMargin)

    canComputersBeOwned = config.fetch("server.computer.canComputersBeOwned", canComputersBeOwned,
      """|This determines whether computers can only be used by players that
        |are registered as users on them. Per default a newly placed computer
        |has no users. Whenever there are no users the computer is free for
        |all. Users can be managed via the Lua API (os.addUser, os.removeUser,
        |os.users). If this is true, the following interactions are only
        |possible for users:[nl]
        | - input via the keyboard.[nl]
        | - inventory management.[nl]
        | - breaking the computer block.[nl]
        |If this is set to false, all computers will always be usable by all
        |players, no matter the contents of the user list. Note that operators
        |are treated as if they were in the user list of every computer, i.e.
        |no restrictions apply to them.[nl]
        |See also: `maxUsers` and `maxUsernameLength`.""".stripMargin)

    maxClipboard = config.fetch("server.computer.maxClipboard", maxClipboard,
      """|The maximum length of a string that may be pasted. This is used to
        |limit the size of the data sent to the server when the user tries
        |to paste a string from the clipboard (Shift+Ins on a screen with a
        |keyboard).""".stripMargin)

    maxUsernameLength = config.fetch("server.computer.maxUsernameLength", maxUsernameLength,
      """|Sanity check for username length for users registered with computers.
        |We store the actual user names instead of a hash to allow iterating
        |the list of registered users on the Lua side.[nl]
        |See also: `canComputersBeOwned`.""".stripMargin) max 0

    maxUsers = config.fetch("server.computer.maxUsers", maxUsers,
      """|The maximum number of users that can be registered with a single
        |computer. This is used to avoid computers allocating unchecked
        |amounts of memory by registering an unlimited number of users.
        |See also: `canComputersBeOwned`.""".stripMargin) max 0

    startupDelay = config.fetch("server.computer.startupDelay", startupDelay,
      """|The time in seconds to wait after a computer has been restored before
        |it continues to run. This is meant to allow the world around the
        |computer to settle, avoiding issues such as components in neighboring
        |chunks being removed and then re-connected and other odd things that
        |might happen.""".stripMargin) max 0

    threads = config.fetch("server.computer.threads", threads,
      """|The overall number of threads to use to drive computers. Whenever a
        |computer should run, for example because a signal should be processed
        |or some sleep timer expired it is queued for execution by a worker
        |thread. The higher the number of worker threads, the less likely it
        |will be that computers block each other from running, but the higher
        |the host system's load may become.""".stripMargin) max 1

    timeout = config.fetch("server.computer.timeout", timeout,
      """|The time in seconds a program may run without yielding before it is
        |forcibly aborted. This is used to avoid stupidly written or malicious
        |programs blocking other computers by locking down the executor
        |threads. Note that changing this won't have any effect on computers
        |that are already running - they'll have to be rebooted for this to
        |take effect.""".stripMargin) max 0

    // --------------------------------------------------------------------- //

    fileCost = config.fetch("server.filesystem.fileCost", fileCost,
      """|The base 'cost' of a single file or directory on a limited file
        |system, such as hard drives. When computing the used space we add
        |this cost to the real size of each file (and folders, which are zero
        |sized otherwise). This is to ensure that users cannot spam the file
        |system with an infinite number of files and/or folders. Note that the
        |size returned via the API will always be the real file size, however.""".stripMargin) max 0

    bufferChanges = config.fetch("server.filesystem.bufferChanges", bufferChanges,
      """|Whether persistent file systems such as disk drivers should be
        |'buffered', and only written to disk when the world is saved. This
        |applies to all hard drives. The advantage of having this enabled is
        |that data will never go 'out of sync' with the computer's state if
        |the game crashes. The price is slightly higher memory consumption,
        |since all loaded files have to be kept in memory (loaded as in when
        |the hard drive is in a computer).""".stripMargin)

    maxHandles = config.fetch("server.filesystem.maxHandles", maxHandles,
      """|The maximum number of file handles any single computer may have open
        |at a time. Note that this is *per filesystem*. Also note that this is
        |only enforced by the filesystem node - if an add-on decides to be
        |fancy it may well ignore this. Since file systems are usually
        |'virtual' this will usually not have any real impact on performance
        |and won't be noticeable on the host operating system.""".stripMargin) max 0

    maxReadBuffer = config.fetch("server.filesystem.maxReadBuffer", maxReadBuffer,
      """|The maximum block size that can be read in one 'read' call on a file
        |system. This is used to limit the amount of memory a call from a user
        |program can cause to be allocated on the host side: when 'read' is,
        |called a byte array with the specified size has to be allocated. So
        |if this weren't limited, a Lua program could trigger massive memory
        |allocations regardless of the amount of RAM installed in the computer
        |it runs on. As a side effect this pretty much determines the read
        |performance of file systems.""".stripMargin) max 0

    // --------------------------------------------------------------------- //

    commandUser = config.fetch("server.misc.commandUser", commandUser,
      """|The user name to specify when executing a command via a command
        |block. If you leave this empty it will use the address of the network
        |node that sent the execution request - which will usually be a
        |computer.""".stripMargin).trim

    maxScreenHeight = config.fetch("server.misc.maxScreenHeight", maxScreenHeight,
      """|The maximum height of multi-block screens, in blocks. This is limited
        |to avoid excessive computations for merging screens. If you really
        |need bigger screens it's probably safe to bump this quite a bit
        |before you notice anything, since at least incremental updates should
        |be very efficient (i.e. when adding/removing a single screen).""".stripMargin) max 1

    maxScreenWidth = config.fetch("server.misc.maxScreenWidth", maxScreenWidth,
      """|The maximum width of multi-block screens, in blocks.[nl]
        |See also: `maxScreenHeight`.""".stripMargin) max 1

    maxWirelessRange = config.fetch("server.misc.maxWirelessRange", maxWirelessRange,
      """|The maximum distance a wireless message can be sent. In other words,
        |this is the maximum signal strength a wireless network card supports.
        |This is used to limit the search range in which to check for modems,
        |which may or may not lead to performance issues for ridiculous
        |ranges - like, you know, more than the loaded area.[nl]
        |See also: `wirelessCostPerRange`.""".stripMargin) max 0

    // --------------------------------------------------------------------- //

    config.getCategory("robot").
      setComment("Robot related settings, what they may do and general balancing.")

    allowActivateBlocks = config.fetch("robot.allowActivateBlocks", allowActivateBlocks,
      """|Whether robots may 'activate' blocks in the world. This includes
        |pressing buttons and flipping levers, for example. Disable this if
        |it causes problems with some mod (but let me know!) or if you think
        |this feature is too over-powered.""".stripMargin)

    canAttackPlayers = config.fetch("robot.canAttackPlayers", canAttackPlayers,
      """|Whether robots may damage players if they get in their way. This
        |includes all 'player' entities, which may be more than just real
        |players in the game.""".stripMargin)

    canPlaceInAir = config.fetch("robot.canPlaceInAir", canPlaceInAir,
      """|Whether robots may place blocks in thin air, i.e. without a reference
        |point (as is required for real players). Set this to true to emulate
        |ComputerCraft's Turtles' behavior. When left false robots have to
        |target an existing block face to place another block. For example,
        |if the robots stands on a perfect plane, you have to call
        |`robot.place(sides.down)` to place a block, instead of just
        |`robot.place()`, which will default to `robot.place(sides.front)`.""".stripMargin)

    itemDamageRate = config.fetch("robot.itemDamageRate", itemDamageRate,
      """|The rate at which items used as tools by robots take damage. A value
        |of one means that items lose durability as quickly as when they are
        |used by a real player. A value of zero means they will not lose any
        |durability at all. This only applies to items that can actually be
        |damaged (such as swords, pickaxes, axes and shovels).[nl]
        |Note that this actually is the *chance* of an item losing durability
        |when it is used. Or in other words, it's the inverse chance that the
        |item will be automatically repaired for the damage it just took
        |immediately after it was used.""".stripMargin) max 0 min 1

    swingRange = config.fetch("robot.swingRange", swingRange,
      """|The 'range' of robots when swinging an equipped tool (left click).
        |This is the distance to the center of block the robot swings the
        |tool in to the side the tool is swung towards. I.e. for the collision
        |check, which is performed via ray tracing, this determines the end
        |point of the ray like so:[nl]
        |`block_center + unit_vector_towards_side * swingRange`[nl]
        |This defaults to a value just below 0.5 to ensure the robots will not
        |hit anything that's actually outside said block.""".stripMargin)

    useAndPlaceRange = config.fetch("robot.useAndPlaceRange", useAndPlaceRange,
      """|The 'range' of robots when using an equipped tool (right click) or
        |when placing items from their inventory. See `robot.swingRange`. This
        |defaults to a value large enough to allow robots to detect 'farmland',
        |i.e. tilled dirt, so that they can plant seeds.""".stripMargin)

    // ----------------------------------------------------------------------- //
    // robot.delays

    dropDelay = config.fetch("robot.delays.drop", dropDelay,
      """|The time in seconds to pause execution after an item was successfully
        |dropped from a robot's inventory.""".stripMargin) max 0

    moveDelay = config.fetch("robot.delays.move", moveDelay,
      """|The time in seconds to pause execution after a robot issued a
        |successful move command. Note that this essentially determines how
        |fast robots can move around, since this also determines the length of
        |the move animation.""".stripMargin) max 0.1

    placeDelay = config.fetch("robot.delays.place", placeDelay,
      """|The time in seconds to pause execution after a robot successfully
        |placed an item from its inventory.""".stripMargin) max 0

    suckDelay = config.fetch("robot.delays.suck", suckDelay,
      """|The time in seconds to pause execution after a robot successfully
        |picked up an item after triggering a suck command.""".stripMargin) max 0

    swingDelay = config.fetch("robot.delays.swing", swingDelay,
      """|The time in seconds to pause execution after a robot successfully
        |swung a tool (or it's 'hands' if nothing is equipped). Successful in
        |this case means that it hit something, either by attacking an entity
        |or by breaking a block.""".stripMargin) max 0

    turnDelay = config.fetch("robot.delays.turn", turnDelay,
      """|The time in seconds to pause execution after a robot turned either
        |left or right. Note that this essentially determines hw fast robots
        |can turn around, since this also determines the length of the turn
        |animation.""".stripMargin) max 0.1

    useDelay = config.fetch("robot.delays.use", useDelay,
      """|The time in seconds to pause execution after a robot successfully
        |used an equipped tool (or it's 'hands' if nothing is equipped).
        |Successful in this case means that it either used the equipped item,
        |for example bone meal, or that it activated a block, for example by
        |pushing a button.""".stripMargin) max 0

    // --------------------------------------------------------------------- //

    if (config.hasChanged) {
      config.save()
    }
  }
}
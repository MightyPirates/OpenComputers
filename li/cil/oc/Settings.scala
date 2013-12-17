package li.cil.oc

import com.typesafe.config.{ConfigRenderOptions, Config, ConfigFactory}
import java.io._
import li.cil.oc.util.PackedColor
import scala.collection.convert.WrapAsScala._

class Settings(config: Config) {
  val itemId = config.getInt("ids.item")
  val (blockId1, blockId2, blockId3, blockId4) = Array(config.getIntList("ids.block"): _*) match {
    case Array(id1, id2, id3, id4) =>
      (id1: Int, id2: Int, id3: Int, id4: Int)
    case _ =>
      OpenComputers.log.warning("Bad number of block ids, ignoring.")
      (3650, 3651, 3652, 3653)
  }

  // ----------------------------------------------------------------------- //
  // client

  val screenTextFadeStartDistance = config.getDouble("client.screenTextFadeStartDistance")
  val maxScreenTextRenderDistance = config.getDouble("client.maxScreenTextRenderDistance")
  val textLinearFiltering = config.getBoolean("client.textLinearFiltering")
  val rTreeDebugRenderer = false // *Not* to be configurable via config file.

  // ----------------------------------------------------------------------- //
  // computer
  val threads = config.getInt("computer.threads") max 1
  val timeout = config.getDouble("computer.timeout") max 0
  val startupDelay = config.getDouble("computer.startupDelay") max 0.05
  val activeGC = config.getBoolean("computer.activeGC")
  val ramSizes = Array(config.getIntList("computer.ramSizes"): _*) match {
    case Array(tier1, tier2, tier3) =>
      Array(tier1: Int, tier2: Int, tier3: Int)
    case _ =>
      OpenComputers.log.warning("Bad number of RAM sizes, ignoring.")
      Array(64, 128, 256)
  }
  val ramScaleFor64Bit = config.getDouble("computer.ramScaleFor64Bit") max 1
  val canComputersBeOwned = config.getBoolean("computer.canComputersBeOwned")
  val maxUsers = config.getInt("computer.maxUsers") max 0
  val maxUsernameLength = config.getInt("computer.maxUsernameLength") max 0

  // ----------------------------------------------------------------------- //
  // robot

  val canPlaceInAir = config.getBoolean("robot.canPlaceInAir")
  val allowActivateBlocks = config.getBoolean("robot.allowActivateBlocks")
  val allowUseItemsWithDuration = config.getBoolean("robot.allowUseItemsWithDuration")
  val canAttackPlayers = config.getBoolean("robot.canAttackPlayers")
  val screwCobwebs = config.getBoolean("robot.notAfraidOfSpiders")
  val swingRange = config.getDouble("robot.swingRange")
  val useAndPlaceRange = config.getDouble("robot.useAndPlaceRange")
  val itemDamageRate = config.getDouble("robot.itemDamageRate") max 0 min 1
  val nameFormat = config.getString("robot.nameFormat")

  // ----------------------------------------------------------------------- //
  // robot.xp

  val baseXpToLevel = config.getDouble("robot.xp.baseValue") max 0
  val constantXpGrowth = config.getDouble("robot.xp.constantGrowth") max 1
  val exponentialXpGrowth = config.getDouble("robot.xp.exponentialGrowth") max 1
  val robotActionXp = config.getDouble("robot.xp.actionXp") max 0
  val robotExhaustionXpRate = config.getDouble("robot.xp.exhaustionXpRate") max 0
  val bufferPerLevel = config.getDouble("robot.xp.bufferPerLevel") max 0
  val toolEfficiencyPerLevel = config.getDouble("robot.xp.toolEfficiencyPerLevel") max 0
  val harvestSpeedBoostPerLevel = config.getDouble("robot.xp.harvestSpeedBoostPerLevel") max 0

  // ----------------------------------------------------------------------- //
  // robot.delays

  // Note: all delays are reduced by one tick to account for the tick they are
  // performed in (since all actions are delegated to the server thread).
  val turnDelay = (config.getDouble("robot.delays.turn") - 0.06) max 0.05
  val moveDelay = (config.getDouble("robot.delays.move") - 0.06) max 0.05
  val swingDelay = (config.getDouble("robot.delays.swing") - 0.06) max 0
  val useDelay = (config.getDouble("robot.delays.use") - 0.06) max 0
  val placeDelay = (config.getDouble("robot.delays.place") - 0.06) max 0
  val dropDelay = (config.getDouble("robot.delays.drop") - 0.06) max 0
  val suckDelay = (config.getDouble("robot.delays.suck") - 0.06) max 0
  val harvestRatio = config.getDouble("robot.delays.harvestRatio") max 0

  // ----------------------------------------------------------------------- //
  // power

  val ignorePower = config.getBoolean("power.ignorePower")
  val tickFrequency = config.getDouble("power.tickFrequency") max 1
  val ratioBuildCraft = config.getDouble("power.ratioBuildCraft").toFloat
  val ratioIndustrialCraft2 = config.getDouble("power.ratioIndustrialCraft2").toFloat
  val ratioUniversalElectricity = config.getDouble("power.ratioUniversalElectricity").toFloat
  val ratioThermalExpansion = config.getDouble("power.ratioThermalExpansion").toFloat
  val chargeRate = config.getDouble("power.chargerChargeRate")
  val generatorEfficiency = config.getDouble("power.generatorEfficiency")

  // power.buffer
  val bufferCapacitor = config.getDouble("power.buffer.capacitor") max 0
  val bufferCapacitorAdjacencyBonus = config.getDouble("power.buffer.capacitorAdjacencyBonus") max 0
  val bufferComputer = config.getDouble("power.buffer.computer") max 0
  val bufferRobot = config.getDouble("power.buffer.robot") max 0
  val bufferConverter = config.getDouble("power.buffer.converter") max 0

  // power.cost
  val computerCost = config.getDouble("power.cost.computer") max 0
  val robotCost = config.getDouble("power.cost.robot") max 0
  val sleepCostFactor = config.getDouble("power.cost.sleepFactor") max 0
  val screenCost = config.getDouble("power.cost.screen") max 0
  val hddReadCost = (config.getDouble("power.cost.hddRead") max 0) / 1024
  val hddWriteCost = (config.getDouble("power.cost.hddWrite") max 0) / 1024
  val gpuSetCost = (config.getDouble("power.cost.gpuSet") max 0) / Settings.basicScreenPixels
  val gpuFillCost = (config.getDouble("power.cost.gpuFill") max 0) / Settings.basicScreenPixels
  val gpuClearCost = (config.getDouble("power.cost.gpuClear") max 0) / Settings.basicScreenPixels
  val gpuCopyCost = (config.getDouble("power.cost.gpuCopy") max 0) / Settings.basicScreenPixels
  val robotTurnCost = config.getDouble("power.cost.robotTurn") max 0
  val robotMoveCost = config.getDouble("power.cost.robotMove") max 0
  val robotExhaustionCost = config.getDouble("power.cost.robotExhaustion") max 0
  val wirelessCostPerRange = config.getDouble("power.cost.wirelessStrength") max 0

  // ----------------------------------------------------------------------- //
  // filesystem
  val fileCost = config.getInt("filesystem.fileCost") max 0
  val bufferChanges = config.getBoolean("filesystem.bufferChanges")
  val hddSizes = Array(config.getIntList("filesystem.hddSizes"): _*) match {
    case Array(tier1, tier2, tier3) =>
      Array(tier1: Int, tier2: Int, tier3: Int)
    case _ =>
      OpenComputers.log.warning("Bad number of HDD sizes, ignoring.")
      Array(1024, 2048, 4096)
  }
  val floppySize = config.getInt("filesystem.floppySize") max 0
  val tmpSize = config.getInt("filesystem.tmpSize") max 0
  val maxHandles = config.getInt("filesystem.maxHandles") max 0
  val maxReadBuffer = config.getInt("filesystem.maxReadBuffer") max 0

  // ----------------------------------------------------------------------- //
  // http
  val httpEnabled = config.getBoolean("http.enable")
  val httpThreads = config.getInt("http.threads") max 1
  val httpHostBlacklist = Array(config.getStringList("http.blacklist"): _*)
  val httpHostWhitelist = Array(config.getStringList("http.whitelist"): _*)

  // ----------------------------------------------------------------------- //
  // misc
  val maxScreenWidth = config.getInt("misc.maxScreenWidth") max 1
  val maxScreenHeight = config.getInt("misc.maxScreenHeight") max 1
  val maxClipboard = config.getInt("misc.maxClipboard") max 0
  val commandUser = config.getString("misc.commandUser").trim
  val maxNetworkPacketSize = config.getInt("misc.maxNetworkPacketSize") max 0
  val maxWirelessRange = config.getDouble("misc.maxWirelessRange") max 0
  val rTreeMaxEntries = 10 // TODO config?
}

object Settings {
  val resourceDomain = "opencomputers"
  val namespace = "oc:"
  val savePath = "opencomputers/"
  val scriptPath = "/assets/" + resourceDomain + "/lua/"
  val screenResolutionsByTier = Array((50, 16), (80, 25), (160, 50))
  val screenDepthsByTier = Array(PackedColor.Depth.OneBit, PackedColor.Depth.FourBit, PackedColor.Depth.EightBit)

  def basicScreenPixels = screenResolutionsByTier(0)._1 * screenResolutionsByTier(0)._2

  private var settings: Settings = _

  def get = settings

  def load(file: File) = {
    val defaults = ConfigFactory.defaultReference().withOnlyPath("opencomputers")
    try {
      val config = ConfigFactory.parseFile(file).withFallback(defaults)
      settings = new Settings(config.getConfig("opencomputers"))

      val renderSettings = ConfigRenderOptions.defaults.setJson(false).setOriginComments(false)
      val out = new PrintWriter(file)
      out.write(config.root.render(renderSettings).lines.
        // Strip extra spaces in front and fix additional space in of comments.
        map(_.stripPrefix("    ").replaceAll("^(\\s*)#  ", "$1# ")).
        // Indent two spaces instead of four.
        map(line => """^(\s*)""".r.replaceAllIn(line, m => m.group(1).replace("  ", " "))).
        // Finalize the string.
        filter(_ != "").mkString("\n").
        // Newline after values.
        replaceAll( """((?:\s*#.*\n)(?:\s*[^#\s].*\n)+)""", "$1\n"))
      out.close()
    }
    catch {
      case e: Throwable =>
        OpenComputers.log.warning("Failed loading config, using defaults. The reason was: " + e.getMessage)
        settings = new Settings(defaults.getConfig("opencomputers"))
    }
  }
}
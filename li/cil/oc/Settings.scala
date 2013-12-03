package li.cil.oc

import com.typesafe.config.{Config, ConfigFactory}
import java.io.{FileOutputStream, File}
import java.nio.channels.Channels
import java.util.logging.Level
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
  val canComputersBeOwned = config.getBoolean("computer.canComputersBeOwned")
  val maxUsers = config.getInt("computer.maxUsers") max 0
  val maxUsernameLength = config.getInt("computer.maxUsernameLength") max 0

  // ----------------------------------------------------------------------- //
  // robot

  val canPlaceInAir = config.getBoolean("robot.canPlaceInAir")
  val allowActivateBlocks = config.getBoolean("robot.allowActivateBlocks")
  val allowUseItemsWithDuration = config.getBoolean("robot.allowUseItemsWithDuration")
  val canAttackPlayers = config.getBoolean("robot.canAttackPlayers")
  val swingRange = config.getDouble("robot.swingRange")
  val useAndPlaceRange = config.getDouble("robot.useAndPlaceRange")
  val itemDamageRate = config.getDouble("robot.itemDamageRate") max 0 min 1
  val nameFormat = config.getString("robot.nameFormat")

  // ----------------------------------------------------------------------- //
  // robot.delays

  val turnDelay = config.getDouble("robot.delays.turn") max 0.05
  val moveDelay = config.getDouble("robot.delays.move") max 0.05
  val swingDelay = config.getDouble("robot.delays.swing") max 0
  val useDelay = config.getDouble("robot.delays.use") max 0
  val placeDelay = config.getDouble("robot.delays.place") max 0
  val dropDelay = config.getDouble("robot.delays.drop") max 0
  val suckDelay = config.getDouble("robot.delays.suck") max 0

  // ----------------------------------------------------------------------- //
  // power

  val ignorePower = config.getBoolean("power.ignorePower")
  val ratioBuildCraft = config.getDouble("power.ratioBuildCraft").toFloat
  val ratioIndustrialCraft2 = config.getDouble("power.ratioIndustrialCraft2").toFloat
  val ratioUniversalElectricity = config.getDouble("power.ratioUniversalElectricity").toFloat
  val chargeRate = config.getDouble("power.chargerChargeRate")
  val generatorEfficiency = config.getDouble("power.generatorEfficiency")

  // power.buffer
  val bufferCapacitor = config.getDouble("power.buffer.capacitor") max 0
  val bufferCapacitorAdjacencyBonus = config.getDouble("power.buffer.capacitorAdjacencyBonus") max 0
  val bufferRobot = config.getDouble("power.buffer.robot") max 0

  // power.cost
  val computerCost = config.getDouble("power.cost.computer") max 0
  val robotCost = config.getDouble("power.cost.robot") max 0
  val sleepCostFactor = config.getDouble("power.cost.sleepFactor") max 0
  val screenCost = config.getDouble("power.cost.screen") max 0
  val hddReadCost = config.getDouble("power.cost.hddRead") max 0
  val hddWriteCost = config.getDouble("power.cost.hddWrite") max 0
  val gpuSetCost = config.getDouble("power.cost.gpuSet") max 0
  val gpuFillCost = config.getDouble("power.cost.gpuFill") max 0
  val gpuClearCost = config.getDouble("power.cost.gpuClear") max 0
  val gpuCopyCost = config.getDouble("power.cost.gpuCopy") max 0
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
      Array(2048, 4096, 8192)
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

  private var settings: Settings = _

  def get = settings

  def load(file: File) = {
    if (!file.exists() || file.length() == 0) {
      val in = Channels.newChannel(classOf[Settings].getResourceAsStream("/reference.conf"))
      val out = new FileOutputStream(file).getChannel
      out.transferFrom(in, 0, Long.MaxValue)
      in.close()
      out.close()
    }
    val defaults = ConfigFactory.defaultReference().withOnlyPath("opencomputers")
    try {
      val config = ConfigFactory.parseFile(file).withFallback(defaults)
      settings = new Settings(config.getConfig("opencomputers"))
    }
    catch {
      case e: Throwable =>
        OpenComputers.log.warning("Failed loading config, using defaults. The reason was: " + e.getMessage)
        settings = new Settings(defaults.getConfig("opencomputers"))
    }
  }
}
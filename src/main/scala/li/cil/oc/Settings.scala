package li.cil.oc

import java.io._
import java.net.{Inet4Address, InetAddress}
import java.util.logging.Level

import com.google.common.net.InetAddresses
import com.typesafe.config._
import cpw.mods.fml.common.Loader
import cpw.mods.fml.common.versioning.{DefaultArtifactVersion, VersionRange}
import li.cil.oc.api.component.TextBuffer.ColorDepth
import li.cil.oc.util.mods.Mods
import org.apache.commons.lang3.StringEscapeUtils

import scala.collection.convert.WrapAsScala._
import scala.io.Source

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
  val textAntiAlias = config.getBoolean("client.textAntiAlias")
  val robotLabels = config.getBoolean("client.robotLabels")
  val soundVolume = config.getDouble("client.soundVolume").toFloat max 0 min 2
  val fontCharScale = config.getDouble("client.fontCharScale") max 0.5 min 2
  val hologramFadeStartDistance = config.getDouble("client.hologramFadeStartDistance") max 0
  val hologramRenderDistance = config.getDouble("client.hologramRenderDistance") max 0
  val hologramFlickerFrequency = config.getDouble("client.hologramFlickerFrequency") max 0
  val logOpenGLErrors = config.getBoolean("client.logOpenGLErrors")

  // ----------------------------------------------------------------------- //
  // computer
  val threads = config.getInt("computer.threads") max 1
  val timeout = config.getDouble("computer.timeout") max 0
  val startupDelay = config.getDouble("computer.startupDelay") max 0.05
  val ramSizes = Array(config.getIntList("computer.ramSizes"): _*) match {
    case Array(tier1, tier2, tier3, tier4, tier5, tier6) =>
      Array(tier1: Int, tier2: Int, tier3: Int, tier4: Int, tier5: Int, tier6: Int)
    case _ =>
      OpenComputers.log.warning("Bad number of RAM sizes, ignoring.")
      Array(192, 256, 384, 512, 768, 1024)
  }
  val ramScaleFor64Bit = config.getDouble("computer.ramScaleFor64Bit") max 1
  val cpuComponentSupport = Array(config.getIntList("computer.cpuComponentCount"): _*) match {
    case Array(tier1, tier2, tier3) =>
      Array(tier1: Int, tier2: Int, tier3: Int)
    case _ =>
      OpenComputers.log.warning("Bad number of CPU component counts, ignoring.")
      Array(8, 12, 16)
  }
  val canComputersBeOwned = config.getBoolean("computer.canComputersBeOwned")
  val maxUsers = config.getInt("computer.maxUsers") max 0
  val maxUsernameLength = config.getInt("computer.maxUsernameLength") max 0
  val allowBytecode = config.getBoolean("computer.allowBytecode")
  val eraseTmpOnReboot = config.getBoolean("computer.eraseTmpOnReboot")
  val executionDelay = config.getInt("computer.executionDelay") max 0

  // ----------------------------------------------------------------------- //
  // computer.debug

  val logLuaCallbackErrors = config.getBoolean("computer.debug.logCallbackErrors")
  val forceLuaJ = config.getBoolean("computer.debug.forceLuaJ")
  val allowUserdata = !config.getBoolean("computer.debug.disableUserdata")
  val allowPersistence = !config.getBoolean("computer.debug.disablePersistence")
  val limitMemory = !config.getBoolean("computer.debug.disableMemoryLimit")

  // ----------------------------------------------------------------------- //
  // robot

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
  val robotOreXpRate = config.getDouble("robot.xp.oreXpRate") max 0
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

  val pureIgnorePower = config.getBoolean("power.ignorePower")
  val ignorePower = pureIgnorePower ||
    (!Mods.BuildCraftPower.isAvailable &&
      !Mods.IndustrialCraft2.isAvailable &&
      !Mods.ThermalExpansion.isAvailable &&
      !Mods.UniversalElectricity.isAvailable)
  val tickFrequency = config.getDouble("power.tickFrequency") max 1
  val chargeRate = config.getDouble("power.chargerChargeRate")
  val generatorEfficiency = config.getDouble("power.generatorEfficiency")
  val solarGeneratorEfficiency = config.getDouble("power.solarGeneratorEfficiency")
  val assemblerTickAmount = config.getDouble("power.assemblerTickAmount") max 1
  val disassemblerTickAmount = config.getDouble("power.disassemblerTickAmount") max 1

  // power.buffer
  val bufferCapacitor = config.getDouble("power.buffer.capacitor") max 0
  val bufferCapacitorAdjacencyBonus = config.getDouble("power.buffer.capacitorAdjacencyBonus") max 0
  val bufferComputer = config.getDouble("power.buffer.computer") max 0
  val bufferRobot = config.getDouble("power.buffer.robot") max 0
  val bufferConverter = config.getDouble("power.buffer.converter") max 0
  val bufferDistributor = config.getDouble("power.buffer.distributor") max 0
  val bufferCapacitorUpgrades = Array(config.getDoubleList("power.buffer.batteryUpgrades"): _*) match {
    case Array(tier1, tier2, tier3) =>
      Array(tier1: Double, tier2: Double, tier3: Double)
    case _ =>
      OpenComputers.log.warning("Bad number of battery upgrade buffer sizes, ignoring.")
      Array(10000.0, 15000.0, 20000.0)
  }

  // power.cost
  val computerCost = config.getDouble("power.cost.computer") max 0
  val robotCost = config.getDouble("power.cost.robot") max 0
  val sleepCostFactor = config.getDouble("power.cost.sleepFactor") max 0
  val screenCost = config.getDouble("power.cost.screen") max 0
  val hologramCost = config.getDouble("power.cost.hologram") max 0
  val hddReadCost = (config.getDouble("power.cost.hddRead") max 0) / 1024
  val hddWriteCost = (config.getDouble("power.cost.hddWrite") max 0) / 1024
  val gpuSetCost = (config.getDouble("power.cost.gpuSet") max 0) / Settings.basicScreenPixels
  val gpuFillCost = (config.getDouble("power.cost.gpuFill") max 0) / Settings.basicScreenPixels
  val gpuClearCost = (config.getDouble("power.cost.gpuClear") max 0) / Settings.basicScreenPixels
  val gpuCopyCost = (config.getDouble("power.cost.gpuCopy") max 0) / Settings.basicScreenPixels
  val robotTurnCost = config.getDouble("power.cost.robotTurn") max 0
  val robotMoveCost = config.getDouble("power.cost.robotMove") max 0
  val robotExhaustionCost = config.getDouble("power.cost.robotExhaustion") max 0
  val wirelessCostPerRange = config.getDouble("power.cost.wirelessCostPerRange") max 0
  val abstractBusPacketCost = config.getDouble("power.cost.abstractBusPacket") max 0
  val geolyzerScanCost = config.getDouble("power.cost.geolyzerScan") max 0
  val robotBaseCost = config.getDouble("power.cost.robotAssemblyBase") max 0
  val robotComplexityCost = config.getDouble("power.cost.robotAssemblyComplexity") max 0
  val disassemblerItemCost = config.getDouble("power.cost.disassemblerPerItem") max 0
  val chunkloaderCost = config.getDouble("power.cost.chunkloaderCost") max 0

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
  // internet
  val httpEnabled = config.getBoolean("internet.enableHttp")
  val tcpEnabled = config.getBoolean("internet.enableTcp")
  val httpHostBlacklist = Array(config.getStringList("internet.blacklist").map(new Settings.AddressValidator(_)): _*)
  val httpHostWhitelist = Array(config.getStringList("internet.whitelist").map(new Settings.AddressValidator(_)): _*)
  val httpTimeout = (config.getInt("internet.requestTimeout") max 0) * 1000
  val httpMaxDownloadSize = config.getInt("internet.requestMaxDownloadSize") max 0
  val maxConnections = config.getInt("internet.maxTcpConnections") max 0
  val internetThreads = config.getInt("internet.threads") max 1

  // ----------------------------------------------------------------------- //
  // misc
  val maxScreenWidth = config.getInt("misc.maxScreenWidth") max 1
  val maxScreenHeight = config.getInt("misc.maxScreenHeight") max 1
  val inputUsername = config.getBoolean("misc.inputUsername")
  val maxClipboard = config.getInt("misc.maxClipboard") max 0
  val maxNetworkPacketSize = config.getInt("misc.maxNetworkPacketSize") max 0
  val maxOpenPorts = config.getInt("misc.maxOpenPorts") max 0
  val maxWirelessRange = config.getDouble("misc.maxWirelessRange") max 0
  val rTreeMaxEntries = 10
  val terminalsPerTier = Array(config.getIntList("misc.terminalsPerTier"): _*) match {
    case Array(tier1, tier2, tier3) =>
      Array(math.max(tier1, 1), math.max(tier2, 1), math.max(tier3, 1))
    case _ =>
      OpenComputers.log.warning("Bad number of Remote Terminal counts, ignoring.")
      Array(2, 4, 8)
  }
  val updateCheck = config.getBoolean("misc.updateCheck")
  val alwaysTryNative = config.getBoolean("misc.alwaysTryNative")
  val lootProbability = config.getInt("misc.lootProbability")
  val debugPersistence = config.getBoolean("misc.verbosePersistenceErrors")
  val geolyzerRange = config.getInt("misc.geolyzerRange")
  val geolyzerNoise = config.getDouble("misc.geolyzerNoise").toFloat max 0
  val disassembleAllTheThings = config.getBoolean("misc.disassembleAllTheThings")
  val disassemblerBreakChance = config.getDouble("misc.disassemblerBreakChance") max 0 min 1
  val hideOwnPet = config.getBoolean("misc.hideOwnSpecial")
}

object Settings {
  val resourceDomain = "opencomputers"
  val namespace = "oc:"
  val savePath = "opencomputers/"
  val scriptPath = "/assets/" + resourceDomain + "/lua/"
  val screenResolutionsByTier = Array((50, 16), (80, 25), (160, 50))
  val screenDepthsByTier = Array(ColorDepth.OneBit, ColorDepth.FourBit, ColorDepth.EightBit)
  val hologramMaxScaleByTier = Array(3, 4)
  val robotComplexityByTier = Array(12, 24, 32, 9001)
  var rTreeDebugRenderer = false

  // Power conversion values. These are the same values used by Universal
  // Electricity to provide global power support.
  val valueBC = 56280.0
  val valueIC2 = 22512.0
  val valueTE = 5628.0
  val valueUE = 1.0

  val valueOC = valueBC

  val ratioBC = valueBC / valueOC
  val ratioIC2 = valueIC2 / valueOC
  val ratioTE = valueTE / valueOC
  val ratioUE = valueUE / valueOC

  def basicScreenPixels = screenResolutionsByTier(0)._1 * screenResolutionsByTier(0)._2

  private var settings: Settings = _

  def get = settings

  def load(file: File) = {
    // typesafe config's internal method for loading the reference.conf file
    // seems to fail on some systems (as does their parseResource method), so
    // we'll have to load the default config manually. This was reported on the
    // Minecraft Forums, I could not reproduce the issue, but this version has
    // reportedly fixed the problem.
    val defaults = {
      val in = classOf[Settings].getResourceAsStream("/reference.conf")
      val config = Source.fromInputStream(in).mkString.replace("\r\n", "\n")
      in.close()
      ConfigFactory.parseString(config)
    }
    val config =
      try {
        val plain = Source.fromFile(file).mkString.replace("\r\n", "\n")
        val config = patchConfig(ConfigFactory.parseString(plain), defaults).withFallback(defaults)
        settings = new Settings(config.getConfig("opencomputers"))
        config
      }
      catch {
        case e: Throwable =>
          if (file.exists()) {
            OpenComputers.log.log(Level.WARNING, "Failed loading config, using defaults.", e)
          }
          settings = new Settings(defaults.getConfig("opencomputers"))
          defaults
      }
    try {
      val renderSettings = ConfigRenderOptions.defaults.setJson(false).setOriginComments(false)
      val nl = sys.props("line.separator")
      val nle = StringEscapeUtils.escapeJava(nl)
      val out = new PrintWriter(file)
      out.write(config.root.render(renderSettings).lines.
        // Indent two spaces instead of four.
        map(line => """^(\s*)""".r.replaceAllIn(line, m => m.group(1).replace("  ", " "))).
        // Finalize the string.
        filter(_ != "").mkString(nl).
        // Newline after values.
        replaceAll(s"((?:\\s*#.*$nle)(?:\\s*[^#\\s].*$nle)+)", "$1" + nl))
      out.close()
    }
    catch {
      case e: Throwable =>
        OpenComputers.log.log(Level.WARNING, "Failed saving config.", e)
    }
  }

  private val configPatches = Array(
    // Upgrading to version 1.3, increased lower bounds for default RAM sizes
    // and reworked the way black- and whitelisting works (IP based).
    VersionRange.createFromVersionSpec("[0.0,1.3-alpha)") -> Array(
      "computer.ramSizes",
      "internet.blacklist",
      "internet.whitelist"
    )
  )

  // Checks the config version (i.e. the version of the mod the config was
  // created by) against the current version to see if some hard changes
  // were made. If so, the new default values are copied over.
  private def patchConfig(config: Config, defaults: Config) = {
    val mod = Loader.instance.activeModContainer
    val prefix = "opencomputers."
    val configVersion = new DefaultArtifactVersion(if (config.hasPath(prefix + "version")) config.getString(prefix + "version") else "0.0.0")
    var patched = config
    if (configVersion.compareTo(mod.getProcessedVersion) != 0) {
      OpenComputers.log.info(s"Updating config from version '${configVersion.getVersionString}' to '${defaults.getString(prefix + "version")}'.")
      patched = patched.withValue(prefix + "version", defaults.getValue(prefix + "version"))
      for ((version, paths) <- configPatches if version.containsVersion(configVersion)) {
        for (path <- paths) {
          val fullPath = prefix + path
          OpenComputers.log.info(s"Updating setting '$fullPath'. ")
          patched = patched.withValue(fullPath, defaults.getValue(fullPath))
        }
      }
    }
    patched
  }

  val cidrPattern = """(\d{1,3}\.\d{1,3}\.\d{1,3}\.\d{1,3})(?:/(\d{1,2}))""".r

  class AddressValidator(val value: String) {
    val validator = try cidrPattern.findFirstIn(value) match {
      case Some(cidrPattern(address, prefix)) =>
        val addr = InetAddresses.coerceToInteger(InetAddresses.forString(address))
        val mask = 0xFFFFFFFF << (32 - prefix.toInt)
        val min = addr & mask
        val max = min | ~mask
        (inetAddress: InetAddress, host: String) => inetAddress match {
          case v4: Inet4Address =>
            val numeric = InetAddresses.coerceToInteger(v4)
            min <= numeric && numeric <= max
          case _ => true // Can't check IPv6 addresses so we pass them.
        }
      case _ =>
        val address = InetAddress.getByName(value)
        (inetAddress: InetAddress, host: String) => host == value || inetAddress == address
    } catch {
      case t: Throwable =>
        OpenComputers.log.log(Level.WARNING, "Invalid entry in internet blacklist / whitelist: " + value, t)
        (inetAddress: InetAddress, host: String) => true
    }

    def apply(inetAddress: InetAddress, host: String) = validator(inetAddress, host)
  }

}
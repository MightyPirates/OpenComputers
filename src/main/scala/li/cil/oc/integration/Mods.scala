package li.cil.oc.integration

import cpw.mods.fml.common.Loader
import cpw.mods.fml.common.ModAPIManager
import cpw.mods.fml.common.versioning.VersionParser
import li.cil.oc.Settings
import li.cil.oc.integration

import scala.collection.mutable

object Mods {
  private val handlers = mutable.Set.empty[ModProxy]

  private val knownMods = mutable.ArrayBuffer.empty[ModBase]

  lazy val isPowerProvidingModPresent = knownMods.exists(mod => mod.providesPower && mod.isAvailable)

  // ----------------------------------------------------------------------- //

  def All = knownMods.clone()

  val AppliedEnergistics2 = new SimpleMod(IDs.AppliedEnergistics2, version = "@[rv1,)", providesPower = true)
  val BattleGear2 = new SimpleMod(IDs.BattleGear2)
  val BuildCraft = new SimpleMod(IDs.BuildCraft)
  val BuildCraftTiles = new SimpleMod(IDs.BuildCraftTiles)
  val BuildCraftTools = new SimpleMod(IDs.BuildCraftTools)
  val BuildCraftTransport = new SimpleMod(IDs.BuildCraftTransport)
  val CoFHEnergy = new SimpleMod(IDs.CoFHEnergy, providesPower = true)
  val CoFHItem = new SimpleMod(IDs.CoFHItem)
  val CoFHTileEntity = new SimpleMod(IDs.CoFHTileEntity)
  val CoFHTransport = new SimpleMod(IDs.CoFHTransport)
  val ComputerCraft = new SimpleMod(IDs.ComputerCraft)
  val CraftingCosts = new SimpleMod(IDs.CraftingCosts)
  val ElectricalAge = new SimpleMod(IDs.ElectricalAge)
  val EnderIO = new SimpleMod(IDs.EnderIO)
  val EnderStorage = new SimpleMod(IDs.EnderStorage)
  val Factorization = new SimpleMod(IDs.Factorization, providesPower = true)
  val Forestry = new SimpleMod(IDs.Forestry)
  val ForgeMultipart = new SimpleMod(IDs.ForgeMultipart)
  val Galacticraft = new SimpleMod(IDs.Galacticraft, providesPower = true)
  val GregTech = new SimpleMod(IDs.GregTech)
  val IndustrialCraft2 = new SimpleMod(IDs.IndustrialCraft2, providesPower = true)
  val IndustrialCraft2Classic = new SimpleMod(IDs.IndustrialCraft2Classic, providesPower = true)
  val Mekanism = new SimpleMod(IDs.Mekanism, providesPower = true)
  val Minecraft = new SimpleMod(IDs.Minecraft)
  val MineFactoryReloaded = new SimpleMod(IDs.MineFactoryReloaded)
  val Mystcraft = new SimpleMod(IDs.Mystcraft)
  val NotEnoughItems = new SimpleMod(IDs.NotEnoughItems)
  val OpenComputers = new SimpleMod(IDs.OpenComputers)
  val PortalGun = new SimpleMod(IDs.PortalGun)
  val ProjectRedTransmission = new SimpleMod(IDs.ProjectRedTransmission)
  val Railcraft = new SimpleMod(IDs.Railcraft)
  val RedLogic = new SimpleMod(IDs.RedLogic)
  val StargateTech2 = new ModBase {
    def id = IDs.StargateTech2

    protected override lazy val isModAvailable = Loader.isModLoaded(IDs.StargateTech2) && {
      val mod = Loader.instance.getIndexedModList.get(IDs.StargateTech2)
      mod.getVersion.startsWith("0.7.")
    }
  }
  val Thaumcraft = new SimpleMod(IDs.Thaumcraft)
  val ThermalExpansion = new SimpleMod(IDs.ThermalExpansion, providesPower = true)
  val TinkersConstruct = new SimpleMod(IDs.TinkersConstruct)
  val TMechWorks = new SimpleMod(IDs.TMechWorks)
  val VersionChecker = new SimpleMod(IDs.VersionChecker)
  val Waila = new SimpleMod(IDs.Waila)
  val WirelessRedstoneCBE = new SimpleMod(IDs.WirelessRedstoneCBE)
  val WirelessRedstoneSVE = new SimpleMod(IDs.WirelessRedstoneSV)

  // ----------------------------------------------------------------------- //

  val Proxies = Array(
    integration.appeng.ModAppEng,
    integration.buildcraft.tools.ModBuildCraftAPITools,
    integration.buildcraft.tiles.ModBuildCraftAPITiles,
    integration.buildcraft.transport.ModBuildCraftAPITransport,
    integration.cofh.energy.ModCoFHEnergy,
    integration.cofh.item.ModCoFHItem,
    integration.cofh.tileentity.ModCoFHTileEntity,
    integration.cofh.transport.ModCoFHTransport,
    integration.enderstorage.ModEnderStorage,
    integration.forestry.ModForestry,
    integration.fmp.ModForgeMultipart,
    integration.gc.ModGalacticraft,
    integration.gregtech.ModGregtech,
    integration.ic2.ModIndustrialCraft2,
    integration.mfr.ModMineFactoryReloaded,
    integration.mystcraft.ModMystcraft,
    integration.railcraft.ModRailcraft,
    integration.stargatetech2.ModStargateTech2,
    integration.thaumcraft.ModThaumcraft,
    integration.thermalexpansion.ModThermalExpansion,
    integration.tcon.ModTinkersConstruct,
    integration.tmechworks.ModTMechworks,
    integration.vanilla.ModVanilla,
    integration.versionchecker.ModVersionChecker,
    integration.waila.ModWaila,
    integration.wrcbe.ModWRCBE,
    integration.wrsve.ModWRSVE,

    // Register the general IPeripheral driver last, if at all, to avoid it
    // being used rather than other more concrete implementations.
    integration.computercraft.ModComputerCraft,

    // We go last to ensure all other mod integration is done, e.g. to
    // allow properly checking if wireless redstone is present.
    integration.opencomputers.ModOpenComputers
  )

  def init(): Unit = {
    for (proxy <- Proxies) {
      tryInit(proxy)
    }
  }

  private def tryInit(mod: ModProxy) {
    val isBlacklisted = Settings.get.modBlacklist.contains(mod.getMod.id)
    val alwaysEnabled = mod.getMod == null || mod.getMod == Mods.Minecraft
    if (!isBlacklisted && (alwaysEnabled || mod.getMod.isAvailable) && handlers.add(mod)) {
      li.cil.oc.OpenComputers.log.info(s"Initializing mod integration for '${mod.getMod.id}'.")
      try mod.initialize() catch {
        case e: Throwable =>
          li.cil.oc.OpenComputers.log.warn(s"Error initializing integration for '${mod.getMod.id}'", e)
      }
    }
  }

  // ----------------------------------------------------------------------- //

  object IDs {
    final val AppliedEnergistics2 = "appliedenergistics2"
    final val BattleGear2 = "battlegear2"
    final val BuildCraft = "BuildCraft|Core"
    final val BuildCraftPower = "BuildCraftAPI|power"
    final val BuildCraftTiles = "BuildCraftAPI|tiles"
    final val BuildCraftTools = "BuildCraftAPI|tools"
    final val BuildCraftTransport = "BuildCraftAPI|transport"
    final val CoFHEnergy = "CoFHAPI|energy"
    final val CoFHItem = "CoFHAPI|item"
    final val CoFHTileEntity = "CoFHAPI|tileentity"
    final val CoFHTransport = "CoFHAPI|transport"
    final val ComputerCraft = "ComputerCraft"
    final val CraftingCosts = "CraftingCosts"
    final val ElectricalAge = "Eln"
    final val EnderIO = "EnderIO"
    final val EnderStorage = "EnderStorage"
    final val Factorization = "factorization"
    final val Forestry = "Forestry"
    final val ForgeMultipart = "ForgeMultipart"
    final val Galacticraft = "Galacticraft API"
    final val GregTech = "gregtech"
    final val IndustrialCraft2 = "IC2"
    final val IndustrialCraft2Classic = "IC2-Classic"
    final val Mekanism = "Mekanism"
    final val Minecraft = "Minecraft"
    final val MineFactoryReloaded = "MineFactoryReloaded"
    final val Mystcraft = "Mystcraft"
    final val NotEnoughItems = "NotEnoughItems"
    final val OpenComputers = "OpenComputers"
    final val PortalGun = "PortalGun"
    final val ProjectRedTransmission = "ProjRed|Transmission"
    final val Railcraft = "Railcraft"
    final val RedLogic = "RedLogic"
    final val StargateTech2 = "StargateTech2"
    final val Thaumcraft = "Thaumcraft"
    final val ThermalExpansion = "ThermalExpansion"
    final val TinkersConstruct = "TConstruct"
    final val TMechWorks = "TMechworks"
    final val VersionChecker = "VersionChecker"
    final val Waila = "Waila"
    final val WirelessRedstoneCBE = "WR-CBE|Core"
    final val WirelessRedstoneSV = "WirelessRedstoneCore"
  }

  // ----------------------------------------------------------------------- //

  trait ModBase extends Mod {
    knownMods += this

    private var powerDisabled = false

    protected lazy val isPowerModEnabled = !providesPower || (!Settings.get.pureIgnorePower && !Settings.get.powerModBlacklist.contains(id))

    protected def isModAvailable: Boolean

    def id: String

    def isAvailable = !powerDisabled && isModAvailable && isPowerModEnabled

    def providesPower: Boolean = false

    // This is called from the class transformer when injecting an interface of
    // this power type fails, to avoid class not found / class cast exceptions.
    def disablePower() = powerDisabled = true
  }

  class SimpleMod(val id: String, override val providesPower: Boolean = false, version: String = "") extends ModBase {
    override protected lazy val isModAvailable = {
      val version = VersionParser.parseVersionReference(id + this.version)
      if (Loader.isModLoaded(version.getLabel))
        version.containsVersion(Loader.instance.getIndexedModList.get(version.getLabel).getProcessedVersion)
      else ModAPIManager.INSTANCE.hasAPI(version.getLabel)
    }
  }

  class ClassBasedMod(val id: String, val classNames: String*)(override val providesPower: Boolean) extends ModBase {
    override protected lazy val isModAvailable = classNames.forall(className => try Class.forName(className) != null catch {
      case _: Throwable => false
    })
  }

}

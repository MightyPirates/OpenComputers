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

  // ----------------------------------------------------------------------- //

  def All = knownMods.clone()

  val AgriCraft = new SimpleMod(IDs.AgriCraft, version = "@[1.4.0,)")
  val AppliedEnergistics2 = new SimpleMod(IDs.AppliedEnergistics2, version = "@[rv1,)")
  val AvaritiaAddons = new SimpleMod(IDs.AvaritiaAddons)
  val BattleGear2 = new SimpleMod(IDs.BattleGear2)
  val BetterRecords = new SimpleMod(IDs.BetterRecords)
  val BloodMagic = new SimpleMod(IDs.BloodMagic)
  val BluePower = new SimpleMod(IDs.BluePower, version = "@[0.2.928,)")
  val BuildCraft = new SimpleMod(IDs.BuildCraft)
  val BuildCraftLibrary = new SimpleMod(IDs.BuildCraftLibrary, version = "@[7.0.9,)")
  val BuildCraftRecipes = new SimpleMod(IDs.BuildCraftRecipes)
  val BuildCraftTiles = new SimpleMod(IDs.BuildCraftTiles)
  val BuildCraftTools = new SimpleMod(IDs.BuildCraftTools)
  val BuildCraftTransport = new SimpleMod(IDs.BuildCraftTransport)
  val CoFHEnergy = new SimpleMod(IDs.CoFHEnergy)
  val CoFHItem = new SimpleMod(IDs.CoFHItem)
  val CoFHTileEntity = new SimpleMod(IDs.CoFHTileEntity)
  val CoFHTransport = new SimpleMod(IDs.CoFHTransport)
  val ColoredLights = new SimpleMod(IDs.ColoredLights)
  val ComputerCraft = new SimpleMod(IDs.ComputerCraft, version = "@[1.73,)")
  val CraftingCosts = new SimpleMod(IDs.CraftingCosts)
  val DeepStorageUnit = new ClassBasedMod(IDs.DeepStorageUnit, "powercrystals.minefactoryreloaded.api.IDeepStorageUnit")
  val ElectricalAge = new SimpleMod(IDs.ElectricalAge)
  val EnderIO = new SimpleMod(IDs.EnderIO, version = "@[2.2,)")
  val EnderStorage = new SimpleMod(IDs.EnderStorage)
  val ExtraCells = new SimpleMod(IDs.ExtraCells, version = "@[2.2.73,)")
  val Factorization = new SimpleMod(IDs.Factorization)
  val Forestry = new SimpleMod(IDs.Forestry, version = "@[4.0,)")
  val ForgeMultipart = new SimpleMod(IDs.ForgeMultipart)
  val Galacticraft = new SimpleMod(IDs.Galacticraft)
  val GregTech = new ClassBasedMod(IDs.GregTech, "gregtech.api.GregTech_API")
  val IndustrialCraft2 = new SimpleMod(IDs.IndustrialCraft2)
  val IndustrialCraft2Classic = new SimpleMod(IDs.IndustrialCraft2Classic)
  val IngameWiki = new SimpleMod(IDs.IngameWiki, version = "@[1.1.3,)")
  val Mekanism = new SimpleMod(IDs.Mekanism)
  val MekanismGas = new SimpleMod(IDs.MekanismGas)
  val Minecraft = new SimpleMod(IDs.Minecraft)
  val MineFactoryReloaded = new SimpleMod(IDs.MineFactoryReloaded)
  val Mystcraft = new SimpleMod(IDs.Mystcraft)
  val NotEnoughItems = new SimpleMod(IDs.NotEnoughItems)
  val NotEnoughKeys = new SimpleMod(IDs.NotEnoughKeys)
  val OpenComputers = new SimpleMod(IDs.OpenComputers)
  val PortalGun = new SimpleMod(IDs.PortalGun)
  val ProjectRedCore = new SimpleMod(IDs.ProjectRedCore)
  val ProjectRedTransmission = new SimpleMod(IDs.ProjectRedTransmission)
  val Railcraft = new SimpleMod(IDs.Railcraft)
  val RedLogic = new SimpleMod(IDs.RedLogic)
  val RotaryCraft = new SimpleMod(IDs.RotaryCraft)
  val StargateTech2 = new ModBase {
    def id = IDs.StargateTech2

    private lazy val isModAvailable_ = Loader.isModLoaded(IDs.StargateTech2) && {
      val mod = Loader.instance.getIndexedModList.get(IDs.StargateTech2)
      mod.getVersion.startsWith("0.7.")
    }

    override def isModAvailable: Boolean = isModAvailable_
  }
  val Thaumcraft = new SimpleMod(IDs.Thaumcraft)
  val ThaumicEnergistics = new SimpleMod(IDs.ThaumicEnergistics)
  val ThermalExpansion = new SimpleMod(IDs.ThermalExpansion)
  val TinkersConstruct = new SimpleMod(IDs.TinkersConstruct)
  val TIS3D = new SimpleMod(IDs.TIS3D, version = "@[0.7,)")
  val TMechWorks = new SimpleMod(IDs.TMechWorks)
  val VersionChecker = new SimpleMod(IDs.VersionChecker)
  val Waila = new SimpleMod(IDs.Waila)
  val WirelessRedstoneCBE = new SimpleMod(IDs.WirelessRedstoneCBE)
  val WirelessRedstoneSVE = new SimpleMod(IDs.WirelessRedstoneSV)

  // ----------------------------------------------------------------------- //

  val Proxies = Array(
    integration.agricraft.ModAgriCraft,
    integration.appeng.ModAppEng,
    integration.avaritiaaddons.ModAvaritiaAddons,
    integration.betterrecords.ModBetterRecords,
    integration.bloodmagic.ModBloodMagic,
    integration.bluepower.ModBluePower,
    integration.buildcraft.library.ModBuildCraftAPILibrary,
    integration.buildcraft.recipes.ModBuildCraftRecipes,
    integration.buildcraft.tools.ModBuildCraftAPITools,
    integration.buildcraft.tiles.ModBuildCraftAPITiles,
    integration.buildcraft.transport.ModBuildCraftAPITransport,
    integration.cofh.energy.ModCoFHEnergy,
    integration.cofh.item.ModCoFHItem,
    integration.cofh.tileentity.ModCoFHTileEntity,
    integration.cofh.transport.ModCoFHTransport,
    integration.ec.ModExtraCells,
    integration.enderio.ModEnderIO,
    integration.enderstorage.ModEnderStorage,
    integration.dsu.ModDeepStorageUnit,
    integration.forestry.ModForestry,
    integration.fmp.ModForgeMultipart,
    integration.gc.ModGalacticraft,
    integration.gregtech.ModGregtech,
    integration.ic2.ModIndustrialCraft2,
    integration.mekanism.ModMekanism,
    integration.mekanism.gas.ModMekanismGas,
    integration.mfr.ModMineFactoryReloaded,
    integration.mystcraft.ModMystcraft,
    integration.nek.ModNotEnoughKeys,
    integration.projectred.ModProjectRed,
    integration.railcraft.ModRailcraft,
    integration.redlogic.ModRedLogic,
    integration.rotarycraft.ModRotaryCraft,
    integration.stargatetech2.ModStargateTech2,
    integration.thaumcraft.ModThaumcraft,
    integration.thaumicenergistics.ModThaumicEnergistics,
    integration.thermalexpansion.ModThermalExpansion,
    integration.tcon.ModTinkersConstruct,
    integration.tis3d.ModTIS3D,
    integration.tmechworks.ModTMechworks,
    integration.vanilla.ModVanilla,
    integration.waila.ModWaila,
    integration.wrcbe.ModWRCBE,
    integration.wrsve.ModWRSVE,

    // Register the general IPeripheral driver last, if at all, to avoid it
    // being used rather than other more concrete implementations.
    integration.computercraft.ModComputerCraft,

    // We go late to ensure all other mod integration is done, e.g. to
    // allow properly checking if wireless redstone is present.
    integration.opencomputers.ModOpenComputers,

    // Run IGW registration after OC registration because we use the manual
    // in there to know which pages to register.
    integration.igw.ModIngameWiki
  )

  def init(): Unit = {
    for (proxy <- Proxies) {
      tryInit(proxy)
    }
  }

  private def tryInit(mod: ModProxy) {
    val isBlacklisted = Settings.get.modBlacklist.contains(mod.getMod.id)
    val alwaysEnabled = mod.getMod == null || mod.getMod == Mods.Minecraft
    if (!isBlacklisted && (alwaysEnabled || mod.getMod.isModAvailable) && handlers.add(mod)) {
      li.cil.oc.OpenComputers.log.debug(s"Initializing mod integration for '${mod.getMod.id}'.")
      try mod.initialize() catch {
        case e: Throwable =>
          li.cil.oc.OpenComputers.log.warn(s"Error initializing integration for '${mod.getMod.id}'", e)
      }
    }
  }

  // ----------------------------------------------------------------------- //

  object IDs {
    final val AgriCraft = "AgriCraft"
    final val AppliedEnergistics2 = "appliedenergistics2"
    final val AvaritiaAddons = "avaritiaddons"
    final val BattleGear2 = "battlegear2"
    final val BetterRecords = "betterrecords"
    final val BloodMagic = "AWWayofTime"
    final val BluePower = "bluepowerAPI"
    final val BuildCraft = "BuildCraft|Core"
    final val BuildCraftLibrary = "BuildCraftAPI|library"
    final val BuildCraftRecipes = "BuildCraftAPI|recipes"
    final val BuildCraftTiles = "BuildCraftAPI|tiles"
    final val BuildCraftTools = "BuildCraftAPI|tools"
    final val BuildCraftTransport = "BuildCraftAPI|transport"
    final val CoFHEnergy = "CoFHAPI|energy"
    final val CoFHItem = "CoFHAPI|item"
    final val CoFHTileEntity = "CoFHAPI|tileentity"
    final val CoFHTransport = "CoFHAPI|transport"
    final val ColoredLights = "easycoloredlights"
    final val ComputerCraft = "ComputerCraft"
    final val CraftingCosts = "CraftingCosts"
    final val ElectricalAge = "Eln"
    final val EnderIO = "EnderIO"
    final val EnderStorage = "EnderStorage"
    final val ExtraCells = "extracells"
    final val Factorization = "factorization"
    final val Forestry = "Forestry"
    final val ForgeMultipart = "ForgeMultipart"
    final val DeepStorageUnit = "MineFactoryReloaded|DeepStorageUnit" // Doesn't really exist.
    final val Galacticraft = "Galacticraft API"
    final val GregTech = "gregtech"
    final val IndustrialCraft2 = "IC2"
    final val IndustrialCraft2Classic = "IC2-Classic"
    final val IndustrialCraft2Spmod = "IC2-Classic-Spmod"
    final val IngameWiki = "IGWMod"
    final val Mekanism = "Mekanism"
    final val MekanismGas = "MekanismAPI|gas"
    final val Minecraft = "Minecraft"
    final val MineFactoryReloaded = "MineFactoryReloaded"
    final val Mystcraft = "Mystcraft"
    final val NotEnoughItems = "NotEnoughItems"
    final val NotEnoughKeys = "notenoughkeys"
    final val OpenComputers = "OpenComputers"
    final val PortalGun = "PortalGun"
    final val ProjectRedCore = "ProjRed|Core"
    final val ProjectRedTransmission = "ProjRed|Transmission"
    final val Railcraft = "Railcraft"
    final val RedLogic = "RedLogic"
    final val RotaryCraft = "RotaryCraft"
    final val StargateTech2 = "StargateTech2"
    final val Thaumcraft = "Thaumcraft"
    final val ThaumicEnergistics = "thaumicenergistics"
    final val ThermalExpansion = "ThermalExpansion"
    final val TinkersConstruct = "TConstruct"
    final val TIS3D = "tis3d"
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

    def isModAvailable: Boolean

    def id: String

    def isAvailable = isModAvailable

    // This is called from the class transformer when injecting an interface of
    // this power type fails, to avoid class not found / class cast exceptions.
    def disablePower() = powerDisabled = true

    def container = Option(Loader.instance.getIndexedModList.get(id))

    def version = container.map(_.getProcessedVersion)
  }

  class SimpleMod(val id: String, version: String = "") extends ModBase {
    private lazy val isModAvailable_ = {
      val version = VersionParser.parseVersionReference(id + this.version)
      if (Loader.isModLoaded(version.getLabel))
        version.containsVersion(Loader.instance.getIndexedModList.get(version.getLabel).getProcessedVersion)
      else ModAPIManager.INSTANCE.hasAPI(version.getLabel)
    }

    def isModAvailable = isModAvailable_
  }

  class ClassBasedMod(val id: String, val classNames: String*) extends ModBase {
    private lazy val isModAvailable_ = classNames.forall(className => try Class.forName(className) != null catch {
      case _: Throwable => false
    })

    def isModAvailable = isModAvailable_
  }

}

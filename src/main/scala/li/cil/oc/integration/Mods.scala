package li.cil.oc.integration

import li.cil.oc.Settings
import li.cil.oc.integration
import net.minecraftforge.fml.common.Loader
import net.minecraftforge.fml.common.ModAPIManager
import net.minecraftforge.fml.common.versioning.ArtifactVersion
import net.minecraftforge.fml.common.versioning.VersionParser

import scala.collection.mutable
import scala.collection.mutable.ArrayBuffer

object Mods {
  private val handlers = mutable.Set.empty[ModProxy]

  private val knownMods = mutable.ArrayBuffer.empty[ModBase]

  // ----------------------------------------------------------------------- //

  def All: ArrayBuffer[ModBase] = knownMods.clone()
  val AppliedEnergistics2 = new ClassBasedMod(IDs.AppliedEnergistics2, "appeng.api.storage.channels.IItemStorageChannel")
  val ComputerCraft = new SimpleMod(IDs.ComputerCraft)
  val ExtraCells = new SimpleMod(IDs.ExtraCells, version = "@[2.5.2,)")
  val Forestry = new SimpleMod(IDs.Forestry, version = "@[5.2,)")
  val IndustrialCraft2 = new SimpleMod(IDs.IndustrialCraft2)
  val Forge = new SimpleMod(IDs.Forge)
  val JustEnoughItems = new SimpleMod(IDs.JustEnoughItems)
  val Mekanism = new SimpleMod(IDs.Mekanism)
  val MekanismGas = new SimpleMod(IDs.MekanismGas)
  val Minecraft = new SimpleMod(IDs.Minecraft)
  val OpenComputers = new SimpleMod(IDs.OpenComputers)
  val TIS3D = new SimpleMod(IDs.TIS3D, version = "@[0.9,)")
  val Waila = new SimpleMod(IDs.Waila)
  val ProjectRedBase = new SimpleMod((IDs.ProjectRedCore))
  val ProjectRedTransmission = new SimpleMod((IDs.ProjectRedTransmission))
  val DraconicEvolution = new SimpleMod(IDs.DraconicEvolution)
  val EnderStorage = new SimpleMod(IDs.EnderStorage)
  val Thaumcraft = new SimpleMod(IDs.Thaumcraft)
  val Charset = new SimpleMod(IDs.Charset)
  val WirelessRedstoneCBE = new SimpleMod(IDs.WirelessRedstoneCBE)

  // ----------------------------------------------------------------------- //

  val Proxies = Array(
    integration.appeng.ModAppEng,
    integration.ec.ModExtraCells,
    integration.forestry.ModForestry,
    integration.ic2.ModIndustrialCraft2,
    integration.minecraftforge.ModMinecraftForge,
    integration.tis3d.ModTIS3D,
    integration.mekanism.ModMekanism,
    integration.mekanism.gas.ModMekanismGas,
    integration.minecraft.ModMinecraft,
    integration.waila.ModWaila,
    integration.projectred.ModProjectRed,
    integration.computercraft.ModComputerCraft,
    integration.enderstorage.ModEnderStorage,
    integration.thaumcraft.ModThaumcraft,
    integration.charset.ModCharset,
    integration.wrcbe.ModWRCBE,

    // We go late to ensure all other mod integration is done, e.g. to
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
    final val AppliedEnergistics2 = "appliedenergistics2"
    final val ComputerCraft = "computercraft"
    final val ExtraCells = "extracells"
    final val Forestry = "forestry"
    final val Forge = "forge"
    final val IndustrialCraft2 = "ic2"
    final val JustEnoughItems = "jei"
    final val Mekanism = "mekanism"
    final val MekanismGas = "MekanismAPI|gas"
    final val Minecraft = "minecraft"
    final val OpenComputers = "opencomputers"
    final val TIS3D = "tis3d"
    final val Waila = "waila"
    final val ProjectRedCore = "projectred-core"
    final val ProjectRedTransmission = "projectred-transmission"
    final val DraconicEvolution = "draconicevolution"
    final val EnderStorage = "enderstorage"
    final val Thaumcraft = "thaumcraft"
    final val Charset = "charset"
    final val WirelessRedstoneCBE = "wrcbe"
  }

  // ----------------------------------------------------------------------- //

  trait ModBase extends Mod {
    knownMods += this

    def isModAvailable: Boolean

    def id: String

    def container = Option(Loader.instance.getIndexedModList.get(id))

    def version: Option[ArtifactVersion] = container.map(_.getProcessedVersion)
  }

  class SimpleMod(val id: String, version: String = "") extends ModBase {
    private lazy val isModAvailable_ = {
      val version = VersionParser.parseVersionReference(id + this.version)
      if (Loader.isModLoaded(version.getLabel))
        version.containsVersion(Loader.instance.getIndexedModList.get(version.getLabel).getProcessedVersion)
      else ModAPIManager.INSTANCE.hasAPI(version.getLabel)
    }

    def isModAvailable: Boolean = isModAvailable_
  }

  class ClassBasedMod(val id: String, val classNames: String*) extends ModBase {
    private lazy val isModAvailable_ = Loader.isModLoaded(id) && classNames.forall(className => try Class.forName(className) != null catch {
      case _: Throwable => false
    })

    def isModAvailable: Boolean = isModAvailable_
  }

}

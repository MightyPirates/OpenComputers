package li.cil.oc.integration

import java.util.Optional

import li.cil.oc.Settings
import li.cil.oc.integration
import net.minecraftforge.fml.ModList
import net.minecraftforge.fml.ModContainer
import net.minecraftforge.forgespi.language.MavenVersionAdapter
import org.apache.maven.artifact.versioning.ArtifactVersion

import scala.collection.mutable
import scala.collection.mutable.ArrayBuffer

object Mods {
  private var preInited = false
  private var inited = false

  private val knownMods = mutable.ArrayBuffer.empty[ModBase]

  // ----------------------------------------------------------------------- //

  def All: ArrayBuffer[ModBase] = knownMods.clone()
  val AppliedEnergistics2 = new ClassBasedMod(IDs.AppliedEnergistics2, "appeng.api.storage.channels.IItemStorageChannel")
  val ComputerCraft = new SimpleMod(IDs.ComputerCraft)
  val Forge = new SimpleMod(IDs.Forge)
  val JustEnoughItems = new SimpleMod(IDs.JustEnoughItems)
  val Mekanism = new SimpleMod(IDs.Mekanism)
  val Minecraft = new SimpleMod(IDs.Minecraft)
  val OpenComputers = new SimpleMod(IDs.OpenComputers)
  val TIS3D = new SimpleMod(IDs.TIS3D, version = "[0.9,)")
  val Waila = new SimpleMod(IDs.Waila)
  val ProjectRedTransmission = new SimpleMod((IDs.ProjectRedTransmission))
  val DraconicEvolution = new SimpleMod(IDs.DraconicEvolution)
  val EnderStorage = new SimpleMod(IDs.EnderStorage)

  // ----------------------------------------------------------------------- //

  val Proxies = Array(
    integration.appeng.ModAppEng,
    integration.minecraftforge.ModMinecraftForge,
    integration.tis3d.ModTIS3D,
    integration.mekanism.ModMekanism,
    integration.minecraft.ModMinecraft,
    integration.waila.ModWaila,
    integration.projectred.ModProjectRed,
    integration.computercraft.ModComputerCraft,
    integration.enderstorage.ModEnderStorage,

    // We go late to ensure all other mod integration is done, e.g. to
    // allow properly checking if wireless redstone is present.
    integration.opencomputers.ModOpenComputers
  )

  def preInit(): Unit = {
    if (!preInited) {
      preInited = true
      for (proxy <- Proxies) {
        tryPreInit(proxy)
      }
    }
  }

  private def tryPreInit(mod: ModProxy) {
    val handlers = mutable.Set.empty[ModProxy]
    val isBlacklisted = Settings.get.modBlacklist.contains(mod.getMod.id)
    val alwaysEnabled = mod.getMod == null || mod.getMod == Mods.Minecraft
    if (!isBlacklisted && (alwaysEnabled || mod.getMod.isModAvailable) && handlers.add(mod)) {
      li.cil.oc.OpenComputers.log.debug(s"Pre-initializing mod integration for '${mod.getMod.id}'.")
      try mod.preInitialize() catch {
        case e: Throwable =>
          li.cil.oc.OpenComputers.log.warn(s"Error pre-initializing integration for '${mod.getMod.id}'", e)
      }
    }
  }

  def init(): Unit = {
    if (!inited) {
      inited = true
      for (proxy <- Proxies) {
        tryInit(proxy)
      }
    }
  }

  private def tryInit(mod: ModProxy) {
    val handlers = mutable.Set.empty[ModProxy]
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
    final val Forge = "forge"
    final val JustEnoughItems = "jei"
    final val Mekanism = "mekanism"
    final val Minecraft = "minecraft"
    final val OpenComputers = "opencomputers"
    final val TIS3D = "tis3d"
    final val Waila = "waila"
    final val ProjectRedTransmission = "projectred-transmission"
    final val DraconicEvolution = "draconicevolution"
    final val EnderStorage = "enderstorage"
  }

  // ----------------------------------------------------------------------- //

  private def optionToScala[T](opt: Optional[T]): Option[T] = if (opt.isPresent) Some(opt.get) else None

  trait ModBase extends Mod {
    knownMods += this

    def isModAvailable: Boolean

    def id: String

    def container: Option[ModContainer] = optionToScala(ModList.get.getModContainerById(id))

    def version: Option[ArtifactVersion] = container.map(_.getModInfo.getVersion)
  }

  class SimpleMod(val id: String, version: String = "") extends ModBase {
    private lazy val isModAvailable_ = optionToScala(ModList.get.getModContainerById(id)) match {
      case Some(container) => version.isEmpty || MavenVersionAdapter.createFromVersionSpec(version).containsVersion(container.getModInfo.getVersion)
      case _ => false
    }

    def isModAvailable: Boolean = isModAvailable_
  }

  class ClassBasedMod(val id: String, val classNames: String*) extends ModBase {
    private lazy val isModAvailable_ = ModList.get.isLoaded(id) && classNames.forall(className => try Class.forName(className) != null catch {
      case _: Throwable => false
    })

    def isModAvailable: Boolean = isModAvailable_
  }

}

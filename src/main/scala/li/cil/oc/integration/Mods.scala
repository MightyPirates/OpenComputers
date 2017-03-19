package li.cil.oc.integration

import li.cil.oc.Settings
import li.cil.oc.integration
import net.minecraftforge.fml.common.Loader
import net.minecraftforge.fml.common.ModAPIManager
import net.minecraftforge.fml.common.versioning.VersionParser

import scala.collection.mutable

object Mods {
  private val handlers = mutable.Set.empty[ModProxy]

  private val knownMods = mutable.ArrayBuffer.empty[ModBase]

  lazy val isPowerProvidingModPresent = knownMods.exists(mod => mod.providesPower && mod.isAvailable)

  // ----------------------------------------------------------------------- //

  def All = knownMods.clone()
  val Forestry = new SimpleMod(IDs.Forestry, version = "@[5.2,)")
  val JustEnoughItems = new SimpleMod(IDs.JustEnoughItems)
  val Minecraft = new SimpleMod(IDs.Minecraft)
  val OpenComputers = new SimpleMod(IDs.OpenComputers)
  val TIS3D = new SimpleMod(IDs.TIS3D, version = "@[0.9,)")

  // ----------------------------------------------------------------------- //

  val Proxies = Array(
    integration.forestry.ModForestry,
    integration.tis3d.ModTIS3D,
    integration.minecraft.ModMinecraft,

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
    val isBlacklisted = Settings.Integration.modBlacklist.contains(mod.getMod.id)
    val alwaysEnabled = mod.getMod == null || mod.getMod == Mods.Minecraft
    if (!isBlacklisted && (alwaysEnabled || mod.getMod.isModAvailable) && handlers.add(mod)) {
      li.cil.oc.OpenComputers.log.info(s"Initializing mod integration for '${mod.getMod.id}'.")
      try mod.initialize() catch {
        case e: Throwable =>
          li.cil.oc.OpenComputers.log.warn(s"Error initializing integration for '${mod.getMod.id}'", e)
      }
    }
  }

  // ----------------------------------------------------------------------- //

  object IDs {
    final val Forestry = "forestry"
    final val JustEnoughItems = "JEI"
    final val Minecraft = "Minecraft"
    final val OpenComputers = "OpenComputers"
    final val TIS3D = "tis3d"
  }

  // ----------------------------------------------------------------------- //

  trait ModBase extends Mod {
    knownMods += this

    private var powerDisabled = false

    protected lazy val isPowerModEnabled = !providesPower || (!Settings.Power.ignorePower && !Settings.Power.powerModBlacklist.contains(id))

    def isModAvailable: Boolean

    def id: String

    def isAvailable = !powerDisabled && isModAvailable && isPowerModEnabled

    def providesPower: Boolean = false

    // This is called from the class transformer when injecting an interface of
    // this power type fails, to avoid class not found / class cast exceptions.
    def disablePower() = powerDisabled = true

    def container = Option(Loader.instance.getIndexedModList.get(id))

    def version = container.map(_.getProcessedVersion)
  }

  class SimpleMod(val id: String, override val providesPower: Boolean = false, version: String = "") extends ModBase {
    private lazy val isModAvailable_ = {
      val version = VersionParser.parseVersionReference(id + this.version)
      if (Loader.isModLoaded(version.getLabel))
        version.containsVersion(Loader.instance.getIndexedModList.get(version.getLabel).getProcessedVersion)
      else ModAPIManager.INSTANCE.hasAPI(version.getLabel)
    }

    def isModAvailable = isModAvailable_
  }

  class ClassBasedMod(val id: String, val classNames: String*)(override val providesPower: Boolean = false) extends ModBase {
    private lazy val isModAvailable_ = classNames.forall(className => try Class.forName(className) != null catch {
      case _: Throwable => false
    })

    def isModAvailable = isModAvailable_
  }

}

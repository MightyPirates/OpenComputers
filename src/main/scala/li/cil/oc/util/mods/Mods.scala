package li.cil.oc.util.mods

import cpw.mods.fml.common.versioning.VersionParser
import cpw.mods.fml.common.{Loader, ModAPIManager}
import li.cil.oc.Settings

import scala.collection.mutable

object Mods {

  object IDs {
    final val BattleGear2 = "battlegear2"
    final val BuildCraftPower = "BuildCraftAPI|power"
    final val ComputerCraft = "ComputerCraft"
    final val Factorization = "factorization"
    final val ForgeMultipart = "ForgeMultipart"
    final val GregTech = "gregtech_addon"
    final val IndustrialCraft2 = "IC2"
    final val IndustrialCraft2Classic = "IC2-Classic"
    final val Mekanism = "Mekanism"
    final val MineFactoryReloaded = "MineFactoryReloaded"
    final val NotEnoughItems = "NotEnoughItems"
    final val PortalGun = "PortalGun"
    final val ProjectRedTransmission = "ProjRed|Transmission"
    final val RedLogic = "RedLogic"
    final val StargateTech2 = "StargateTech2"
    final val ThermalExpansion = "ThermalExpansion"
    final val TinkersConstruct = "TConstruct"
    final val UniversalElectricity = "UniversalElectricity"
    final val Waila = "Waila"
    final val WirelessRedstoneCBE = "WR-CBE|Core"
    final val WirelessRedstoneSV = "WirelessRedstoneCore"
  }

  private val knownMods = mutable.ArrayBuffer.empty[Mod]

  lazy val isPowerProvidingModPresent = knownMods.exists(mod => mod.providesPower && mod.isAvailable)

  val BattleGear2 = new SimpleMod(IDs.BattleGear2)
  val BuildCraftPower = new SimpleMod(IDs.BuildCraftPower, providesPower = true)
  val ComputerCraft15 = new SimpleMod(IDs.ComputerCraft) {
    override val isAvailable = isModLoaded && (try Class.forName("dan200.computer.api.ComputerCraftAPI") != null catch {
      case _: Throwable => false
    })
  }
  val ComputerCraft16 = new SimpleMod(IDs.ComputerCraft) {
    override val isAvailable = isModLoaded && (try Class.forName("dan200.computercraft.api.ComputerCraftAPI") != null catch {
      case _: Throwable => false
    })
  }
  val ComputerCraft = new Mod {
    def id = IDs.ComputerCraft

    override def isAvailable = ComputerCraft15.isAvailable || ComputerCraft16.isAvailable
  }
  val Factorization = new SimpleMod(IDs.Factorization, providesPower = true)
  val ForgeMultipart = new SimpleMod(IDs.ForgeMultipart)
  val GregTech = new SimpleMod(IDs.GregTech)
  val IndustrialCraft2 = new SimpleMod(IDs.IndustrialCraft2, providesPower = true)
  val IndustrialCraft2Classic = new SimpleMod(IDs.IndustrialCraft2Classic, providesPower = true)
  val Mekanism = new SimpleMod(IDs.Mekanism, providesPower = true)
  val MineFactoryReloaded = new SimpleMod(IDs.MineFactoryReloaded)
  val NotEnoughItems = new SimpleMod(IDs.NotEnoughItems)
  val PortalGun = new SimpleMod(IDs.PortalGun)
  val ProjectRedTransmission = new SimpleMod(IDs.ProjectRedTransmission)
  val RedLogic = new SimpleMod(IDs.RedLogic)
  val StargateTech2 = new Mod {
    def id = IDs.StargateTech2

    val isAvailable = Loader.isModLoaded(IDs.StargateTech2) && {
      val mod = Loader.instance.getIndexedModList.get(IDs.StargateTech2)
      mod.getVersion.startsWith("0.7.")
    }
  }
  val ThermalExpansion = new SimpleMod(IDs.ThermalExpansion, providesPower = true)
  val TinkersConstruct = new SimpleMod(IDs.TinkersConstruct)
  val UniversalElectricity = new SimpleMod(IDs.UniversalElectricity + "@[3.1,)", providesPower = true)
  val Waila = new SimpleMod(IDs.Waila)
  val WirelessRedstoneCBE = new SimpleMod(IDs.WirelessRedstoneCBE)
  val WirelessRedstoneSV = new SimpleMod(IDs.WirelessRedstoneSV)

  trait Mod {
    knownMods += this

    def id: String

    def isAvailable: Boolean

    def providesPower: Boolean = false
  }

  class SimpleMod(val id: String, override val providesPower: Boolean = false) extends Mod {
    protected val isModLoaded = {
      val version = VersionParser.parseVersionReference(id)
      if (Loader.isModLoaded(version.getLabel))
        version.containsVersion(Loader.instance.getIndexedModList.get(version.getLabel).getProcessedVersion)
      else ModAPIManager.INSTANCE.hasAPI(version.getLabel)
    }

    protected val isPowerModEnabled = !providesPower || (!Settings.get.pureIgnorePower && !Settings.get.powerModBlacklist.contains(id))

    override def isAvailable = isModLoaded && isPowerModEnabled
  }

}

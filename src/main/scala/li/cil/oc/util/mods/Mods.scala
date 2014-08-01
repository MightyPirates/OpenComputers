package li.cil.oc.util.mods

import cpw.mods.fml.common.versioning.VersionParser
import cpw.mods.fml.common.{Loader, ModAPIManager}

object Mods {

  object IDs {
    final val BattleGear2 = "battlegear2"
    final val BuildCraftPower = "BuildCraftAPI|power"
    final val ComputerCraft = "ComputerCraft"
    final val ElectricalAge = "Eln"
    final val ForgeMultipart = "ForgeMultipart"
    final val GregTech = "gregtech"
    final val IndustrialCraft2 = "IC2API"
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

  val BattleGear2 = new SimpleMod(IDs.BattleGear2)
  val BuildCraftPower = new SimpleMod(IDs.BuildCraftPower)
  val ComputerCraft = new SimpleMod(IDs.ComputerCraft)
  val ElectricalAge = new SimpleMod(IDs.ElectricalAge)
  val ForgeMultipart = new SimpleMod(IDs.ForgeMultipart)
  val GregTech = new SimpleMod(IDs.GregTech)
  val IndustrialCraft2 = new SimpleMod(IDs.IndustrialCraft2)
  val Mekanism = new SimpleMod(IDs.Mekanism)
  val MineFactoryReloaded = new SimpleMod(IDs.MineFactoryReloaded)
  val NotEnoughItems = new SimpleMod(IDs.NotEnoughItems)
  val PortalGun = new SimpleMod(IDs.PortalGun)
  val ProjectRedTransmission = new SimpleMod(IDs.ProjectRedTransmission)
  val RedLogic = new SimpleMod(IDs.RedLogic)
  val StargateTech2 = new Mod {
    val isAvailable = Loader.isModLoaded(IDs.StargateTech2) && {
      val mod = Loader.instance.getIndexedModList.get(IDs.StargateTech2)
      mod.getVersion.startsWith("0.7.")
    }
  }
  val ThermalExpansion = new SimpleMod(IDs.ThermalExpansion)
  val TinkersConstruct = new SimpleMod(IDs.TinkersConstruct)
  val UniversalElectricity = new SimpleMod(IDs.UniversalElectricity + "@[3.1,)")
  val Waila = new SimpleMod(IDs.Waila)
  val WirelessRedstoneCBE = new SimpleMod(IDs.WirelessRedstoneCBE)
  val WirelessRedstoneSV = new SimpleMod(IDs.WirelessRedstoneSV)

  trait Mod {
    def isAvailable: Boolean
  }

  class SimpleMod(val id: String) extends Mod {
    protected val isModLoaded = {
      val version = VersionParser.parseVersionReference(id)
      if (Loader.isModLoaded(version.getLabel))
        version.containsVersion(Loader.instance.getIndexedModList.get(version.getLabel).getProcessedVersion)
      else ModAPIManager.INSTANCE.hasAPI(version.getLabel)
    }

    override def isAvailable = isModLoaded
  }

}

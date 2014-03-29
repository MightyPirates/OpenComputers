package li.cil.oc.util.mods

import cpw.mods.fml.common.versioning.VersionParser
import cpw.mods.fml.common.{ModAPIManager, Loader}

object Mods {
  val BuildCraftPower = new Mod("BuildCraftAPI|power")
  val ComputerCraft = new Mod("ComputerCraft@[1.6,1.50)")
  val ForgeMultipart = new Mod("ForgeMultipart")
  val GregTech = new Mod("gregtech_addon")
  val IndustrialCraft2 = new Mod("IC2")
  val MineFactoryReloaded = new Mod("MineFactoryReloaded")
  val NotEnoughItems = new Mod("NotEnoughItems")
  val PortalGun = new Mod("PortalGun")
  val ProjectRed = new Mod("ProjRed|Transmission")
  val RedLogic = new Mod("RedLogic")
  val StargateTech2 = new Mod("StargateTech2@[0.6.0,)")
  val ThermalExpansion = new Mod("ThermalExpansion")
  val UniversalElectricity = new Mod("UniversalElectricity@[3.1,)")

  class Mod(val id: String) {
    val isAvailable = {
      val version = VersionParser.parseVersionReference(id)
      if (Loader.isModLoaded(version.getLabel))
        version.containsVersion(Loader.instance.getIndexedModList.get(version.getLabel).getProcessedVersion)
      else ModAPIManager.INSTANCE.hasAPI(version.getLabel)
    }
  }

}

package li.cil.oc.integration.cofh.energy

import li.cil.oc.api
import li.cil.oc.api.Driver
import li.cil.oc.integration.ModProxy
import li.cil.oc.integration.Mods
import net.minecraftforge.common.MinecraftForge

object ModCoFHEnergy extends ModProxy {
  override def getMod = Mods.CoFHEnergy

  private val versionsUsingSplitEnergyAPI = VersionRange.createFromVersionSpec("[1.0.0,)")

  override def initialize() {
    api.IMC.registerToolDurabilityProvider("li.cil.oc.integration.cofh.energy.EventHandlerRedstoneFlux.getDurability")
    api.IMC.registerItemCharge(
      "RedstoneFlux",
      "li.cil.oc.integration.cofh.energy.EventHandlerRedstoneFlux.canCharge",
      "li.cil.oc.integration.cofh.energy.EventHandlerRedstoneFlux.charge")

    MinecraftForge.EVENT_BUS.register(EventHandlerRedstoneFlux)

    val apiVersion = ModAPIManager.INSTANCE.getAPIList.find(_.getModId == Mods.IDs.CoFHEnergy).map(_.getProcessedVersion)
    if (apiVersion.exists(versionsUsingSplitEnergyAPI.containsVersion)) {
      Driver.add(new DriverEnergyProvider)
      Driver.add(new DriverEnergyReceiver)
    }
    else {
      Driver.add(new DriverEnergyHandler)
    }

    Driver.add(new ConverterEnergyContainerItem)
  }
}
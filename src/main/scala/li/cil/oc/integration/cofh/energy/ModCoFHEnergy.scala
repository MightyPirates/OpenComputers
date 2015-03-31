package li.cil.oc.integration.cofh.energy

import cpw.mods.fml.common.ModAPIManager
import cpw.mods.fml.common.event.FMLInterModComms
import cpw.mods.fml.common.versioning.VersionRange
import li.cil.oc.api.Driver
import li.cil.oc.integration.ModProxy
import li.cil.oc.integration.Mods
import net.minecraftforge.common.MinecraftForge

import scala.collection.convert.WrapAsScala._

object ModCoFHEnergy extends ModProxy {
  override def getMod = Mods.CoFHEnergy

  private val versionsUsingSplitEnergyAPI = VersionRange.createFromVersionSpec("[1.0.0,)")

  override def initialize() {
    FMLInterModComms.sendMessage(Mods.IDs.OpenComputers, "registerToolDurabilityProvider", "li.cil.oc.integration.cofh.energy.EventHandlerRedstoneFlux.getDurability")

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
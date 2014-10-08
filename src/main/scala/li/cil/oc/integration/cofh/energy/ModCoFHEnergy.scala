package li.cil.oc.integration.cofh.energy

import li.cil.oc.api.Driver
import li.cil.oc.integration.ModProxy
import li.cil.oc.integration.Mods
import net.minecraftforge.common.MinecraftForge

object ModCoFHEnergy extends ModProxy {
  override def getMod = Mods.CoFHEnergy

  override def initialize() {
    Driver.add(new DriverEnergyHandler)

    MinecraftForge.EVENT_BUS.register(EventHandlerRedstoneFlux)

    Driver.add(new ConverterEnergyContainerItem)
  }
}
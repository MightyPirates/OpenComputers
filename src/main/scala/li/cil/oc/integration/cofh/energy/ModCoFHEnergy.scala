package li.cil.oc.integration.cofh.energy

import cpw.mods.fml.common.event.FMLInterModComms
import li.cil.oc.api.Driver
import li.cil.oc.integration.ModProxy
import li.cil.oc.integration.Mods
import net.minecraftforge.common.MinecraftForge

object ModCoFHEnergy extends ModProxy {
  override def getMod = Mods.CoFHEnergy

  override def initialize() {
    FMLInterModComms.sendMessage(Mods.IDs.OpenComputers, "registerToolDurabilityProvider", "li.cil.oc.integration.cofh.energy.EventHandlerRedstoneFlux.getDurability")

    MinecraftForge.EVENT_BUS.register(EventHandlerRedstoneFlux)

    Driver.add(new DriverEnergyHandler)

    Driver.add(new ConverterEnergyContainerItem)
  }
}
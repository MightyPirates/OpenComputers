package li.cil.oc.integration.ic2

import cpw.mods.fml.common.event.FMLInterModComms
import li.cil.oc.api.Driver
import li.cil.oc.integration.{ModProxy, Mods}
import net.minecraftforge.common.MinecraftForge

object ModIndustrialCraft2 extends ModProxy {
  override def getMod = Mods.IndustrialCraft2

  override def initialize() {
    FMLInterModComms.sendMessage("OpenComputers", "registerToolDurabilityProvider", "li.cil.oc.integration.ic2.EventHandlerIndustrialCraft2.getDurability")

    MinecraftForge.EVENT_BUS.register(EventHandlerIndustrialCraft2)

    Driver.add(new DriverEnergyConductor)
    Driver.add(new DriverEnergySink)
    Driver.add(new DriverEnergySource)
    Driver.add(new DriverEnergyStorage)
    Driver.add(new DriverMassFab)
    Driver.add(new DriverReactor)
    Driver.add(new DriverReactorChamber)

    Driver.add(new ConverterElectricItem)
  }
}

package li.cil.oc.integration.ic2

import cpw.mods.fml.common.Loader
import li.cil.oc.api
import li.cil.oc.api.Driver
import li.cil.oc.integration.ModProxy
import li.cil.oc.integration.Mods
import net.minecraftforge.common.MinecraftForge

object ModIndustrialCraft2 extends ModProxy {
  override def getMod = Mods.IndustrialCraft2

  override def initialize() {
    api.IMC.registerToolDurabilityProvider("li.cil.oc.integration.ic2.EventHandlerIndustrialCraft2.getDurability")
    api.IMC.registerWrenchTool("li.cil.oc.integration.ic2.EventHandlerIndustrialCraft2.useWrench")
    api.IMC.registerWrenchToolCheck("li.cil.oc.integration.ic2.EventHandlerIndustrialCraft2.isWrench")
    api.IMC.registerItemCharge(
      "IndustrialCraft2",
      "li.cil.oc.integration.ic2.EventHandlerIndustrialCraft2.canCharge",
      "li.cil.oc.integration.ic2.EventHandlerIndustrialCraft2.charge")

    MinecraftForge.EVENT_BUS.register(EventHandlerIndustrialCraft2)

    if (!Loader.isModLoaded(Mods.IDs.IndustrialCraft2Spmod)) {
      Driver.add(new DriverReactorRedstonePort)
      Driver.add(new DriverMassFab)
    }

    Driver.add(new DriverEnergyConductor)
    Driver.add(new DriverEnergySink)
    Driver.add(new DriverEnergySource)
    Driver.add(new DriverEnergyStorage)
    Driver.add(new DriverReactor)
    Driver.add(new DriverReactorChamber)
    Driver.add(new DriverTeleporter)

    Driver.add(new ConverterElectricItem)
    Driver.add(new ConverterBaseSeed)
    Driver.add(new DriverCrop)
  }
}

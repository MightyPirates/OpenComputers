package li.cil.oc.integration.ic2

import li.cil.oc.api.Driver
import li.cil.oc.integration.ModProxy
import li.cil.oc.integration.Mods

object ModIndustrialCraft2 extends ModProxy {
  override def getMod = Mods.IndustrialCraft2

  override def initialize() {
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
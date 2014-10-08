package li.cil.oc.integration.cofh.energy

import li.cil.oc.api.Driver
import li.cil.oc.integration.IMod
import li.cil.oc.integration.Mods

object ModCoFHEnergy extends IMod {
  override def getMod = Mods.CoFHEnergy

  override def initialize() {
    Driver.add(new DriverEnergyHandler)
    Driver.add(new ConverterEnergyContainerItem)
  }
}
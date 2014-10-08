package li.cil.oc.integration.thermalexpansion

import li.cil.oc.api.Driver
import li.cil.oc.integration.IMod
import li.cil.oc.integration.Mods

object ModThermalExpansion extends IMod {
  override def getMod = Mods.ThermalExpansion

  override def initialize() {
    Driver.add(new DriverLamp)
  }
}
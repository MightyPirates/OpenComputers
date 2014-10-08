package li.cil.oc.integration.thermalexpansion

import li.cil.oc.api.Driver
import li.cil.oc.integration.ModProxy
import li.cil.oc.integration.Mods

object ModThermalExpansion extends ModProxy {
  override def getMod = Mods.ThermalExpansion

  override def initialize() {
    Driver.add(new DriverLamp)
  }
}
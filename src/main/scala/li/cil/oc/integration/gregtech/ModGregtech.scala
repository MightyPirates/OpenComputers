package li.cil.oc.integration.gregtech

import li.cil.oc.api.Driver
import li.cil.oc.integration.ModProxy
import li.cil.oc.integration.Mods

object ModGregtech extends ModProxy {
  override def getMod = Mods.GregTech

  override def initialize() {
    Driver.add(new DriverEnergyContainer)
  }
}
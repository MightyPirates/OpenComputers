package li.cil.oc.integration.gregtech

import li.cil.oc.api.Driver
import li.cil.oc.integration.IMod
import li.cil.oc.integration.Mods

object ModGregtech extends IMod {
  override def getMod = Mods.GregTech

  override def initialize() {
    Driver.add(new DriverEnergyContainer)
  }
}
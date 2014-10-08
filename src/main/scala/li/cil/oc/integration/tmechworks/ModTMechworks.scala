package li.cil.oc.integration.tmechworks

import li.cil.oc.api.Driver
import li.cil.oc.integration.IMod
import li.cil.oc.integration.Mods

object ModTMechworks extends IMod {
  override def getMod = Mods.TMechWorks

  override def initialize() {
    Driver.add(new DriverDrawBridge)
  }
}
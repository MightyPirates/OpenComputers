package li.cil.oc.integration.tmechworks

import li.cil.oc.api.Driver
import li.cil.oc.integration.ModProxy
import li.cil.oc.integration.Mods

object ModTMechworks extends ModProxy {
  override def getMod = Mods.TMechWorks

  override def initialize() {
    Driver.add(new DriverDrawBridge)
  }
}
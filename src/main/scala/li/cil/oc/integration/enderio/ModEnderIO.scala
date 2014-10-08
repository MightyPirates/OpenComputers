package li.cil.oc.integration.enderio

import li.cil.oc.api.Driver
import li.cil.oc.integration.IMod
import li.cil.oc.integration.Mods

object ModEnderIO extends IMod {
  override def getMod = Mods.EnderIO

  override def initialize() {
    Driver.add(new DriverCapacitor)
  }
}
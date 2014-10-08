package li.cil.oc.integration.railcraft

import li.cil.oc.api.Driver
import li.cil.oc.integration.ModProxy
import li.cil.oc.integration.Mods

object ModRailcraft extends ModProxy {
  override def getMod = Mods.Railcraft

  override def initialize() {
    Driver.add(new DriverBoilerFirebox)
    Driver.add(new DriverSteamTurbine)
  }
}
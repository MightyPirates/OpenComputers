package li.cil.oc.integration.railcraft

import li.cil.oc.api.Driver
import li.cil.oc.integration.IMod
import li.cil.oc.integration.Mods

object ModRailcraft extends IMod {
  override def getMod = Mods.Railcraft

  override def initialize() {
    Driver.add(new DriverBoilerFirebox)
    Driver.add(new DriverSteamTurbine)
  }
}
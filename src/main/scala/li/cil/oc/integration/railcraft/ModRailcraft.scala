package li.cil.oc.integration.railcraft

import li.cil.oc.api
import li.cil.oc.api.Driver
import li.cil.oc.integration.ModProxy
import li.cil.oc.integration.Mods

object ModRailcraft extends ModProxy {
  override def getMod = Mods.Railcraft

  override def initialize() {
    api.IMC.registerWrenchTool("li.cil.oc.integration.railcraft.EventHandlerRailcraft.useWrench")
    api.IMC.registerWrenchToolCheck("li.cil.oc.integration.railcraft.EventHandlerRailcraft.isWrench")

    Driver.add(new DriverBoilerFirebox)
    Driver.add(new DriverSteamTurbine)
    Driver.add(DriverAnchor)
  }
}
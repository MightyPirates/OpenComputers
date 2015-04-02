package li.cil.oc.integration.fsp

import li.cil.oc.api.Driver
import li.cil.oc.integration.ModProxy
import li.cil.oc.integration.Mods

object ModFSP extends ModProxy{
  override def getMod = Mods.FlaxbeardsSteamPower

  override def initialize() {
    Driver.add(DriverSteamBoiler)
    Driver.add(DriverValvePipe)
    Driver.add(DriverSteamGauge)
  }

}

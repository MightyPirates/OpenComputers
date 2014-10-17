package li.cil.oc.integration.buildcraft

import li.cil.oc.api.Driver
import li.cil.oc.integration.ModProxy
import li.cil.oc.integration.Mods

object ModBuildCraft extends ModProxy {
  override def getMod = Mods.BuildCraft

  override def initialize() {
    Driver.add(new DriverPipeTE)
    Driver.add(new DriverPowerReceptor)
    Driver.add(new DriverMachine)
  }
}
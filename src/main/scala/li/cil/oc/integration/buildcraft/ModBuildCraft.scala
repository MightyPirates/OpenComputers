package li.cil.oc.integration.buildcraft

import li.cil.oc.api.Driver
import li.cil.oc.integration.IMod
import li.cil.oc.integration.Mods

object ModBuildCraft extends IMod {
  override def getMod = Mods.BuildCraft

  override def initialize() {
    Driver.add(new DriverPipeTE)
    Driver.add(new DriverPowerReceptor)
    Driver.add(new DriverMachine)
  }
}
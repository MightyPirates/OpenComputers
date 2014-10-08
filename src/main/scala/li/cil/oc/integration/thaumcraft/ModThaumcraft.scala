package li.cil.oc.integration.thaumcraft

import li.cil.oc.api.Driver
import li.cil.oc.integration.IMod
import li.cil.oc.integration.Mods

object ModThaumcraft extends IMod {
  override def getMod = Mods.Thaumcraft

  override def initialize() {
    Driver.add(new DriverAspectContainer)
    Driver.add(new ConverterIAspectContainer)
  }
}
package li.cil.oc.integration.forestry

import li.cil.oc.api.Driver
import li.cil.oc.integration.IMod
import li.cil.oc.integration.Mods

object ModForestry extends IMod {
  override def getMod = Mods.Forestry

  override def initialize() {
    Driver.add(new ConverterIAlleles)
    Driver.add(new ConverterIIndividual)
    Driver.add(new DriverBeeHouse)
  }
}
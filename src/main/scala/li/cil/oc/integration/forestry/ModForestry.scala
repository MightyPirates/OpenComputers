package li.cil.oc.integration.forestry

import li.cil.oc.api.Driver
import li.cil.oc.integration.ModProxy
import li.cil.oc.integration.Mods

object ModForestry extends ModProxy {
  override def getMod = Mods.Forestry

  override def initialize() {
    Driver.add(new ConverterIAlleles)
    Driver.add(new ConverterIIndividual)
    Driver.add(ConverterItemStack)
    Driver.add(new DriverAnalyzer)
    Driver.add(new DriverBeeHouse)
  }
}
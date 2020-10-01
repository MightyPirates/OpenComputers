package li.cil.oc.integration.thaumcraft

import li.cil.oc.api.Driver
import li.cil.oc.integration.ModProxy
import li.cil.oc.integration.Mods

object ModThaumcraft extends ModProxy {
  override def getMod = Mods.Thaumcraft

  override def initialize() {
    Driver.add(new DriverAspectContainer)
    Driver.add(ConverterAspectItem)
  }
}
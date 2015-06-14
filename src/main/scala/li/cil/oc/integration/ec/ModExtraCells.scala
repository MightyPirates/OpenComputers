package li.cil.oc.integration.ec

import li.cil.oc.api.Driver
import li.cil.oc.integration.{Mods, Mod, ModProxy}

object ModExtraCells extends ModProxy{
  override def getMod: Mod = Mods.ExtraCells

  override def initialize(): Unit = {
    Driver.add(DriverController)
    Driver.add(DriverInterface)
  }
}

package li.cil.oc.integration.ec

import li.cil.oc.api.Driver
import li.cil.oc.integration.Mod
import li.cil.oc.integration.ModProxy
import li.cil.oc.integration.Mods

object ModExtraCells extends ModProxy {
  override def getMod: Mod = Mods.ExtraCells

  override def initialize(): Unit = {
    Driver.add(DriverController)
    Driver.add(DriverBlockInterface)

    Driver.add(DriverController.Provider)
    Driver.add(DriverBlockInterface.Provider)
  }
}
package li.cil.oc.integration.thaumicenergistics

import li.cil.oc.api.Driver
import li.cil.oc.integration.Mod
import li.cil.oc.integration.ModProxy
import li.cil.oc.integration.Mods

object ModThaumicEnergistics extends ModProxy {
  override def getMod: Mod = Mods.ThaumicEnergistics

  override def initialize(): Unit = {
    Driver.add(DriverController)
    Driver.add(DriverBlockInterface)

    Driver.add(DriverController.Provider)
    Driver.add(DriverBlockInterface.Provider)
  }
}
package li.cil.oc.integration.appeng

import li.cil.oc.api.Driver
import li.cil.oc.integration.ModProxy
import li.cil.oc.integration.Mods

object ModAppEng extends ModProxy {
  override def getMod = Mods.AppliedEnergistics2

  override def initialize() {
    Driver.add(DriverController)

    Driver.add(new ConverterCellInventory)
  }
}
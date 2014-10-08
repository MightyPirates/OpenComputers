package li.cil.oc.integration.appeng

import li.cil.oc.api.Driver
import li.cil.oc.integration.IMod
import li.cil.oc.integration.Mods

object ModAppEng extends IMod {
  override def getMod = Mods.AppliedEnergistics2

  override def initialize() {
    Driver.add(new DriverGridNode)
    Driver.add(new DriverCellContainer)
    Driver.add(new ConverterCellInventory)
  }
}
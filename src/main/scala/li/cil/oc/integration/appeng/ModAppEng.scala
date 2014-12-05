package li.cil.oc.integration.appeng

import appeng.api.AEApi
import li.cil.oc.api.Driver
import li.cil.oc.integration.ModProxy
import li.cil.oc.integration.Mods

object ModAppEng extends ModProxy {
  override def getMod = Mods.AppliedEnergistics2

  override def initialize() {
    AEApi.instance().partHelper().registerNewLayer("li.cil.oc.integration.appeng.LayerSidedEnvironment", "li.cil.oc.api.network.SidedEnvironment")

    Driver.add(DriverController)
    Driver.add(DriverExportBus)

    Driver.add(new ConverterCellInventory)
  }
}

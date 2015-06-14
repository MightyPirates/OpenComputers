package li.cil.oc.integration.appeng

import appeng.api.AEApi
import li.cil.oc.api.Driver
import li.cil.oc.common.tileentity.Print
import li.cil.oc.integration.ModProxy
import li.cil.oc.integration.Mods

object ModAppEng extends ModProxy {
  override def getMod = Mods.AppliedEnergistics2

  override def initialize() {
    AEApi.instance.registries.movable.whiteListTileEntity(classOf[Print])

    Driver.add(DriverController)
    Driver.add(DriverExportBus)
    Driver.add(DriverImportBus)
    Driver.add(DriverPartInterface)
    Driver.add(DriverBlockInterface)

    Driver.add(new ConverterCellInventory)
  }
}

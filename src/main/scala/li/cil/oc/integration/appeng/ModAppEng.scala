package li.cil.oc.integration.appeng

import appeng.api.AEApi
import li.cil.oc.api
import li.cil.oc.api.Driver
import li.cil.oc.common.tileentity.Print
import li.cil.oc.integration.ModProxy
import li.cil.oc.integration.Mods

object ModAppEng extends ModProxy {
  override def getMod = Mods.AppliedEnergistics2

  override def initialize() {
    api.IMC.registerWrenchTool("li.cil.oc.integration.appeng.EventHandlerAE2.useWrench")
    api.IMC.registerWrenchToolCheck("li.cil.oc.integration.appeng.EventHandlerAE2.isWrench")

    AEApi.instance.registries.movable.whiteListTileEntity(classOf[Print])

    Driver.add(DriverController)
    Driver.add(DriverExportBus)
    Driver.add(DriverImportBus)
    Driver.add(DriverPartInterface)
    Driver.add(DriverBlockInterface)

    Driver.add(new ConverterCellInventory)
    Driver.add(new ConverterPattern)

    Driver.add(DriverController.Provider)
    Driver.add(DriverExportBus.Provider)
    Driver.add(DriverImportBus.Provider)
    Driver.add(DriverPartInterface.Provider)
    Driver.add(DriverBlockInterface.Provider)
  }
}

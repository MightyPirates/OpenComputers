package li.cil.oc.integration.mfr

import li.cil.oc.api
import li.cil.oc.api.Driver
import li.cil.oc.integration.ModProxy
import li.cil.oc.integration.Mods

object ModMineFactoryReloaded extends ModProxy {
  override def getMod = Mods.MineFactoryReloaded

  override def initialize() {
    api.IMC.registerWrenchTool("li.cil.oc.integration.mfr.EventHandlerMFR.useWrench")
    api.IMC.registerWrenchToolCheck("li.cil.oc.integration.mfr.EventHandlerMFR.isWrench")

    Driver.add(ConverterSafariNet)
  }
}

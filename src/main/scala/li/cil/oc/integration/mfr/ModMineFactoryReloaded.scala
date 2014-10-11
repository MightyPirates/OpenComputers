package li.cil.oc.integration.mfr

import li.cil.oc.api.Driver
import li.cil.oc.integration.ModProxy
import li.cil.oc.integration.Mods

object ModMineFactoryReloaded extends ModProxy {
  override def getMod = Mods.MineFactoryReloaded

  override def initialize() {
    Driver.add(ConverterSafariNet)
  }
}

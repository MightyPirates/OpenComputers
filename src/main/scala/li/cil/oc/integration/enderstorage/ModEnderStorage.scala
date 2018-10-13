package li.cil.oc.integration.enderstorage

import li.cil.oc.api.Driver
import li.cil.oc.integration.ModProxy
import li.cil.oc.integration.Mods

object ModEnderStorage extends ModProxy {
  override def getMod = Mods.EnderStorage

  override def initialize() {
    Driver.add(new DriverFrequencyOwner)
  }
}
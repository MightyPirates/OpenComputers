package li.cil.oc.integration.betterrecords

import li.cil.oc.api.Driver
import li.cil.oc.integration.ModProxy
import li.cil.oc.integration.Mods

object ModBetterRecords extends ModProxy {
  override def getMod = Mods.BetterRecords

  override def initialize() {
    Driver.add(ConverterRecord)
  }
}

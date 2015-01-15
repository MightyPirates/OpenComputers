package li.cil.oc.integration.botania

import li.cil.oc.api.Driver
import li.cil.oc.integration.{Mods, ModProxy}

object ModBotania extends ModProxy {
  override def getMod = Mods.Botania

  override def initialize() {
    Driver.add(new DriverManaBlock)
    Driver.add(new DriverManaReceiver)
    Driver.add(new DriverManaPool)
    Driver.add(new DriverManaSpreader)
    Driver.add(new DriverAltar)

    Driver.add(new ConverterManaItem)
  }
}

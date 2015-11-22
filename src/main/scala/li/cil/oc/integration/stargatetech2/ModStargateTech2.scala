package li.cil.oc.integration.stargatetech2

import li.cil.oc.api
import li.cil.oc.integration.ModProxy
import li.cil.oc.integration.Mods

object ModStargateTech2 extends ModProxy {
  override def getMod = Mods.StargateTech2

  override def initialize() {
    api.Driver.add(DriverAbstractBusCard)

    api.Driver.add(ConverterBusPacketNetScanDevice)

    api.Driver.add(DriverAbstractBusCard.Provider)
  }
}

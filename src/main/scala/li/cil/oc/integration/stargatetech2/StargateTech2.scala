package li.cil.oc.integration.stargatetech2

import li.cil.oc.api
import li.cil.oc.integration.IMod
import li.cil.oc.integration.Mods

object StargateTech2 extends IMod {
  override def getMod = Mods.StargateTech2

  override def initialize() {
    api.Driver.add(DriverAbstractBusCard)

    api.Driver.add(ConverterBusPacketNetScanDevice)
  }
}

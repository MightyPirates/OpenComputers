package li.cil.oc.integration.cofh.transport

import li.cil.oc.api.Driver
import li.cil.oc.integration.ModProxy
import li.cil.oc.integration.Mods

object ModCoFHTransport extends ModProxy {
  override def getMod = Mods.CoFHTransport

  override def initialize() {
    Driver.add(new DriverEnderEnergy)
    Driver.add(new DriverEnderFluid)
    Driver.add(new DriverEnderItem)
  }
}
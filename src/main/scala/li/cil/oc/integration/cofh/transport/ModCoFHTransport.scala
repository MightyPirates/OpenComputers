package li.cil.oc.integration.cofh.transport

import li.cil.oc.api.Driver
import li.cil.oc.integration.IMod
import li.cil.oc.integration.Mods

object ModCoFHTransport extends IMod {
  override def getMod = Mods.CoFHTransport

  override def initialize() {
    Driver.add(new DriverEnderEnergy)
    Driver.add(new DriverEnderFluid)
    Driver.add(new DriverEnderItem)
  }
}
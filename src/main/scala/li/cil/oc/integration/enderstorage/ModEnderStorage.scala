package li.cil.oc.integration.enderstorage

import li.cil.oc.api.Driver
import li.cil.oc.integration.IMod
import li.cil.oc.integration.Mods

object ModEnderStorage extends IMod {
  override def getMod = Mods.EnderStorage

  override def initialize() {
    Driver.add(new DriverFrequencyOwner)
  }
}
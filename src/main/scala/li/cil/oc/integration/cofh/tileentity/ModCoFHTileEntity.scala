package li.cil.oc.integration.cofh.tileentity

import li.cil.oc.api.Driver
import li.cil.oc.integration.IMod
import li.cil.oc.integration.Mods

object ModCoFHTileEntity extends IMod {
  override def getMod = Mods.CoFHTileEntity

  override def initialize() {
    Driver.add(new DriverEnergyInfo)
    Driver.add(new DriverRedstoneControl)
    Driver.add(new DriverSecureTile)
  }
}
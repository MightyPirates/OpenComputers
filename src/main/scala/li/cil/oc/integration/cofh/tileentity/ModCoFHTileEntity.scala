package li.cil.oc.integration.cofh.tileentity

import li.cil.oc.api.Driver
import li.cil.oc.integration.ModProxy
import li.cil.oc.integration.Mods

object ModCoFHTileEntity extends ModProxy {
  override def getMod = Mods.CoFHCore

  override def initialize() {
    Driver.add(new DriverEnergyInfo)
    Driver.add(new DriverRedstoneControl)
    Driver.add(new DriverSecureTile)
    Driver.add(new DriverSteamInfo)
  }
}
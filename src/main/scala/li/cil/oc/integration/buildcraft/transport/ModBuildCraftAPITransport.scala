package li.cil.oc.integration.buildcraft.transport

import li.cil.oc.api.Driver
import li.cil.oc.integration.ModProxy
import li.cil.oc.integration.Mods

object ModBuildCraftAPITransport extends ModProxy {
  override def getMod = Mods.BuildCraftTransport

  override def initialize() {
    Driver.add(new DriverPipeTile)
  }
}

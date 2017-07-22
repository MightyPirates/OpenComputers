package li.cil.oc.integration.gc

import li.cil.oc.api
import li.cil.oc.integration.ModProxy
import li.cil.oc.integration.Mods

object ModGalacticraft extends ModProxy {
  override def getMod = Mods.Galacticraft

  override def initialize() {
    api.Driver.add(DriverWorldSensorCard)

    api.Driver.add(DriverWorldSensorCard.Provider)
  }
}

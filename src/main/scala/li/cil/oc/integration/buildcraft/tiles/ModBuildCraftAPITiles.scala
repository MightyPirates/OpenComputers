package li.cil.oc.integration.buildcraft.tiles

import li.cil.oc.api.Driver
import li.cil.oc.integration.ModProxy
import li.cil.oc.integration.Mods

object ModBuildCraftAPITiles extends ModProxy {
  override def getMod = Mods.BuildCraftTiles

  override def initialize() {
    Driver.add(new DriverControllable)
  }
}

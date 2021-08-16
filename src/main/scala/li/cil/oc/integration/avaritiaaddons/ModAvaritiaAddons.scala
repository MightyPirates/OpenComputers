package li.cil.oc.integration.avaritiaaddons

import li.cil.oc.api.Driver
import li.cil.oc.integration.{ModProxy, Mods}

object ModAvaritiaAddons extends ModProxy {
  override def getMod = Mods.AvaritiaAddons
  override def initialize(): Unit = {
    Driver.add(DriverExtremeAutocrafter)
  }
}

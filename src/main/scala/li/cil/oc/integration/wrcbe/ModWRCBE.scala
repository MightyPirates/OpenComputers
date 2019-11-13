package li.cil.oc.integration.wrcbe

import li.cil.oc.integration.ModProxy
import li.cil.oc.integration.Mods
import li.cil.oc.integration.util.WirelessRedstone

object ModWRCBE extends ModProxy {
  override def getMod: Mods.SimpleMod = Mods.WirelessRedstoneCBE

  override def initialize() {
    WirelessRedstone.systems += WirelessRedstoneCBE
  }
}

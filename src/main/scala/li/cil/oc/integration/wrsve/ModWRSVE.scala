package li.cil.oc.integration.wrsve

import li.cil.oc.integration.ModProxy
import li.cil.oc.integration.Mods
import li.cil.oc.integration.util.WirelessRedstone

object ModWRSVE extends ModProxy {
  override def getMod = Mods.WirelessRedstoneSVE

  override def initialize() {
    WirelessRedstone.systems += WirelessRedstoneSVE
  }
}

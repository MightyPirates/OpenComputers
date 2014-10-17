package li.cil.oc.integration.mystcraft

import li.cil.oc.api.Driver
import li.cil.oc.integration.ModProxy
import li.cil.oc.integration.Mods

object ModMystcraft extends ModProxy {
  override def getMod = Mods.Mystcraft

  override def initialize() {
    Driver.add(new ConverterAgebook)
    Driver.add(new ConverterLinkbook)
    Driver.add(new ConverterPage)
  }
}
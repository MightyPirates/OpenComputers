package li.cil.oc.integration.mystcraft

import li.cil.oc.api.Driver
import li.cil.oc.integration.IMod
import li.cil.oc.integration.Mods

object ModMystcraft extends IMod {
  override def getMod = Mods.Mystcraft

  override def initialize() {
    Driver.add(new ConverterAgebook)
    Driver.add(new ConverterLinkbook)
    Driver.add(new ConverterPage)
  }
}
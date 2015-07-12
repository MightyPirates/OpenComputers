package li.cil.oc.integration.poweradvantage

import li.cil.oc.integration.ModProxy
import li.cil.oc.integration.Mods

object ModPowerAdvantage extends ModProxy {
  override def getMod = Mods.PowerAdvantage

  override def initialize(): Unit = {
    LightWeightPowerAcceptor.init()
  }
}

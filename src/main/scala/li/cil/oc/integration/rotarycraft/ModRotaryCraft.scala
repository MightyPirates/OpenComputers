package li.cil.oc.integration.rotarycraft

import li.cil.oc.api.Driver
import li.cil.oc.integration.ModProxy
import li.cil.oc.integration.Mods

object ModRotaryCraft extends ModProxy {
  override def getMod = Mods.RotaryCraft

  override def initialize() {
    Driver.add(new ConverterJetpackItem)
    Driver.add(new ConverterPumpItem)
  }
}
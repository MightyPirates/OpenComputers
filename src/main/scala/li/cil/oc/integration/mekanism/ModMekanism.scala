package li.cil.oc.integration.mekanism

import li.cil.oc.api
import li.cil.oc.integration.ModProxy
import li.cil.oc.integration.Mods

object ModMekanism extends ModProxy {
  override def getMod = Mods.Mekanism

  override def initialize(): Unit = {
    api.IMC.registerWrenchTool("li.cil.oc.integration.mekanism.EventHandlerMekanism.useWrench")
    api.IMC.registerWrenchToolCheck("li.cil.oc.integration.mekanism.EventHandlerMekanism.isWrench")
  }
}
